package com.example.omnicontrol.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.omnicontrol.MainActivity;
import com.example.omnicontrol.R;
import com.example.omnicontrol.managers.ScreenCaptureManager;
import com.example.omnicontrol.managers.CameraController;
import com.example.omnicontrol.managers.AudioCaptureManager;
import com.example.omnicontrol.managers.RemoteControlManager;
import com.example.omnicontrol.managers.TouchControlHandler;
// 已移除BinaryProtocolService相关导入，全部使用RDT+WebSocket体系
import com.example.omnicontrol.utils.RDTProtocol;
import com.example.omnicontrol.utils.RDTDefine;

/**
 * 远程控制后台服务
 * 负责协调屏幕捕获、音视频传输、远程控制等功能
 * 集成Binary Message协议实现实时数据传输和远程控制
 */
public class RemoteControlService extends Service {
    private static final String TAG = "RemoteControlService";
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "remote_control_channel";
    
    // 功能管理器
    private ScreenCaptureManager screenCaptureManager;
    private CameraController cameraController;
    private AudioCaptureManager audioCaptureManager;
    private RemoteControlManager remoteControlManager;
    private TouchControlHandler touchControlHandler;
    
    // 网络协议服务（已移除BinaryProtocolService，改用RDT+WebSocket）
    
    // 服务状态
    private boolean serviceRunning = false;
    private boolean cameraEnabled = false;
    private boolean audioEnabled = false;
    private boolean remoteInputEnabled = false;
    
    // 周期性数据发送
    private android.os.Handler dataHandler;
    private Runnable dataRunnable;
    
    @Override
    public void onCreate() {
        super.onCreate();
        
        // 强制系统级日志，确保可见
        android.util.Log.e("OMNI_SERVICE", "==================== REMOTE CONTROL SERVICE STARTING ====================");
        android.util.Log.e("OMNI_SERVICE", "🚀🚀🚀 RemoteControlService onCreate - 服务正在启动！");
        Log.i(TAG, "🚀🚀🚀 RemoteControlService onCreate - 服务正在启动！");
        
        // 创建通知渠道
        createNotificationChannel();
        
        // 初始化管理器
        initializeManagers();
        
        // 已移除BinaryProtocolService初始化，现在由各组件自管WebSocket连接
        
        android.util.Log.e("OMNI_SERVICE", "✅✅✅ RemoteControlService onCreate 完成！");
        Log.i(TAG, "✅✅✅ RemoteControlService onCreate 完成！");
    }
    
    /**
     * 初始化各功能管理器
     */
    private void initializeManagers() {
        screenCaptureManager = new ScreenCaptureManager(this);
        cameraController = new CameraController(this);
        audioCaptureManager = new AudioCaptureManager(this);
        remoteControlManager = new RemoteControlManager(this);
        touchControlHandler = new TouchControlHandler(this);
    }
    
