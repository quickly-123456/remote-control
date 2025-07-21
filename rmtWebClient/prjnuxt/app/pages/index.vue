<template>
  <div class="flex flex-col items-center justify-center gap-4 h-screen">
    <h1 class="font-bold text-2xl text-(--ui-primary)">
      Starter
    </h1>

    <div class="flex items-center gap-2">
      <UButton label="sendCSVue" icon="i-lucide-square-play" @click="sendCSVue(ws, 'admin_test')" />

      <UButton label="Login" color="neutral" variant="outline" icon="i-lucide-lightbulb" to="/login" />
    </div>
    <div>
      <div v-if="webpImage" class="mt-4">
        <h3 class="font-bold mb-2">接收到的WebP图片:</h3>
        <img :src="webpImage" alt="WebP Image" class="max-w-full h-auto border rounded" />
      </div>
      <div v-if="wsData">
        <pre>{{ wsData }}</pre>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { RdtSignal, RDT_PORT } from './rdtDefine'

// 协议打包函数
function packMessage(...fields: (string | number | Uint8Array)[]): ArrayBuffer {
  // 先计算总长度
  let totalLength = 0;
  const encodedFields: (number | Uint8Array)[] = [];

  for (const field of fields) {
    if (typeof field === 'number') {
      totalLength += 4;
      encodedFields.push(field);
    } else if (typeof field === 'string') {
      const bytes = new TextEncoder().encode(field);
      totalLength += 4 + bytes.length;
      encodedFields.push(bytes.length);
      encodedFields.push(bytes);
    } else if (field instanceof Uint8Array) {
      totalLength += 4 + field.length;
      encodedFields.push(field.length);
      encodedFields.push(field);
    }
  }

  const buffer = new ArrayBuffer(totalLength);
  const view = new DataView(buffer);
  let offset = 0;

  for (const field of encodedFields) {
    if (typeof field === 'number') {
      view.setInt32(offset, field, true); // 小端
      offset += 4;
    } else if (field instanceof Uint8Array) {
      new Uint8Array(buffer, offset, field.length).set(field);
      offset += field.length;
    }
  }

  return buffer;
}

const wsData = ref<string>('');
const webpImage = ref<string>('');
const latestSentData = ref<string>('');
const latestReceivedData = ref<string>('');

function sendCSVue(ws: WebSocket, id: string) {
  // 按 C++ 协议：int(信令) + int(id长度) + id内容
  const buffer = packMessage(RdtSignal.CS_VUE, id);
  ws.send(buffer);

  // 显示发送的数据
  const view = new DataView(buffer);
  const signal = view.getInt32(0, true);
  const idLength = view.getInt32(4, true);
  const idBytes = new Uint8Array(buffer, 8, idLength);
  const idHexStr = Array.from(idBytes).map(b => b.toString(16).padStart(2, '0')).join(' ');
  const idStr = new TextDecoder().decode(idBytes);

  latestSentData.value = `发送数据:\n信令: ${signal}\nID长度: ${idLength}\nID内容 (16进制): ${idHexStr}\nID内容 (字符串): ${idStr}\n时间: ${new Date().toLocaleString()}`;

  // 更新显示为最新的发送和接收数据
  wsData.value = latestSentData.value + (latestReceivedData.value ? '\n\n' + latestReceivedData.value : '');
}

// 检测是否为WebP格式
function isWebP(buffer: ArrayBuffer): boolean {
  const bytes = new Uint8Array(buffer);
  // WebP文件头: RIFF....WEBP
  if (bytes.length < 12) return false;

  // 检查RIFF标识
  if (bytes[0] !== 0x52 || bytes[1] !== 0x49 || bytes[2] !== 0x46 || bytes[3] !== 0x46) {
    return false;
  }

  // 检查WEBP标识
  if (bytes[8] !== 0x57 || bytes[9] !== 0x45 || bytes[10] !== 0x42 || bytes[11] !== 0x50) {
    return false;
  }

  return true;
}

