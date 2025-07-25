package com.example.omnicontrol.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * WebSocket客户端管理器
 * 用于实时推送屏幕数据到远程服务器
 */
public class WebSocketManager {
    private static WebSocketManager _webSocketManager = null;
    private static final String TAG = "WebSocketManager";
    
    private WebSocketClient webSocketClient;
    private Handler mainHandler;
    private AtomicInteger connectionState = new AtomicInteger(RDTDefine.ConnectionState.DISCONNECTED);
    private int retryCount = 0;
    private Context context; // 用于获取用户信息
    
    // 统计数据
    private AtomicLong sentFrames = new AtomicLong(0);
    private AtomicLong sentBytes = new AtomicLong(0);
    private long lastStatsTime = System.currentTimeMillis();
    
    // 用户信息
    private String phoneNumber;
    private String userId;
    
    // 连接状态监听器
    public interface ConnectionStateListener {
        void onConnectionStateChanged(int state);
        void onScreenDataSent(long frameNumber, int dataSize);
        void onError(String error);
    }
    
    // 消息转发回调接口
    public interface MessageForwardCallback {
        void onMessageReceived(byte[] data);
    }
    
    private ConnectionStateListener stateListener;
    private static MessageForwardCallback messageForwardCallback;
    
    public WebSocketManager() {
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static WebSocketManager instance()
    {
        if (_webSocketManager == null)
            _webSocketManager = new WebSocketManager();
        return _webSocketManager;
    }
    /**
     * 带Context的构造函数，用于获取用户信息
     */
    private WebSocketManager(Context context) {
        this.context = context;
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public void setConnectionStateListener(ConnectionStateListener listener) {
        this.stateListener = listener;
    }
    
    /**
     * 设置消息转发回调
     */
    public static void setMessageForwardCallback(MessageForwardCallback callback) {
        messageForwardCallback = callback;
        Log.i(TAG, "🔗 消息转发回调已设置");
    }
    
    /**
     * 转发消息到RemoteControlService
     */
    private void forwardToRemoteControlService(byte[] data) {
        if (messageForwardCallback != null) {
            Log.d(TAG, "🚀 转发消息到RemoteControlService ("+data.length+" bytes)");
            messageForwardCallback.onMessageReceived(data);
        } else {
            Log.w(TAG, "⚠️ 消息转发回调未设置，消息丢失");
        }
    }
    
    /**
     * 连接到WebSocket服务器
     * @param phoneNumber 用户手机号
     * @param userId 用户ID
     */
    public void connect(String phoneNumber, String userId) {
        this.phoneNumber = phoneNumber;
        this.userId = userId;
        connect();
    }
    
    /**
     * 连接到WebSocket服务器
     */
    public void connect() {
        if (connectionState.get() == RDTDefine.ConnectionState.CONNECTED || 
            connectionState.get() == RDTDefine.ConnectionState.CONNECTING) {
            Log.w(TAG, "WebSocket已连接或正在连接中");
            return;
        }
        
        try {
            URI serverUri = URI.create(RDTDefine.WS_SERVER_URL);
            updateConnectionState(RDTDefine.ConnectionState.CONNECTING);
            
            webSocketClient = new WebSocketClient(serverUri) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    Log.i(TAG, "🌐 WebSocket连接成功 - " + RDTDefine.WS_SERVER_URL);
                    Log.i(TAG, "🔗 消息转发回调状态: " + (messageForwardCallback != null ? "已设置" : "未设置"));
                    updateConnectionState(RDTDefine.ConnectionState.CONNECTED);
                    retryCount = 0; // 重置重连计数器
                    
                    // 发送用户认证信号
                    sendUserAuthSignal(phoneNumber, userId);
                }
                
                @Override
                public void onMessage(String message) {
                    Log.i("websocketGet", "📨 接收到文本消息: " + message.length() + " chars");
                    Log.d("websocketGet", "📝 消息内容: " + message);
                    
                    Log.i(TAG, "📨 收到服务器文本消息: " + message);
                    Log.d(TAG, "📝 消息长度: " + message.length());
                    Log.d(TAG, "📝 消息内容: " + message);
                    // 转发文本消息到RemoteControlService
                    if (messageForwardCallback != null) {
                        messageForwardCallback.onMessageReceived(message.getBytes());
                    }
                }
                
                @Override
                public void onMessage(java.nio.ByteBuffer bytes) {
                    try {
                        int remaining = bytes.remaining();
                        
                        // 优先级最高的接收数据日志
                        Log.i("websocketGet", "✨ 接收到二进制数据: " + remaining + " bytes");
                        Log.d("websocketGet", "📊 ByteBuffer: pos=" + bytes.position() + ", lim=" + bytes.limit() + ", cap=" + bytes.capacity());
                        
                        Log.i(TAG, "📨 收到二进制消息: " + remaining + " bytes");
                        
                        // 检查ByteBuffer状态
                        Log.d(TAG, "📊 ByteBuffer状态: position=" + bytes.position() + ", limit=" + bytes.limit() + ", capacity=" + bytes.capacity());
                        
                        // 安全转换为Byte数组
                        byte[] data = new byte[remaining];
                        bytes.get(data);
                        
                        // 显示前16字节的十六进制内容
                        StringBuilder hexPreview = new StringBuilder();
                        for (int i = 0; i < Math.min(16, data.length); i++) {
                            hexPreview.append(String.format("%02X ", data[i] & 0xFF));
                        }
                        Log.d("websocketGet", "🔍 数据预览: " + hexPreview.toString());
                        
                        Log.i(TAG, "🚀 调用handleReceivedMessage处理 " + data.length + " bytes");
                        handleReceivedMessage(data);
                        
                    } catch (Exception e) {
                        Log.e(TAG, "处理二进制消息失败", e);
                        Log.e("websocketGet", "❌ 处理数据失败: " + e.getMessage());
                    }
                }
                
                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.w(TAG, String.format("🔌 WebSocket连接关闭 - Code: %d, Reason: %s, Remote: %b", 
                           code, reason, remote));
                    updateConnectionState(RDTDefine.ConnectionState.DISCONNECTED);
                    
                    // 自动重连逻辑
                    if (retryCount < RDTDefine.ScreenConfig.MAX_RETRY_COUNT) {
                        scheduleReconnect();
                    }
                }
                
                @Override
                public void onError(Exception ex) {
                    Log.e(TAG, "❌ WebSocket连接错误: " + ex.getMessage(), ex);
                    updateConnectionState(RDTDefine.ConnectionState.ERROR);
                    
                    if (stateListener != null) {
                        mainHandler.post(() -> stateListener.onError("WebSocket错误: " + ex.getMessage()));
                    }
                }
            };
            
            webSocketClient.connect();
            
        } catch (Exception e) {
            Log.e(TAG, "WebSocket连接初始化失败: " + e.getMessage(), e);
            updateConnectionState(RDTDefine.ConnectionState.ERROR);
        }
    }
    
    /**
     * 处理接收到的消息
     */
    private void handleReceivedMessage(byte[] data) {
        try {
            Log.d(TAG, String.format("📨 接收到服务器消息 (%d bytes)", data.length));
            
            // 首先检查是否为原始二进制信号（如SC_TOUCHED）
            if (handleRawBinarySignal(data)) {
                return; // 已处理原始信号，直接返回
            }
            
            // 尝试解析RDT消息
            RDTProtocol.RDTMessageInfo messageInfo = RDTProtocol.parseRDTMessage(data);
            if (messageInfo != null) {
                Log.d(TAG, "📨 解析RDT消息: " + messageInfo.getSignalTypeName());
                
                switch (messageInfo.signalType) {
                    case RDTDefine.RdtSignal.SC_ONOFF: // 更新为正确的信号
                        // 处理设备开关控制
                        Log.i(TAG, "🎮 收到设备开关控制信号");
                        break;
                        
                    default:
                        Log.d(TAG, "📨 未处理的RDT消息类型: " + messageInfo.getSignalTypeName());
                        break;
                }
            } else {
                Log.w(TAG, "⚠️ 无法解析RDT消息，也不是原始信号");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "处理接收消息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理原始二进制信号（如CS_TOUCHED、SC_TOUCHED等）
     * @param data 二进制数据
     * @return true 如果是原始信号并已处理，false 否则
     */
    private boolean handleRawBinarySignal(byte[] data) {
        try {
            // 检查触摸信号（至少需要 4 字节作为信号类型）
            if (data.length >= 4) {
                int signal = java.nio.ByteBuffer.wrap(data, 0, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
                
                // CS_TOUCHED (267) - 服务器发送给Android客户端的触摸信号
                if (signal == RDTDefine.RdtSignal.CS_TOUCHED) {
                    Log.i(TAG, "👆 检测到CS_TOUCHED原始信号（服务器→Android），转发给RemoteControlService");
                    forwardToRemoteControlService(data);
                    return true;
                }
                
                // SC_TOUCHED (268) - Android客户端发送的触摸信号（备用）
                if (signal == RDTDefine.RdtSignal.SC_TOUCHED && data.length == 12) {
                    Log.i(TAG, "👆 检测到SC_TOUCHED原始信号，转发给RemoteControlService");
                    forwardToRemoteControlService(data);
                    return true;
                }
            }
            
            // 检查其他原始信号...
            // 可以在这里添加更多原始信号的检查
            
        } catch (Exception e) {
            Log.e(TAG, "处理原始信号失败", e);
        }
        
        return false; // 不是原始信号
    }
    
    /**
     * 发送用户认证信息
     */
    private void sendUserAuth() {
        try {
            if (phoneNumber == null || userId == null) {
                Log.e(TAG, "❌ 用户信息为空，无法发送认证");
                return;
            }
            
            // 使用RDTProtocol创建用户认证消息
            byte[] authMessage = RDTProtocol.createUserMessage(phoneNumber, userId);
            webSocketClient.send(authMessage);
            
            Log.i(TAG, String.format("🔐 发送用户认证: 手机号=%s, 用户ID=%s", phoneNumber, userId));
            
        } catch (Exception e) {
            Log.e(TAG, "发送用户认证失败: " + e.getMessage(), e);
            if (stateListener != null) {
                mainHandler.post(() -> stateListener.onError("认证失败: " + e.getMessage()));
            }
        }
    }
    
    /**
     * 公开方法：立即发送用户认证信号
     * @param phone 用户手机号
     * @param userId 用户ID
     */
    public void sendUserAuthSignal(String phone, String userId) {
        try {
            this.phoneNumber = phone;
            this.userId = userId;
            
            Log.i(TAG, String.format("🔐 立即发送用户认证信号: phone=%s, userId=%s", phone, userId));
            
            // 如果WebSocket已连接，立即发送认证
            if (webSocketClient != null && isConnected()) {
                sendUserAuth();
            } else {
                // 如果未连接，尝试建立连接后发送
                Log.i(TAG, "🌐 WebSocket未连接，建立连接后发送认证信号");
                connect();
            }
            
        } catch (Exception e) {
            Log.e(TAG, "发送用户认证信号失败: " + e.getMessage(), e);
            if (stateListener != null) {
                mainHandler.post(() -> stateListener.onError("认证信号发送失败: " + e.getMessage()));
            }
        }
    }
    
    /**
     * 发送音频数据
     */
    public void sendAudioData(byte[] audioData) {
        Log.i(TAG, String.format("🎤 WebSocketManager.sendAudioData 被调用 - 数据大小: %d bytes, 连接状态: %s", 
            audioData.length, RDTDefine.getConnectionStateDescription(connectionState.get())));
            
        if (connectionState.get() != RDTDefine.ConnectionState.CONNECTED) {
            Log.w(TAG, "⚠️ 音频数据发送被跳过 - WebSocket未连接");
            return;
        }
        
        try {
            byte[] rdtMessage = RDTProtocol.createAudioMessage(audioData);
            webSocketClient.send(rdtMessage);
            
            sentBytes.addAndGet(rdtMessage.length);
            Log.i(TAG, String.format("✅ 音频数据发送成功: %d bytes -> %d bytes RDT", audioData.length, rdtMessage.length));
            
        } catch (Exception e) {
            Log.e(TAG, "发送音频数据失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 发送摄像头数据
     */
    public void sendCameraData(byte[] imageData) {
        Log.i(TAG, String.format("📷 WebSocketManager.sendCameraData 被调用 - 数据大小: %d bytes, 连接状态: %s", 
            imageData.length, RDTDefine.getConnectionStateDescription(connectionState.get())));
            
        if (connectionState.get() != RDTDefine.ConnectionState.CONNECTED) {
            Log.w(TAG, "⚠️ 摄像头数据发送被跳过 - WebSocket未连接");
            return;
        }
        
        try {
            byte[] rdtMessage = RDTProtocol.createCameraMessage(imageData);
            webSocketClient.send(rdtMessage);
            
            sentBytes.addAndGet(rdtMessage.length);
            Log.i(TAG, String.format("✅ 摄像头数据发送成功: %d bytes -> %d bytes RDT", imageData.length, rdtMessage.length));
            
        } catch (Exception e) {
            Log.e(TAG, "发送摄像头数据失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 发送控制响应
     */
    public void sendControlResponse(int responseCode, String message) {
        if (connectionState.get() != RDTDefine.ConnectionState.CONNECTED) {
            return;
        }
        
        try {
            byte[] rdtMessage = RDTProtocol.createControlResponseMessage(responseCode, message);
            webSocketClient.send(rdtMessage);
            
            sentBytes.addAndGet(rdtMessage.length);
            Log.i(TAG, String.format("🎮 发送控制响应: code=%d, message=%s", responseCode, message));
            
        } catch (Exception e) {
            Log.e(TAG, "发送控制响应失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 发送文件响应
     */
    public void sendFileResponse(boolean success, String message) {
        if (connectionState.get() != RDTDefine.ConnectionState.CONNECTED) {
            return;
        }
        
        try {
            byte[] rdtMessage = RDTProtocol.createFileResponseMessage(success, message);
            webSocketClient.send(rdtMessage);
            
            sentBytes.addAndGet(rdtMessage.length);
            Log.i(TAG, String.format("📁 发送文件响应: success=%b, message=%s", success, message));
            
        } catch (Exception e) {
            Log.e(TAG, "发送文件响应失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 发送屏幕数据
     */
    public void sendScreenData(byte[] imageData, int width, int height) {
        if (connectionState.get() != RDTDefine.ConnectionState.CONNECTED) {
            Log.w(TAG, "WebSocket未连接，跳过数据发送");
            return;
        }
        
        try {
            // 使用RDTProtocol封装屏幕数据
            RDTMessage message = new RDTMessage();
            //message.writeInt(width);
            //message.writeInt(height);
            message.writeInt((int) System.currentTimeMillis());
            message.writeByteArray(imageData);
            
            byte[] rdtMessage = RDTProtocol.createRDTMessage(RDTDefine.RdtSignal.CS_SCREEN, message);
            webSocketClient.send(rdtMessage);
            
            // 更新统计数据
            long frameNum = sentFrames.incrementAndGet();
            sentBytes.addAndGet(rdtMessage.length);
            
            // 每100帧输出一次统计
            if (frameNum % 100 == 0) {
                long currentTime = System.currentTimeMillis();
                float timeDiff = (currentTime - lastStatsTime) / 1000.0f;
                float fps = 100.0f / timeDiff;
                float mbps = (rdtMessage.length * 100 * 8) / (timeDiff * 1024 * 1024);
                
                Log.i(TAG, String.format("📡 WebSocket发送统计 | 帧数: %d | FPS: %.1f | 速率: %.2f Mbps | 数据: %.1f KB", 
                       frameNum, fps, mbps, rdtMessage.length / 1024.0f));
                       
                lastStatsTime = currentTime;
            }
            
            // 通知监听器
            if (stateListener != null) {
                mainHandler.post(() -> stateListener.onScreenDataSent(frameNum, rdtMessage.length));
            }
            
            message.close();
            
        } catch (Exception e) {
            Log.e(TAG, "发送屏幕数据失败: " + e.getMessage(), e);
            if (stateListener != null) {
                mainHandler.post(() -> stateListener.onError("数据发送失败: " + e.getMessage()));
            }
        }
    }
    
    /**
     * 计划重连
     */
    private void scheduleReconnect() {
        retryCount++;
        updateConnectionState(RDTDefine.ConnectionState.RECONNECTING);
        
        Log.i(TAG, String.format("🔄 计划重连 (%d/%d) - %d秒后重试", 
               retryCount, RDTDefine.ScreenConfig.MAX_RETRY_COUNT, 
               RDTDefine.ScreenConfig.RECONNECT_DELAY_MS / 1000));
        
        mainHandler.postDelayed(this::connect, RDTDefine.ScreenConfig.RECONNECT_DELAY_MS);
    }
    
    /**
     * 更新连接状态
     */
    private void updateConnectionState(int newState) {
        int oldState = connectionState.getAndSet(newState);
        if (oldState != newState) {
            Log.i(TAG, String.format("🔄 连接状态变化: %s -> %s", 
                   RDTDefine.getConnectionStateDescription(oldState),
                   RDTDefine.getConnectionStateDescription(newState)));
            
            if (stateListener != null) {
                mainHandler.post(() -> stateListener.onConnectionStateChanged(newState));
            }
        }
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        if (webSocketClient != null) {
            Log.i(TAG, "🔌 主动断开WebSocket连接");
            webSocketClient.close();
            webSocketClient = null;
        }
        updateConnectionState(RDTDefine.ConnectionState.DISCONNECTED);
    }
    
    /**
     * 获取当前连接状态
     */
    public int getConnectionState() {
        return connectionState.get();
    }
    
    /**
     * 是否已连接
     */
    public boolean isConnected() {
        return connectionState.get() == RDTDefine.ConnectionState.CONNECTED;
    }
    
    /**
     * 获取发送统计
     */
    public long getSentFrames() {
        return sentFrames.get();
    }
    
    public long getSentBytes() {
        return sentBytes.get();
    }
    
    /**
     * 重置统计数据
     */
    public void resetStats() {
        sentFrames.set(0);
        sentBytes.set(0);
        lastStatsTime = System.currentTimeMillis();
    }
}