    // 已移除initializeBinaryProtocol方法，改用RDT+WebSocket体系
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "📢📢📢 RemoteControlService onStartCommand 被调用！");
        
        if (intent != null) {
            String action = intent.getAction();
            Log.i(TAG, "🎯 Intent action: " + action);
            
            if ("START_SERVICE".equals(action)) {
                Log.i(TAG, "🚀 开始启动 RemoteControlService...");
                startRemoteControlService();
            } else if ("STOP_SERVICE".equals(action)) {
                Log.i(TAG, "🛑 开始停止 RemoteControlService...");
                stopRemoteControlService();
            }
        } else {
            Log.w(TAG, "⚠️ Intent 为 null，无法确定操作");
        }
        
        return START_STICKY; // 服务被杀死后自动重启
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null; // 不支持绑定
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "RemoteControlService onDestroy");
        stopRemoteControlService();
    }
    
    /**
     * 启动远程控制服务
     */
    private void startRemoteControlService() {
        if (serviceRunning) {
            Log.d(TAG, "Service already running");
            return;
        }
        
        try {
            // 启动前台服务
            startForeground(NOTIFICATION_ID, createNotification());
            serviceRunning = true;
            
            // 设置权限功能的回调监听器
            setupPermissionCallbacks();
            
            // 等待连接建立后自动开启音频和摄像头功能
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                autoStartFeatures();
            }, 2000); // 延迟2秒确保连接建立
            
            Log.i(TAG, "Remote control service started successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start remote control service", e);
        }
    }
    
    /**
     * 自动启动摄像头和音频功能
     */
    private void autoStartFeatures() {
        Log.i(TAG, "🎬🎬🎬 autoStartFeatures() 被调用！");
        Log.i(TAG, "📊 状态检查 - serviceRunning: " + serviceRunning + " (已移除BinaryProtocolService，改用RDT+WebSocket体系)");
        
        if (!serviceRunning) {
            Log.w(TAG, "⚠️ 服务未运行，跳过自动启动");
            return;
        }
        
        // 不再检查BinaryProtocolService连接状态，直接启动功能（WebSocket由各组件自己管理）
        Log.d(TAG, "🌐 新架构: CameraController和AudioCaptureManager自管WebSocket连接");
        
        // 检查管理器状态
        Log.d(TAG, "📹 CameraController: " + (cameraController != null ? "已初始化" : "null"));
        Log.d(TAG, "🎤 AudioCaptureManager: " + (audioCaptureManager != null ? "已初始化" : "null"));
        
        // 启动摄像头数据采集
        Log.d(TAG, "📹 正在启动摄像头...");
        startCamera();
        Log.d(TAG, "📹 摄像头状态: cameraEnabled=" + cameraEnabled);
        
        // 启动音频数据采集
        Log.d(TAG, "🎤 正在启动音频录制...");
        startAudioRecording();
        Log.d(TAG, "🎤 音频状态: audioEnabled=" + audioEnabled);
        
        // 总结状态
        Log.i(TAG, String.format("✅ 功能启动完成 - 摄像头:%s, 音频:%s, 架构:%s", 
            cameraEnabled ? "开启" : "关闭", 
            audioEnabled ? "开启" : "关闭",
            "RDT+WebSocket"));
        
        // 已移除旧的数据发送测试方法，现在由CameraController和AudioCaptureManager自管WebSocket推送
        Log.i(TAG, "📡 数据推送由各组件自管WebSocket连接，无需统一协调");
    }
    
    /**
        // 模拟不同大小的摄像头帧（1KB-5KB）
        int frameSize = 1024 + (frameNumber % 4) * 1024;
        byte[] frame = new byte[frameSize];
        
        // 填充一些测试数据
        for (int i = 0; i < frame.length; i++) {
            frame[i] = (byte) ((frameNumber + i) % 256);
        }
        
        return frame;
    }
    
    /**
     * 生成测试音频包数据
     */
    private byte[] generateTestAudioPacket(int packetNumber) {
        // 模拟音频包（512字节）
        byte[] packet = new byte[512];
        
        // 填充一些测试数据
        for (int i = 0; i < packet.length; i++) {
            packet[i] = (byte) ((packetNumber * 10 + i) % 256);
        }
        
        return packet;
    }
    
    // testDataSending方法已移除 - 不再使用BinaryProtocolService进行数据发送
    // 现在由CameraController和AudioCaptureManager自管WebSocket连接和数据推送
    
    /**
     * 设置权限功能的回调监听器
     */
    private void setupPermissionCallbacks() {
        // 设置音频数据回调（简化版 - AudioCaptureManager自管WebSocket推送）
        if (audioCaptureManager != null) {
            audioCaptureManager.setAudioDataCallback(new AudioCaptureManager.AudioDataCallback() {
                @Override
                public void onAudioData(byte[] audioData, int length) {
                    // AudioCaptureManager现在自管WebSocket推送，这里只需要日志确认
                    Log.v(TAG, String.format("🎤 音频数据回调: 大小=%d bytes (由AudioCaptureManager自管WebSocket推送)", length));
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Audio capture error: " + error);
                }
            });
        }
        
        // 设置摄像头数据回调 - 集成CS_CAMERA信号发送
        if (cameraController != null) {
            cameraController.setCameraDataCallback(new CameraController.CameraDataCallback() {
                @Override
                public void onCameraData(byte[] data) {
                    // 如果摄像头功能启用且连接正常，发送摄像头数据
                    if (cameraEnabled) {
                        // 摄像头数据已由CameraController自管WebSocket推送，此回调仅作日志记录
                        Log.d(TAG, String.format("📷 摄像头数据回调: 大小=%d bytes (由CameraController自管WebSocket推送)", data.length));
                    }
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Camera capture error: " + error);
                }
            });
        }
        
        // 设置远程控制回调
        if (remoteControlManager != null) {
            remoteControlManager.setRemoteControlCallback(new RemoteControlManager.RemoteControlCallback() {
                @Override
                public void onRemoteInputStateChanged(boolean enabled) {
                    remoteInputEnabled = enabled;
                    Log.d(TAG, "Remote input state changed: " + enabled);
                }
                
                @Override
                public void onFileOperationResult(boolean success, String message) {
                    Log.d(TAG, "File operation result: " + success + ", " + message);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Remote control error: " + error);
                }
                
            });
        }
        
        // 设置摄像头数据回调 - 集成CS_CAMERA信号发送
        if (cameraController != null) {
            cameraController.setCameraDataCallback(new CameraController.CameraDataCallback() {
                @Override
                public void onCameraData(byte[] data) {
                    // 如果摄像头功能启用且连接正常，发送摄像头数据
                    if (cameraEnabled) {
                        // 摄像头数据已由CameraController自管WebSocket推送，此回调仅作日志记录
                        Log.d(TAG, String.format("📷 摄像头数据回调: 大小=%d bytes (由CameraController自管WebSocket推送)", data.length));
                    }
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Camera capture error: " + error);
                }
            });
        }
    }
    
    /**
     * 停止远程控制服务
     */
    private void stopRemoteControlService() {
        if (!serviceRunning) {
            return;
        }
        
        serviceRunning = false;
        
        // 停止周期性数据发送
        if (dataHandler != null && dataRunnable != null) {
            dataHandler.removeCallbacks(dataRunnable);
            Log.d(TAG, "🔄 周期性数据发送已停止");
        }
        
        // 停止摄像头
        stopCamera();
        
        // 停止音频录制
        stopAudioRecording();
        
        // 已移除BinaryProtocolService断开连接逻辑，现在由各组件自管WebSocket连接
        
        // 停止远程控制管理器
        if (remoteControlManager != null) {
            remoteControlManager.disconnect();
        }
        
        // 停止前台服务
        stopForeground(true);
        
        Log.i(TAG, "Remote control service stopped successfully");
    }
    
    /**
     * 启动摄像头 - 启用CS_CAMERA信号发送
     * 必须先检查系统权限，防止绕过权限检查启动功能
     */
    public void startCamera() {
        Log.d(TAG, "📹 startCamera() 调用 - cameraController: " + (cameraController != null ? "存在" : "null") + ", cameraEnabled: " + cameraEnabled);
        
        // 🔒 强制检查系统权限，防止绕过权限启动功能
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "❌ 系统摄像头权限未授予，拒绝启动摄像头功能");
            cameraEnabled = false;
            return;
        }
        
        if (cameraController != null && !cameraEnabled) {
            try {
                Log.d(TAG, "📹 系统权限检查通过，正在启动CameraController...");
                cameraController.startCamera();
                cameraEnabled = true;
                Log.i(TAG, "✅ 摄像头已启动，CS_CAMERA信号已启用");
                
                // 检查摄像头是否正在运行
                if (cameraController.isCameraOpen()) {
                    Log.d(TAG, "📹 摄像头确认已打开");
                } else {
                    Log.w(TAG, "⚠️ 摄像头未成功打开");
                    cameraEnabled = false;
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ 启动摄像头失败", e);
                cameraEnabled = false;
            }
        } else {
            if (cameraController == null) {
                Log.w(TAG, "⚠️ CameraController为null，无法启动摄像头");
            } else if (cameraEnabled) {
                Log.d(TAG, "📹 摄像头已在运行中");
            }
        }
    }
    
    /**
     * 停止摄像头
     */
    public void stopCamera() {
        if (cameraController != null && cameraEnabled) {
            cameraController.stopCamera();
            cameraEnabled = false;
            Log.i(TAG, "Camera stopped and CS_CAMERA signal disabled");
        }
    }
    
    /**
     * 启动音频录制 - 启用CS_RECORDED_AUDIO信号发送
     * 必须先检查系统权限，防止绕过权限检查启动功能
     */
    public void startAudioRecording() {
        Log.d(TAG, "🎤 startAudioRecording() 调用 - audioCaptureManager: " + (audioCaptureManager != null ? "存在" : "null") + ", audioEnabled: " + audioEnabled);
        
        // 🔒 强制检查系统权限，防止绕过权限启动功能
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "❌ 系统麦克风权限未授予，拒绝启动音频录制功能");
            audioEnabled = false;
            return;
        }
        
        if (audioCaptureManager != null && !audioEnabled) {
            try {
                Log.d(TAG, "🎤 系统权限检查通过，正在启动AudioCaptureManager...");
                audioCaptureManager.startRecording();
                audioEnabled = true;
                Log.i(TAG, "✅ 音频录制已启动，CS_RECORDED_AUDIO信号已启用");
                
                // 检查音频是否正在录制
                if (audioCaptureManager.isRecording()) {
                    Log.d(TAG, "🎤 音频确认正在录制");
                } else {
                    Log.w(TAG, "⚠️ 音频未成功开始录制");
                    audioEnabled = false;
                }
            } catch (Exception e) {
                Log.e(TAG, "❌ 启动音频录制失败", e);
                audioEnabled = false;
            }
        } else {
            if (audioCaptureManager == null) {
                Log.w(TAG, "⚠️ AudioCaptureManager为null，无法启动音频录制");
            } else if (audioEnabled) {
                Log.d(TAG, "🎤 音频录制已在进行中");
            }
        }
    }
    
    /**
     * 停止音频录制
     */
    public void stopAudioRecording() {
        if (audioCaptureManager != null && audioEnabled) {
            audioCaptureManager.stopRecording();
            audioEnabled = false;
            Log.i(TAG, "Audio recording stopped and CS_RECORDED_AUDIO signal disabled");
        }
    }
    
    /**
     * 启用远程输入 - 准备接收SC_TOUCHED信号
     */
    public void enableRemoteInput() {
        if (remoteControlManager != null) {
            remoteControlManager.enableRemoteInput();
            Log.i(TAG, "Remote input enabled, ready to receive SC_TOUCHED signals");
        }
    }
    
    /**
     * 禁用远程输入
     */
    public void disableRemoteInput() {
        if (remoteControlManager != null) {
            remoteControlManager.disableRemoteInput();
            Log.i(TAG, "Remote input disabled");
        }
    }
    
    // BinaryProtocolService.ProtocolListener实现已移除 - 不再使用Binary Protocol
    // 现在由RDT+WebSocket体系处理所有数据传输
    
    /**
     * 处理服务器发送的消息（包括SC_TOUCHED等）
     */
    public void handleServerMessage(byte[] messageData) {
        try {
            // 先检查是否为SC_TOUCHED信号（直接坐标数据，非RDT格式）
            if (messageData.length == 12) { // 4bytes signal + 4bytes x + 4bytes y
                try {
                    // 解析信号类型
                    int signal = java.nio.ByteBuffer.wrap(messageData, 0, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
                    if (signal == 0x10C) { // SC_TOUCHED = 268 = 0x10C
                        // 解析坐标
                        int x = java.nio.ByteBuffer.wrap(messageData, 4, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
                        int y = java.nio.ByteBuffer.wrap(messageData, 8, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();
                        
                        Log.d(TAG, String.format("👆 接收到SC_TOUCHED信号: 坐标=(%d, %d)", x, y));
                        
                        // 使用TouchControlHandler处理触摸事件
                        if (touchControlHandler != null) {
                            touchControlHandler.handleTouchEvent(x, y);
                        } else {
                            Log.w(TAG, "TouchControlHandler未初始化");
                        }
                        return;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析SC_TOUCHED信号失败", e);
                }
            }
            
            // 处理其他RDT消息
            RDTProtocol.RDTMessageInfo messageInfo = RDTProtocol.parseRDTMessage(messageData);
            if (messageInfo == null) {
                Log.w(TAG, "Failed to parse RDT message");
                return;
            }
            
            Log.d(TAG, String.format("收到服务器消息: 信号类型=%s (0x%X)", 
                messageInfo.getSignalTypeName(), messageInfo.signalType));
            
            switch (messageInfo.signalType) {
                case RDTDefine.RdtSignal.SC_CONTROL:
                    handleControlMessage(messageInfo.messageData);
                    break;
                    
                case RDTDefine.RdtSignal.SC_FILE:
                    handleFileMessage(messageInfo.messageData);
                    break;
                    
                default:
                    Log.d(TAG, "未处理的消息类型: " + messageInfo.getSignalTypeName());
                    break;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "处理服务器消息失败", e);
        }
    }
    
    /**
     * 处理SC_CONTROL控制消息（非触摸事件）
     */
    private void handleControlMessage(byte[] messageData) {
        try {
            String command = RDTProtocol.parseControlCommand(messageData);
            Log.d(TAG, String.format("接收到控制命令: %s", command));
            
            // 处理其他控制命令（非触摸事件）
            Log.d(TAG, "其他控制命令: " + command);
            
        } catch (Exception e) {
            Log.e(TAG, "处理控制消息失败", e);
        }
    }
    
    /**
     * 处理SC_FILE文件消息
     */
    private void handleFileMessage(byte[] messageData) {
        try {
            RDTProtocol.FileOperationInfo fileInfo = RDTProtocol.parseFileOperation(messageData);
            if (fileInfo != null) {
                Log.d(TAG, String.format("收到文件操作: 文件名=%s, 类型=%s, 大小=%d bytes", 
                    fileInfo.fileName, fileInfo.fileType, fileInfo.fileData.length));
                
                // 委托给RemoteControlManager处理文件操作
                if (remoteControlManager != null) {
                    // 这里可以调用remoteControlManager的文件处理方法
                    Log.d(TAG, "文件操作已委托给RemoteControlManager处理");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "处理文件消息失败", e);
        }
    }
    
    // onDisconnectedByServer和onConnectionStatusChanged方法已移除 - 不再实现Binary Protocol接口
    // 现在由WebSocket连接状态管理器处理连接状态变化
    
    public void handleConnectionStatusChange(boolean connected) {
        Log.i(TAG, "WebSocket连接状态变化: " + (connected ? "已连接" : "已断开"));
        
        if (!connected) {
            // 连接断开时，停止数据发送
            cameraEnabled = false;
            audioEnabled = false;
        }
    }
    
    /**
     * 获取所有权限功能的状态
     */
    public String getPermissionStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== 全视界远程控制权限状态 ===\n");
        
        // 服务状态
        status.append("服务状态: ").append(serviceRunning ? "运行中" : "已停止").append("\n");
        status.append("服务器连接: ").append("WebSocket连接 (由各组件自管)").append("\n");
        
        // 麦克风权限 (CS_RECORDED_AUDIO)
        if (audioCaptureManager != null) {
            status.append("麦克风权限: ").append(audioEnabled && audioCaptureManager.isRecording() ? "正在录制并发送" : "未开启").append("\n");
        }
        
        // 摄像头权限 (CS_CAMERA)
        if (cameraController != null) {
            status.append("摄像头权限: ").append(cameraEnabled && cameraController.isCameraOpen() ? "正在捕获并发送" : "未开启").append("\n");
        }
        
        // 远程输入权限 (SC_TOUCHED)
        if (remoteControlManager != null) {
            status.append("远程输入权限: ").append(remoteInputEnabled ? "已启用，可接收触摸控制" : "未启用").append("\n");
        }
        
        // 文件访问权限
        if (remoteControlManager != null) {
            status.append("文件访问权限: ").append(remoteControlManager.isFileAccessEnabled() ? "已启用" : "未启用").append("\n");
        }
        
        status.append("================================\n");
        return status.toString();
    }
    
    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "远程控制服务",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("远程控制后台服务通知");
            channel.setShowBadge(false);
            
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
    
    /**
     * 创建前台服务通知
     */
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, 
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("全视界远程控制")
            .setContentText("远程控制服务正在后台运行")
            .setSmallIcon(R.drawable.ic_device)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build();
    }
    
    /**
     * 获取服务运行状态
     */
    public boolean isServiceRunning() {
        return serviceRunning;
    }
}
