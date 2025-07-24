package com.example.omnicontrol.utils;

import android.util.Log;

/**
 * RDT协议封装类
 * 处理完整的RDT消息格式：[信号类型4字节][数据长度4字节][数据内容]
 */
public class RDTProtocol {
    private static final String TAG = "RDTProtocol";
    
    /**
     * 创建完整的RDT消息
     * @param signalType 信号类型（如RdtSignal.CS_USER）
     * @param message RDTMessage消息内容
     * @return 完整的RDT消息字节数组
     */
    public static byte[] createRDTMessage(int signalType, RDTMessage message) {
        try {
            byte[] messageData = message.getData();
            
            // 创建完整消息：信号类型(4字节) + 数据长度(4字节) + 数据内容
            RDTMessage fullMessage = new RDTMessage();
            fullMessage.writeInt(signalType);        // 信号类型
            byte[] result = fullMessage.getData();

            fullMessage.close();

            byte[] combinedResult = new byte[result.length + messageData.length];

            System.arraycopy(result, 0, combinedResult, 0, result.length);
            System.arraycopy(messageData, 0, combinedResult, result.length, messageData.length);

            Log.d(TAG, String.format("创建RDT消息: 信号类型=0x%X, 数据长度=%d, 总长度=%d",
                signalType, messageData.length, result.length));
            
            return combinedResult;
            
        } catch (Exception e) {
            Log.e(TAG, "创建RDT消息失败", e);
            return new byte[0];
        }
    }
    
    /**
     * 解析RDT消息
     * @param data 完整的RDT消息字节数组
     * @return RDTMessageInfo 包含信号类型和消息内容
     */
    public static RDTMessageInfo parseRDTMessage(byte[] data) {
        try {
            if (data.length < 8) { // 至少需要8字节（信号类型4字节 + 数据长度4字节）
                Log.e(TAG, "RDT消息长度不足: " + data.length);
                return null;
            }
            
            RDTMessage parser = new RDTMessage(data);
            
            // 解析信号类型和数据长度
            int signalType = parser.readInt();
            int dataLength = parser.readInt();
            
            Log.d(TAG, String.format("解析RDT消息: 信号类型=0x%X, 数据长度=%d", 
                signalType, dataLength));
            
            // 验证数据长度
            if (data.length != 8 + dataLength) {
                Log.e(TAG, String.format("RDT消息长度不匹配: 期望=%d, 实际=%d", 
                    8 + dataLength, data.length));
                parser.close();
                return null;
            }
            
            // 读取剩余数据
            byte[] messageData = new byte[dataLength];
            System.arraycopy(data, 8, messageData, 0, dataLength);
            
            parser.close();
            
            return new RDTMessageInfo(signalType, messageData);
            
        } catch (Exception e) {
            Log.e(TAG, "解析RDT消息失败", e);
            return null;
        }
    }
    
    /**
     * 创建用户信息消息（CS_USER）
     * @param phoneNumber 电话号码
     * @param userId 用户ID
     * @return 完整的RDT消息
     */
    public static byte[] createUserMessage(String phoneNumber, String userId) {
        RDTMessage message = new RDTMessage();
        message.writeString(phoneNumber);
        message.writeString(userId);
        
        byte[] result = createRDTMessage(RDTDefine.RdtSignal.CS_USER, message);
        message.close();
        return result;
    }
    
    /**
     * 创建音频数据消息（CS_AUDIO）
     * @param audioData 音频数据
     * @return 完整的RDT消息
     */
    public static byte[] createAudioMessage(byte[] audioData) {
        RDTMessage message = new RDTMessage();
        message.writeByteArray(audioData);
        
        byte[] result = createRDTMessage(RDTDefine.RdtSignal.CS_AUDIO, message);
        message.close();
        return result;
    }
    
    /**
     * 创建摄像头数据消息（CS_CAMERA）
     * @param imageData 图像数据
     * @return 完整的RDT消息
     */
    public static byte[] createCameraMessage(byte[] imageData) {
        RDTMessage message = new RDTMessage();
        message.writeByteArray(imageData);
        
        byte[] result = createRDTMessage(RDTDefine.RdtSignal.CS_CAMERA, message);
        message.close();
        return result;
    }
    
