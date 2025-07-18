package com.example.omnicontrol.utils;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * RDT消息协议 - Java版本
 * 用于Android与远程服务器之间的二进制数据通信
 * 对应C++版本的RDTMessage类
 */
public class RDTMessage {
    private ByteArrayOutputStream dataStream;
    private DataOutputStream writer;
    private ByteArrayInputStream inputStream;
    private DataInputStream reader;
    private int readOffset = 0;
    
    public RDTMessage() {
        dataStream = new ByteArrayOutputStream();
        writer = new DataOutputStream(dataStream);
    }
    
    public RDTMessage(byte[] data) {
        inputStream = new ByteArrayInputStream(data);
        reader = new DataInputStream(inputStream);
    }
    
    // 写入操作符重载 (<<)
    public RDTMessage writeString(String value) {
        try {
            byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
            writeInt(bytes.length);
            writer.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }
    
    public RDTMessage writeByteArray(byte[] value) {
        try {
            writeInt(value.length);
            writer.write(value);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }
    
    public RDTMessage writeInt(int value) {
        try {
            for (int i = 0; i < 4; i++) {
                writer.writeByte((value >> (i * 8)) & 0xFF);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }
    
    public RDTMessage writeFloat(float value) {
        try {
            writer.writeFloat(value);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }
    
    public RDTMessage writeRawData(byte[] data) {
        try {
            writeInt(data.length);
            writer.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }
    
    public RDTMessage writeRawBytes(byte[] data) {
        try {
            writer.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }
    
    // 读取操作符重载 (>>)
    public String readString() {
        try {
            int length = readInt();
            if (length > 0) {
                byte[] bytes = new byte[length];
                reader.readFully(bytes);
                return new String(bytes, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }
    
    public byte[] readByteArray() {
        try {
            int length = readInt();
            if (length > 0) {
                byte[] bytes = new byte[length];
                reader.readFully(bytes);
                return bytes;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }
    
    public int readInt() {
        int value = 0;
        try {
            for (int i = 0; i < 4; i++) {
                int byte_value = reader.readByte() & 0xFF;
                value += byte_value << (i * 8);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return value;
    }
    
    public float readFloat() {
        try {
            return reader.readFloat();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0.0f;
    }
    
    public byte[] readRawData() {
        try {
            int length = readInt();
            if (length > 0) {
                byte[] data = new byte[length];
                reader.readFully(data);
                return data;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new byte[0];
    }
    
    // 获取所有数据
    public byte[] getData() {
        try {
            writer.flush();
            return dataStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return new byte[0];
        }
    }
    
    // 清空数据
    public void clear() {
        try {
            dataStream.reset();
            readOffset = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 获取数据大小
    public int size() {
        return dataStream.size();
    }
    
    // 关闭流
    public void close() {
        try {
            if (writer != null) writer.close();
            if (dataStream != null) dataStream.close();
            if (reader != null) reader.close();
            if (inputStream != null) inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // 测试方法
    public static void test() {
        // 写入测试
        RDTMessage message = new RDTMessage();
        message.writeString("Hello, World!")
               .writeInt(123)
               .writeFloat(45.67f);
               
        byte[] data = message.getData();
        System.out.println("Generated data size: " + data.length);
        
        // 读取测试
        RDTMessage readMessage = new RDTMessage(data);
        String str = readMessage.readString();
        int intVal = readMessage.readInt();
        float floatVal = readMessage.readFloat();
        
        System.out.println("String: " + str);
        System.out.println("Int: " + intVal);
        System.out.println("Float: " + floatVal);
        
        message.close();
        readMessage.close();
    }
}
