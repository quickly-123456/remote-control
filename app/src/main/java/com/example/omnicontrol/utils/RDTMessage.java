package com.example.omnicontrol.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * RDT消息封装类
 * 支持二进制数据的读写操作，用于RDT协议消息的构建和解析
 */
public class RDTMessage {
    private ByteArrayOutputStream outputStream;
    private ByteBuffer readBuffer;
    private int readPosition = 0;
    
    /**
     * 构造函数 - 用于写入数据
     */
    public RDTMessage() {
        this.outputStream = new ByteArrayOutputStream();
    }
    
    /**
     * 构造函数 - 用于读取数据
     * @param data 要解析的字节数组
     */
    public RDTMessage(byte[] data) {
        this.readBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        this.readPosition = 0;
    }
    
    /**
     * 写入整数（4字节，小端序）
     * @param value 整数值
     * @return this，支持链式调用
     */
    public RDTMessage writeInt(int value) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(value);
            outputStream.write(buffer.array());
        } catch (IOException e) {
            throw new RuntimeException("写入整数失败", e);
        }
        return this;
    }
    
    /**
     * 写入字符串（UTF-8编码，带长度前缀）
     * @param str 字符串
     * @return this，支持链式调用
     */
    public RDTMessage writeString(String str) {
        if (str == null) {
            writeInt(0);
            return this;
        }
        
        byte[] strBytes = str.getBytes(StandardCharsets.UTF_8);
        writeInt(strBytes.length);
        writeRawBytes(strBytes);
        return this;
    }
    
    /**
     * 写入字节数组（带长度前缀）
     * @param data 字节数组
     * @return this，支持链式调用
     */
    public RDTMessage writeByteArray(byte[] data) {
        if (data == null) {
            writeInt(0);
            return this;
        }
        
        writeInt(data.length);
        writeRawBytes(data);
        return this;
    }
    
    /**
     * 写入原始字节（不带长度前缀）
     * @param data 字节数组
     * @return this，支持链式调用
     */
    public RDTMessage writeRawBytes(byte[] data) {
        if (data != null && data.length > 0) {
            try {
                outputStream.write(data);
            } catch (IOException e) {
                throw new RuntimeException("写入字节数组失败", e);
            }
        }
        return this;
    }
    
    /**
     * 写入原始数据（writeRawBytes的别名，保持兼容性）
     * @param data 字节数组
     * @return this，支持链式调用
     */
    public RDTMessage writeRawData(byte[] data) {
        return writeRawBytes(data);
    }
    
    /**
     * 读取整数（4字节，小端序）
     * @return 整数值
     */
    public int readInt() {
        if (readBuffer == null || readBuffer.remaining() < 4) {
            throw new RuntimeException("读取整数失败：数据不足");
        }
        return readBuffer.getInt();
    }
    
    /**
     * 读取字符串（UTF-8编码，带长度前缀）
     * @return 字符串
     */
    public String readString() {
        int length = readInt();
        if (length == 0) {
            return "";
        }
        
        if (readBuffer.remaining() < length) {
            throw new RuntimeException("读取字符串失败：数据不足");
        }
        
        byte[] strBytes = new byte[length];
        readBuffer.get(strBytes);
        return new String(strBytes, StandardCharsets.UTF_8);
    }
    
    /**
     * 读取字节数组（带长度前缀）
     * @return 字节数组
     */
    public byte[] readByteArray() {
        int length = readInt();
        if (length == 0) {
            return new byte[0];
        }
        
        if (readBuffer.remaining() < length) {
            throw new RuntimeException("读取字节数组失败：数据不足");
        }
        
        byte[] data = new byte[length];
        readBuffer.get(data);
        return data;
    }
    
    /**
     * 读取剩余的所有字节
     * @return 字节数组
     */
    public byte[] readRemainingBytes() {
        if (readBuffer == null) {
            return new byte[0];
        }
        
        int remaining = readBuffer.remaining();
        if (remaining == 0) {
            return new byte[0];
        }
        
        byte[] data = new byte[remaining];
        readBuffer.get(data);
        return data;
    }
    
    /**
     * 获取构建的数据
     * @return 字节数组
     */
    public byte[] getData() {
        if (outputStream != null) {
            return outputStream.toByteArray();
        }
        return new byte[0];
    }
    
    /**
     * 获取剩余可读字节数
     * @return 剩余字节数
     */
    public int remaining() {
        if (readBuffer != null) {
            return readBuffer.remaining();
        }
        return 0;
    }
    
    /**
     * 关闭并释放资源
     */
    public void close() {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                // 忽略关闭异常
            }
            outputStream = null;
        }
        readBuffer = null;
    }
}