    /**
     * 创建控制响应消息（CS_CONTROL）
     * @param responseCode 响应代码
     * @param responseMessage 响应消息
     * @return 完整的RDT消息
     */
    public static byte[] createControlResponseMessage(int responseCode, String responseMessage) {
        RDTMessage message = new RDTMessage();
        message.writeInt(responseCode);
        message.writeString(responseMessage);
        
        byte[] result = createRDTMessage(RDTDefine.RdtSignal.CS_CONTROL, message);
        message.close();
        return result;
    }
    
    /**
     * 创建文件操作响应消息（CS_FILE）
     * @param success 操作是否成功
     * @param message 响应消息
     * @return 完整的RDT消息
     */
    public static byte[] createFileResponseMessage(boolean success, String message) {
        RDTMessage rdtMessage = new RDTMessage();
        rdtMessage.writeInt(success ? 1 : 0);
        rdtMessage.writeString(message);
        
        byte[] result = createRDTMessage(RDTDefine.RdtSignal.CS_FILE, rdtMessage);
        rdtMessage.close();
        return result;
    }
    
    /**
     * 解析控制命令消息（SC_CONTROL）
     * @param messageData 消息数据
     * @return 控制命令字符串
     */
    public static String parseControlCommand(byte[] messageData) {
        try {
            RDTMessage message = new RDTMessage(messageData);
            String command = message.readString();
            message.close();
            return command;
        } catch (Exception e) {
            Log.e(TAG, "解析控制命令失败", e);
            return "";
        }
    }
    
    /**
     * 解析文件操作命令消息（SC_FILE）
     * @param messageData 消息数据
     * @return FileOperationInfo 文件操作信息
     */
    public static FileOperationInfo parseFileOperation(byte[] messageData) {
        try {
            RDTMessage message = new RDTMessage(messageData);
            String fileName = message.readString();
            String fileType = message.readString();
            byte[] fileData = message.readByteArray();
            message.close();
            
            return new FileOperationInfo(fileName, fileType, fileData);
        } catch (Exception e) {
            Log.e(TAG, "解析文件操作失败", e);
            return null;
        }
    }
    
    /**
     * RDT消息信息类
     */
    public static class RDTMessageInfo {
        public final int signalType;
        public final byte[] messageData;
        
        public RDTMessageInfo(int signalType, byte[] messageData) {
            this.signalType = signalType;
            this.messageData = messageData;
        }
        
        public String getSignalTypeName() {
            switch (signalType) {
                case RDTDefine.RdtSignal.CS_USER: return "CS_USER";
                case RDTDefine.RdtSignal.SC_USER: return "SC_USER";
                case RDTDefine.RdtSignal.CS_VUE: return "CS_VUE";
                case RDTDefine.RdtSignal.SC_VUE: return "SC_VUE";
                case RDTDefine.RdtSignal.CS_SCREEN: return "CS_SCREEN";
                case RDTDefine.RdtSignal.SC_SCREEN: return "SC_SCREEN";
                case RDTDefine.RdtSignal.CS_AUDIO: return "CS_AUDIO";
                case RDTDefine.RdtSignal.SC_AUDIO: return "SC_AUDIO";
                case RDTDefine.RdtSignal.CS_CAMERA: return "CS_CAMERA";
                case RDTDefine.RdtSignal.SC_CAMERA: return "SC_CAMERA";
                case RDTDefine.RdtSignal.CS_CONTROL: return "CS_CONTROL";
                case RDTDefine.RdtSignal.SC_CONTROL: return "SC_CONTROL";
                case RDTDefine.RdtSignal.CS_FILE: return "CS_FILE";
                case RDTDefine.RdtSignal.SC_FILE: return "SC_FILE";
                default: return "UNKNOWN(0x" + Integer.toHexString(signalType) + ")";
            }
        }
    }
    
    /**
     * 文件操作信息类
     */
    public static class FileOperationInfo {
        public final String fileName;
        public final String fileType;
        public final byte[] fileData;
        
        public FileOperationInfo(String fileName, String fileType, byte[] fileData) {
            this.fileName = fileName;
            this.fileType = fileType;
            this.fileData = fileData;
        }
    }
}