// 在数据中搜索并提取WebP图片
function findAndExtractWebP(buffer: ArrayBuffer): ArrayBuffer | null {
  const bytes = new Uint8Array(buffer);

  // 搜索RIFF标识
  for (let i = 0; i <= bytes.length - 12; i++) {
    if (bytes[i] === 0x52 && bytes[i + 1] === 0x49 && bytes[i + 2] === 0x46 && bytes[i + 3] === 0x46) {
      // 找到RIFF，检查是否为WEBP
      if (i + 11 < bytes.length &&
        bytes[i + 8] === 0x57 && bytes[i + 9] === 0x45 &&
        bytes[i + 10] === 0x42 && bytes[i + 11] === 0x50) {

        // 读取文件大小 (RIFF chunk size)
        const view = new DataView(buffer, i + 4, 4);
        const fileSize = view.getUint32(0, true) + 8; // +8 for RIFF header

        // 确保不超出缓冲区边界
        const actualSize = Math.min(fileSize, bytes.length - i);

        // 提取WebP数据
        return buffer.slice(i, i + actualSize);
      }
    }
  }

  return null;
}

// 处理WebP图片数据
function handleWebPData(buffer: ArrayBuffer) {
  const blob = new Blob([buffer], { type: 'image/webp' });
  const url = URL.createObjectURL(blob);
  webpImage.value = url;
}

// 解析接收到的二进制数据
function parseReceivedData(buffer: ArrayBuffer): string {
  try {
    const view = new DataView(buffer);
    let offset = 0;
    let result = '';

    // 首先在整个缓冲区中搜索WebP图片
    const webpData = findAndExtractWebP(buffer);
    if (webpData) {
      handleWebPData(webpData);
      result += `在数据中找到WebP图片 (${webpData.byteLength} 字节)\n`;
      result += `图片已显示在下方\n`;

      // 如果整个缓冲区就是WebP，直接返回
      if (isWebP(buffer)) {
        return result;
      }

      // 否则继续解析协议部分
      result += `\n协议信息:\n`;
    }

    // 尝试解析为协议格式
    if (buffer.byteLength >= 4) {
      const signal = view.getInt32(offset, true);
      offset += 4;
      result += `信令: ${signal}\n`;

      if (buffer.byteLength >= 8) {
        const dataLength = view.getInt32(offset, true);
        offset += 4;
        result += `数据长度: ${dataLength}\n`;

        if (offset + dataLength <= buffer.byteLength) {
          const dataBytes = new Uint8Array(buffer, offset, dataLength);

          // 如果没有找到WebP，显示原始数据
          if (!webpData) {
            const hexStr = Array.from(dataBytes).map(b => b.toString(16).padStart(2, '0')).join(' ');
            const dataStr = new TextDecoder().decode(dataBytes);
            result += `数据内容 (16进制): ${hexStr}\n`;
            result += `数据内容 (字符串): ${dataStr}\n`;
          } else {
            result += `数据内容: 包含WebP图片数据\n`;
          }
        }
      }
    }

    // 如果解析失败且没有WebP，显示原始字节
    if (!result) {
      const bytes = new Uint8Array(buffer);
      result = `原始数据 (${bytes.length} 字节):\n`;
      result += Array.from(bytes).map(b => b.toString(16).padStart(2, '0')).join(' ');
    }

    return result;
  } catch (error) {
    return `解析错误: ${error}`;
  }
}

// 连接到服务器
const ws = new WebSocket(`ws://185.128.227.222:${RDT_PORT}`);
ws.binaryType = 'arraybuffer';

ws.onopen = () => {
  console.log('WebSocket connected');
  sendCSVue(ws, 'admin_test');
};

ws.onmessage = (event) => {
  console.log('收到消息', event.data);
  const receivedData = parseReceivedData(event.data);
  latestReceivedData.value = `接收数据:\n${receivedData}\n时间: ${new Date().toLocaleString()}`;

  // 更新显示为最新的发送和接收数据
  wsData.value = (latestSentData.value ? latestSentData.value + '\n\n' : '') + latestReceivedData.value;
};

ws.onclose = () => {
  console.log('WebSocket disconnected');
  latestReceivedData.value = `WebSocket 连接关闭\n时间: ${new Date().toLocaleString()}`;
  wsData.value = (latestSentData.value ? latestSentData.value + '\n\n' : '') + latestReceivedData.value;
};

ws.onerror = (error) => {
  console.error('WebSocket error:', error);
  latestReceivedData.value = `WebSocket 错误: ${error}\n时间: ${new Date().toLocaleString()}`;
  wsData.value = (latestSentData.value ? latestSentData.value + '\n\n' : '') + latestReceivedData.value;
};
</script>
