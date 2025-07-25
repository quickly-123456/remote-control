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
    
    // RDT信号类型枚举 - 根据官方API文档更新
    public static class RdtSignal {
        // 用户连接信号
        public static final int CS_USER = 255;       // 0xFF - Client to Server - Android用户连接
        public static final int SC_USER = 256;       // 0x100 - Server to Client - 用户连接确认
        
        // Vue Web控制台信号
        public static final int CS_VUE = 257;        // 0x101 - Client to Server - Vue Web连接
        public static final int SC_VUE = 258;        // 0x102 - Server to Client - Vue Web成员信息
        
        // 屏幕共享信号
        public static final int CS_SCREEN = 259;     // 0x103 - Client to Server - 屏幕数据
        public static final int SC_SCREEN = 260;     // 0x104 - Server to Client - 屏幕数据
        
        // 网络连接信号
        public static final int SC_DISCONNECTED = 262; // 0x106 - Server to Client - 用户断开连接
        
        // Android管理员信号
        public static final int CS_MOBILE_ADMIN = 263; // 0x107 - Client to Server - Android管理员连接
        public static final int SC_MOBILE_ADMIN = 264; // 0x108 - Server to Client - Android管理员成员信息
        
        // 设备控制信号
        public static final int CS_ONOFF = 265;      // 0x109 - Client to Server - 设备开关控制
        public static final int SC_ONOFF = 266;      // 0x10A - Server to Client - 设备开关状态
        
        // 触摸控制信号 - 关键信号！
        public static final int CS_TOUCHED = 267;    // 0x10B - Client to Server - 发送触摸坐标
        public static final int SC_TOUCHED = 268;    // 0x10C - Server to Client - 接收触摸坐标
        
        // 摄像头信号
        public static final int CS_CAMERA = 269;     // 0x10D - Client to Server - 摄像头数据
        public static final int SC_CAMERA = 270;     // 0x10E - Server to Client - 摄像头数据
        
        // 音频信号
        public static final int CS_RECORDED_AUDIO = 271; // 0x10F - Client to Server - 音频数据
        public static final int SC_RECORDED_AUDIO = 272; // 0x110 - Server to Client - 音频数据
        
        // 手机选择信号
        public static final int CS_SELECTED_PHONE = 273; // 0x111 - Client to Server - 选择手机
        
        // 保持兼容性 - 旧的信号名称(已废弃)
        @Deprecated
        public static final int CS_AUDIO = CS_RECORDED_AUDIO;
        @Deprecated  
        public static final int SC_AUDIO = SC_RECORDED_AUDIO;
        @Deprecated
        public static final int CS_CONTROL = CS_ONOFF;
        @Deprecated
        public static final int SC_CONTROL = SC_ONOFF;
        @Deprecated
        public static final int CS_FILE = CS_TOUCHED;
        @Deprecated
        public static final int SC_FILE = SC_TOUCHED;
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
