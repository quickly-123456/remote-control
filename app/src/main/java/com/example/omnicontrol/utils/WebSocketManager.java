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
    
    private ConnectionStateListener stateListener;
    
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
                    updateConnectionState(RDTDefine.ConnectionState.CONNECTED);
                    retryCount = 0;
                    
                    // 发送用户认证信息
                    sendUserAuth();
                }
                
                @Override
                public void onMessage(String message) {
                    Log.d(TAG, "📨 收到服务器消息: " + message);
                }
                
                @Override
                public void onMessage(java.nio.ByteBuffer bytes) {
                    Log.d(TAG, "📨 收到二进制消息: " + bytes.remaining() + " bytes");
                    // 使用RDTProtocol解析接收到的消息
                    handleReceivedMessage(bytes.array());
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
            RDTProtocol.RDTMessageInfo messageInfo = RDTProtocol.parseRDTMessage(data);
            if (messageInfo != null) {
                Log.d(TAG, "📨 解析RDT消息: " + messageInfo.getSignalTypeName());
                
                switch (messageInfo.signalType) {
                    case RDTDefine.RdtSignal.SC_CONTROL:
                        // 处理控制命令
                        String command = RDTProtocol.parseControlCommand(messageInfo.messageData);
                        Log.i(TAG, "🎮 收到控制命令: " + command);
                        break;
                        
                    case RDTDefine.RdtSignal.SC_FILE:
                        // 处理文件操作
                        RDTProtocol.FileOperationInfo fileOp = RDTProtocol.parseFileOperation(messageInfo.messageData);
                        if (fileOp != null) {
                            Log.i(TAG, String.format("📁 收到文件操作: %s (%s, %d bytes)", 
                                fileOp.fileName, fileOp.fileType, fileOp.fileData.length));
                        }
                        break;
                        
                    default:
                        Log.d(TAG, "📨 未处理的消息类型: " + messageInfo.getSignalTypeName());
                        break;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "处理接收消息失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 发送用户认证信息（用户登录成功后立即调用）
     * @param phone 用户手机号
     * @param userId 用户ID
     */
    public void sendUserAuthSignal(String phone, String userId) {
        this.phoneNumber = phone;
        this.userId = userId;
        
        // 如果 WebSocket 已连接，立即发送
        if (isConnected()) {
            sendUserAuth();
        } else {
            // 如果未连接，先建立连接再发送
            Log.i(TAG, "🌐 WebSocket 未连接，建立连接以发送 CS_USER 信号");
            connect();
        }
    }
    
    /**
     * 发送用户认证信息（内部方法）
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
