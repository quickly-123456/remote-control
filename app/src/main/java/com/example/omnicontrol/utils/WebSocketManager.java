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
    
    /**
     * 带Context的构造函数，用于获取用户信息
     */
    public WebSocketManager(Context context) {
        this.context = context;
        mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public void setConnectionStateListener(ConnectionStateListener listener) {
        this.stateListener = listener;
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
     * 发送用户认证信息
     */
    private void sendUserAuth() {
        try {
            String phoneNumber = "";
            String superId = "";
            
            // 如果有Context，从 UserManager 获取真实的用户信息
            if (context != null) {
                try {
                    UserManager userManager = new UserManager(context);
                    phoneNumber = userManager.getCurrentUsername(); // 手机号
                    superId = userManager.getSuperID(); // Super ID（登录接口返回的super_id）
                    
                    if (phoneNumber == null) phoneNumber = "";
                    if (superId == null) superId = "";
                    
                } catch (Exception e) {
                    Log.w(TAG, "获取用户信息失败，使用默认值: " + e.getMessage());
                }
            }
            
            // 如果没有获取到有效的用户信息，使用默认值
            if (phoneNumber.isEmpty()) {
                phoneNumber = "default_phone";
            }
            if (superId.isEmpty()) {
                superId = "default_super";
            }
            
            RDTMessage message = new RDTMessage();
            message.writeInt(RDTDefine.RdtSignal.CS_USER)
                   .writeString(phoneNumber)
                   .writeString(superId);
            
            byte[] data = message.getData();
            webSocketClient.send(data);
            
            Log.i(TAG, "🔐 已发送用户认证信息: 手机号=" + phoneNumber + ", Super ID=" + superId);
            message.close();
            
        } catch (Exception e) {
            Log.e(TAG, "发送用户认证失败: " + e.getMessage(), e);
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
            RDTMessage message = new RDTMessage();
            
            // 构造屏幕数据消息
            message.writeInt(RDTDefine.RdtSignal.CS_SCREEN)  // 信号类型
                   .writeInt((int) System.currentTimeMillis()) // 时间戳
                   .writeRawData(imageData);                 // 图像数据
            
            byte[] data = message.getData();
            webSocketClient.send(data);
            
            // 更新统计数据
            long frameNum = sentFrames.incrementAndGet();
            sentBytes.addAndGet(data.length);
            
            // 每100帧输出一次统计
            if (frameNum % 100 == 0) {
                long currentTime = System.currentTimeMillis();
                float timeDiff = (currentTime - lastStatsTime) / 1000.0f;
                float fps = 100.0f / timeDiff;
                float mbps = (data.length * 100 * 8) / (timeDiff * 1024 * 1024);
                
                Log.i(TAG, String.format("📡 WebSocket发送统计 | 帧数: %d | FPS: %.1f | 速率: %.2f Mbps | 数据: %.1f KB", 
                       frameNum, fps, mbps, data.length / 1024.0f));
                       
                lastStatsTime = currentTime;
            }
            
            // 通知监听器
            if (stateListener != null) {
                mainHandler.post(() -> stateListener.onScreenDataSent(frameNum, data.length));
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
