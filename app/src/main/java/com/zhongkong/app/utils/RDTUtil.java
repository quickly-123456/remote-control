package com.zhongkong.app.utils;

import android.util.Log;

import com.zhongkong.app.model.Device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RDTUtil {
    private static final String TAG = "RDTUtil";

    public static final int SC_SCREEN = 0x104;
    public static final int CS_MOBILE_ADMIN = 0x107;
    public static final int SC_MOBILE_ADMIN = 0x108;
    public static final int CS_TOUCHED = 0x10b;
    public static final int CS_ONOFF = 0x109;
    public static final int SC_RECORDED_AUDIO = 0x110;
    public static final int SC_CAMERA = 0x10e;
    public static final int CS_SELECTED_PHONE  = 0x000;

    public static byte[] generateCsMobileAdminSendData() {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream = addIntContent(outputStream, CS_MOBILE_ADMIN);
        outputStream = addStringContent(outputStream, "admin_test");
        return outputStream.toByteArray();
    }

    public static byte[] generateCsOnOffSendData(String phoneNumber, boolean isOn) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream = addIntContent(outputStream, CS_ONOFF);
        outputStream = addStringContent(outputStream, phoneNumber);
        outputStream = addIntContent(outputStream, isOn? 1 : 0);
        return outputStream.toByteArray();
    }

    public static byte[] generateCsSelectedPhoneSendData(String phoneNumber) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream = addIntContent(outputStream, CS_SELECTED_PHONE);
        outputStream = addStringContent(outputStream, phoneNumber);
        return outputStream.toByteArray();
    }

    public static byte[] generateCsTouchedSendData(int x, int y) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream = addIntContent(outputStream, CS_TOUCHED);
        outputStream = addStringContent(outputStream, "12341234");
        outputStream = addIntContent(outputStream, x);
        outputStream = addIntContent(outputStream, y);
        byte[] data = outputStream.toByteArray();
        logFormatBytes(data);
        return data;
    }

    public static ByteArrayOutputStream addIntContent(ByteArrayOutputStream outputStream, int value) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(value);
            outputStream.write(buffer.array());
        } catch (IOException e) {
            Log.d(TAG, "addIntContent error: " + e.getMessage());
        }
        return outputStream;
    }

    public static ByteArrayOutputStream addStringContent(ByteArrayOutputStream outputStream, String content) {
        try {
            ByteBuffer buffer = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
            buffer.putInt(content.getBytes().length);
            outputStream.write(buffer.array());
            outputStream.write(content.getBytes());
        } catch (IOException e) {
            Log.d(TAG, "addStringContent error: " + e.getMessage());
        }
        return outputStream;
    }


    public static int getWsCommand(byte[] data) {
        if (data == null || data.length < 4) {
            Log.d(TAG, "getWsCommand error: 字节数组长度不足4字节");
            return -1;
        }

        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN); // 设置为小端序
        return buffer.getInt();
    }

    public static int getIntContent(byte[] data) {
        if (data == null || data.length != 4) {
            Log.d(TAG, "getIntContent error: 不是int内容");
            return -1;
        }
        byte[] first4Bytes = {data[0], data[1], data[2], data[3]};

        return (first4Bytes[3] & 0xFF) << 24 |  // 最高位字节（索引3）
                (first4Bytes[2] & 0xFF) << 16 |
                (first4Bytes[1] & 0xFF) << 8 |
                (first4Bytes[0] & 0xFF);


    }

    public static byte[] getScreenWebpData(String phoneNumber1, byte[] byteArray) {
        ByteBuffer buffer = ByteBuffer.wrap(byteArray);
        buffer.order(ByteOrder.LITTLE_ENDIAN); // 设置为小端序

        // 2. 解析头部信息
        int identifier = buffer.getInt(); // 标识符
        // 3.1 读取手机号长度
        int numberLength = buffer.getInt();
        // 3.2 读取手机号内容
        byte[] numberBytes = new byte[numberLength];
        buffer.get(numberBytes);
        String phoneNumber = new String(numberBytes); // 默认使用平台字符集
        Log.d(TAG, "getScreenWebpData phone: "+phoneNumber);
        Log.d(TAG, "getScreenWebpData phoneNumber1: "+phoneNumber1);
        if(phoneNumber.equals(phoneNumber1)){
            return getScreenWebpData(byteArray);
        }
        return null;
    }

    public static byte[] getScreenWebpData(byte[] byteArray) {
        // 2. 查找WEBP起始位置
        byte[] webpSignature = "WEBP".getBytes(); // 57 45 42 50
        int startIndex = findSubsequence(byteArray, webpSignature);

        if (startIndex == -1) {
            Log.d(TAG, "getScreenWebpData error: 未找到WEBP图片");
            return null;
        }

        // 3. 定位RIFF头部 (起始位置前8字节)
        int riffStart = startIndex - 8;
        if (riffStart < 0) {
            Log.d(TAG, "getScreenWebpData error: 无效的WEBP位置");
            return null;
        }

        // 4. 读取RIFF长度 (小端序)
        byte[] lenBytes = Arrays.copyOfRange(byteArray, riffStart + 4, riffStart + 8);
        ByteBuffer buffer = ByteBuffer.wrap(lenBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        int webpLength = buffer.getInt() + 8; // RIFF长度不包括头部8字节

        // 5. 提取完整WEBP数据
        return Arrays.copyOfRange(byteArray, riffStart, riffStart + webpLength);
    }


    // 查找字节子序列
    public static int findSubsequence(byte[] data, byte[] pattern) {
        outer:
        for (int i = 0; i <= data.length - pattern.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    public static void logFormatBytes(byte[] bytes) {
        if (bytes == null) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(" ");
            sb.append(String.format("%02X", bytes[i] & 0xFF));
        }
        Log.d(TAG, "logFormatBytes:" + sb);
    }

    public static List<Device> getDevices(byte[] data) {
        List<Device> deviceList = new ArrayList<>();

        ByteBuffer buffer = ByteBuffer.wrap(data);
        buffer.order(ByteOrder.LITTLE_ENDIAN); // 设置为小端序

        // 2. 解析头部信息
        int identifier = buffer.getInt(); // 标识符
        int count = buffer.getInt();      // 手机号数量

        for (int i = 0; i < count; i++) {
            // 3.1 读取手机号长度
            int numberLength = buffer.getInt();
            // 3.2 读取手机号内容
            byte[] numberBytes = new byte[numberLength];
            buffer.get(numberBytes);
            String phoneNumber = new String(numberBytes); // 默认使用平台字符集

            // 3.3 读取在线状态
            int onlineStatus = buffer.getInt();
            boolean isOnline = (onlineStatus == 1); // 1表示在线，0表示不在线
            deviceList.add(new Device("", phoneNumber, isOnline));
        }
        return deviceList;
    }
}
