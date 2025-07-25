package com.zhongkong.app.utils;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.java_websocket.enums.ReadyState;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket 管理类
 * 功能：
 * 1. 连接/断开 WebSocket
 * 2. 自动重连机制
 * 3. 消息发送与接收
 * 4. 连接状态监听
 * 5. 心跳检测
 */
public class WebSocketManager {

    // 单例实例
    private static volatile WebSocketManager instance;
    
    // WebSocket 客户端
    private MyWebSocketClient webSocketClient;
    
    // WebSocket 服务器地址
    private String wsUrl = "ws://185.128.227.222:5050";
    
    // 连接状态监听器
    private WebSocketListener listener;
    
    // 自动重连调度器
    private final ScheduledExecutorService reconnectExecutor = Executors.newSingleThreadScheduledExecutor();
    
    // 心跳调度器
    private final ScheduledExecutorService heartbeatExecutor = Executors.newSingleThreadScheduledExecutor();
    
    // 配置参数
    private int heartbeatInterval = 30000; // 心跳间隔(毫秒)
    private boolean autoReconnect = true; // 是否自动重连
    private boolean enableHeartbeat = false; // 是否启用心跳
    private int reconnectInterval = 3000; // 重连间隔(毫秒)
    private int maxReconnectAttempts = 30; // 最大重连尝试次数
    private int currentReconnectCount = 0;
    
    // 私有构造方法
    private WebSocketManager() {}
    
    /**
     * 获取单例实例
     */
    public static WebSocketManager getInstance() {
        if (instance == null) {
            synchronized (WebSocketManager.class) {
                if (instance == null) {
                    instance = new WebSocketManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 初始化 WebSocket
     * @param listener 连接状态监听器
     */
    public void init(WebSocketListener listener) {
        this.listener = listener;
    }
    
    /**
     * 连接 WebSocket
     */
    public void connect() {

        if(currentReconnectCount > maxReconnectAttempts){
            if(!reconnectExecutor.isShutdown()) reconnectExecutor.isShutdown();
            return;
        }

        if (wsUrl == null || wsUrl.isEmpty()) {
            throw new IllegalStateException("WebSocket URL is not initialized");
        }
        
        if (isConnected()) {
            disconnect();
        }
        
        try {
            webSocketClient = new MyWebSocketClient(new URI(wsUrl));
            webSocketClient.connect();
        } catch (URISyntaxException e) {
            if (listener != null) {
                listener.onError(e);
            }
        }

        currentReconnectCount++;
    }

    public WebSocketClient getWebSocketClient(){
        return webSocketClient;
    }
    
    /**
     * 断开 WebSocket 连接
     */
    public void disconnect() {
        if (webSocketClient != null) {
            // 关闭自动重连
            autoReconnect = false;
            
            // 停止心跳
            stopHeartbeat();
            
            // 关闭连接
            webSocketClient.close();
            webSocketClient = null;
        }
    }
    
    /**
     * 发送消息
     * @param message 要发送的消息
     * @return 是否发送成功
     */
    public boolean sendMessage(String message) {
        if (isConnected()) {
            webSocketClient.send(message);
            return true;
        }
        return false;
    }
    
    /**
     * 发送二进制消息
     * @param data 二进制数据
     * @return 是否发送成功
     */
    public boolean sendBinary(byte[] data) {
        if (isConnected()) {
            webSocketClient.send(data);
            return true;
        }
        return false;
    }
    
    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return webSocketClient != null && 
               webSocketClient.getReadyState() == ReadyState.OPEN;
    }
    
    /**
     * 开始心跳检测
     */
    private void startHeartbeat() {
        if (!enableHeartbeat) return;
        
        stopHeartbeat();
        
//        heartbeatExecutor.scheduleAtFixedRate(() -> {
//            if (isConnected()) {
//                // 发送心跳消息
//                sendMessage("{\"type\":\"heartbeat\"}");
//            }
//        }, heartbeatInterval, heartbeatInterval, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 停止心跳检测
     */
    private void stopHeartbeat() {
        heartbeatExecutor.shutdownNow();
    }
    
    /**
     * 开始自动重连
     */
    private void scheduleReconnect() {
        currentReconnectCount = 0;

        if (!autoReconnect) return;

        reconnectExecutor.schedule(() -> {
            if (!isConnected()) {
                connect();
            }
        }, reconnectInterval, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 设置自动重连
     */
    public void setAutoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
    }
    
    /**
     * 设置重连间隔
     */
    public void setReconnectInterval(int milliseconds) {
        this.reconnectInterval = milliseconds;
    }
    
    /**
     * 设置心跳检测
     */
    public void setEnableHeartbeat(boolean enable) {
        this.enableHeartbeat = enable;
        if (enable && isConnected()) {
            startHeartbeat();
        } else {
            stopHeartbeat();
        }
    }
    
    /**
     * 设置心跳间隔
     */
    public void setHeartbeatInterval(int milliseconds) {
        this.heartbeatInterval = milliseconds;
        if (enableHeartbeat && isConnected()) {
            stopHeartbeat();
            startHeartbeat();
        }
    }
    
    /**
     * 自定义 WebSocket 客户端
     */
    private class MyWebSocketClient extends WebSocketClient {
        
        public MyWebSocketClient(URI serverUri) {
            super(serverUri);
        }
        
        @Override
        public void onOpen(ServerHandshake handshakedata) {
            // 连接成功
            if (listener != null) {
                listener.onConnected();
            }

            if(!reconnectExecutor.isShutdown()) reconnectExecutor.shutdownNow();

            // 开始心跳
            startHeartbeat();
        }
        
        @Override
        public void onMessage(String message) {
            // 收到文本消息
            if (listener != null) {
                listener.onMessage(message);
            }
        }
        
        @Override
        public void onMessage(java.nio.ByteBuffer bytes) {
            // 收到二进制消息
            if (listener != null) {
                listener.onBinaryMessage(bytes.array());
            }
        }
        
        @Override
        public void onClose(int code, String reason, boolean remote) {
            // 连接关闭
            if (listener != null) {
                listener.onDisconnected(code, reason, remote);
            }
            
            // 停止心跳
            stopHeartbeat();
            
            // 自动重连
            scheduleReconnect();
        }
        
        @Override
        public void onError(Exception ex) {
            // 发生错误
            if (listener != null) {
                listener.onError(ex);
            }
        }
    }
    
    /**
     * WebSocket 事件监听接口
     */
    public interface WebSocketListener {
        /**
         * 连接成功
         */
        void onConnected();
        
        /**
         * 连接断开
         * @param code 关闭代码
         * @param reason 关闭原因
         * @param remote 是否由远程服务器关闭
         */
        void onDisconnected(int code, String reason, boolean remote);
        
        /**
         * 收到文本消息
         * @param message 消息内容
         */
        void onMessage(String message);
        
        /**
         * 收到二进制消息
         * @param data 二进制数据
         */
        void onBinaryMessage(byte[] data);
        
        /**
         * 发生错误
         * @param ex 异常信息
         */
        void onError(Exception ex);
    }
}