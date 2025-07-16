<template>
    <div>
      <p>index</p>
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

function sendCSVue(ws: WebSocket, id: string) {
  // 按 C++ 协议：int(信令) + int(id长度) + id内容
  const buffer = packMessage(RdtSignal.CS_VUE, id);
  ws.send(buffer);
}

// 连接到服务器
const ws = new WebSocket(`ws://185.128.227.222:${RDT_PORT}`);
ws.binaryType = 'arraybuffer';

ws.onopen = () => {
  sendCSVue(ws, 'admin_test');
};

ws.onmessage = (event) => {
  // 你可以用 DataView 解析收到的二进制数据
  console.log('收到消息', event.data);
};

ws.onclose = () => {
  console.log('WebSocket disconnected');
};

ws.onerror = (error) => {
  console.error('WebSocket error:', error);
};
</script>