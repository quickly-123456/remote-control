package com.example.omnicontrol.utils;

/**
 * RDT协议定义 - Java版本
 * 对应C++版本的rdtDefine.h
 * RDT means remote Android
 */
public class RDTDefine {
    
    // RDT通信端口
    public static final int RDT_PORT = 5050;
    
    // WebSocket服务器地址
    public static final String WS_SERVER_URL = "ws://185.128.227.222:5050";
    
    // RDT信号类型枚举
    public static class RdtSignal {
        public static final int CS_USER = 0xFF;      // Client to Server - 用户信息
        public static final int SC_USER = 0x100;     // Server to Client - 用户信息
        
        public static final int CS_VUE = 0x101;      // Client to Server - Vue页面
        public static final int SC_VUE = 0x102;      // Server to Client - Vue页面
        
        public static final int CS_SCREEN = 0x103;   // Client to Server - 屏幕数据
        public static final int SC_SCREEN = 0x104;   // Server to Client - 屏幕数据
    }
    
    // 屏幕捕获配置
    public static class ScreenConfig {
        public static final int TARGET_FPS = 25;
        public static final int WEBP_QUALITY = 80;
        public static final int MAX_RETRY_COUNT = 3;
        public static final long RECONNECT_DELAY_MS = 5000; // 5秒重连延迟
    }
    
    // 连接状态
    public static class ConnectionState {
        public static final int DISCONNECTED = 0;
        public static final int CONNECTING = 1;
        public static final int CONNECTED = 2;
        public static final int RECONNECTING = 3;
        public static final int ERROR = 4;
    }
    
    // 获取状态描述
    public static String getConnectionStateDescription(int state) {
        switch (state) {
            case ConnectionState.DISCONNECTED:
                return "已断开";
            case ConnectionState.CONNECTING:
                return "连接中";
            case ConnectionState.CONNECTED:
                return "已连接";
            case ConnectionState.RECONNECTING:
                return "重连中";
            case ConnectionState.ERROR:
                return "连接错误";
            default:
                return "未知状态";
        }
    }
}
