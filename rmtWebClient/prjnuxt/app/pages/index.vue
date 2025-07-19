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

  wsData.value = `发送数据:\n信令: ${signal}\nID长度: ${idLength}\nID内容 (16进制): ${idHexStr}\nID内容 (字符串): ${idStr}\n时间: ${new Date().toLocaleString()}\n\n`;
}

// 解析接收到的二进制数据
function parseReceivedData(buffer: ArrayBuffer): string {
  try {
    const view = new DataView(buffer);
    let offset = 0;
    let result = '';

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
          const hexStr = Array.from(dataBytes).map(b => b.toString(16).padStart(2, '0')).join(' ');
          const dataStr = new TextDecoder().decode(dataBytes);
          result += `数据内容 (16进制): ${hexStr}\n`;
          result += `数据内容 (字符串): ${dataStr}\n`;
        }
      }
    }

    // 如果解析失败，显示原始字节
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
  wsData.value += `WebSocket 连接成功\n时间: ${new Date().toLocaleString()}\n\n`;
  sendCSVue(ws, 'admin_test');
};

ws.onmessage = (event) => {
  console.log('收到消息', event.data);
  const receivedData = parseReceivedData(event.data);
  wsData.value += `接收数据:\n${receivedData}\n时间: ${new Date().toLocaleString()}\n\n`;
};

ws.onclose = () => {
  console.log('WebSocket disconnected');
  wsData.value += `WebSocket 连接关闭\n时间: ${new Date().toLocaleString()}\n\n`;
};

ws.onerror = (error) => {
  console.error('WebSocket error:', error);
  wsData.value += `WebSocket 错误: ${error}\n时间: ${new Date().toLocaleString()}\n\n`;
};
</script>
