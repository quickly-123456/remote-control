package com.example.omnicontrol.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.omnicontrol.MainActivity;
import com.example.omnicontrol.R;
import com.example.omnicontrol.managers.ScreenCaptureManager;
import com.example.omnicontrol.managers.CameraController;
import com.example.omnicontrol.managers.AudioCaptureManager;
import com.example.omnicontrol.managers.RemoteControlManager;

/**
 * 远程控制后台服务
 * 负责协调屏幕捕获、音视频传输、远程控制等功能
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
    
    // 服务状态
    private boolean serviceRunning = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "RemoteControlService onCreate");
        
        // 创建通知渠道
        createNotificationChannel();
        
        // 初始化管理器
        initializeManagers();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "RemoteControlService onStartCommand");
        
        if (intent != null) {
            String action = intent.getAction();
            if ("START_SERVICE".equals(action)) {
                startRemoteControlService();
            } else if ("STOP_SERVICE".equals(action)) {
                stopRemoteControlService();
            }
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
     * 初始化各功能管理器
     */
    private void initializeManagers() {
        screenCaptureManager = new ScreenCaptureManager(this);
        cameraController = new CameraController(this);
        audioCaptureManager = new AudioCaptureManager(this);
        remoteControlManager = new RemoteControlManager(this);
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
            
            Log.i(TAG, "Remote control service started successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start remote control service", e);
        }
    }
    
    /**
     * 设置权限功能的回调监听器
     */
    private void setupPermissionCallbacks() {
        // 设置音频数据回调
        if (audioCaptureManager != null) {
            audioCaptureManager.setAudioDataCallback(new AudioCaptureManager.AudioDataCallback() {
                @Override
                public void onAudioData(byte[] audioData, int length) {
                    // 音频数据已经在AudioCaptureManager中进行了日志输出
                    // 这里可以进行额外的处理，比如发送到服务器等
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Audio capture error: " + error);
                }
            });
        }
        
        // 设置摄像头数据回调
        if (cameraController != null) {
            cameraController.setCameraDataCallback(new CameraController.CameraDataCallback() {
                @Override
                public void onCameraData(byte[] data) {
                    // 图像数据已经在CameraController中进行了日志输出
                    // 这里可以进行额外的处理，比如发送到服务器等
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
                    Log.i(TAG, "Remote input state changed: " + enabled);
                }
                
                @Override
                public void onFileOperationResult(boolean success, String message) {
                    Log.i(TAG, "File operation result: " + success + ", " + message);
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Remote control error: " + error);
                }
            });
        }
    }
    
    /**
     * 停止远程控制服务
     */
    private void stopRemoteControlService() {
        if (!serviceRunning) {
            Log.d(TAG, "Service already stopped");
            return;
        }
        
        try {
            // 停止各功能模块
            if (screenCaptureManager != null) {
                screenCaptureManager.stopCapture();
            }
            if (cameraController != null) {
                cameraController.stopCamera();
            }
            if (audioCaptureManager != null) {
                audioCaptureManager.stopRecording();
            }
            if (remoteControlManager != null) {
                remoteControlManager.disconnect();
            }
            
            serviceRunning = false;
            stopForeground(true);
            
            Log.i(TAG, "Remote control service stopped successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping remote control service", e);
        }
    }
    
    /**
     * 启动屏幕捕获
     */
    public void startScreenCapture() {
        if (screenCaptureManager != null) {
            screenCaptureManager.startCapture();
        }
    }
    
    /**
     * 停止屏幕捕获
     */
    public void stopScreenCapture() {
        if (screenCaptureManager != null) {
            screenCaptureManager.stopCapture();
        }
    }
    
    /**
     * 启动摄像头
     */
    public void startCamera() {
        if (cameraController != null) {
            cameraController.startCamera();
        }
    }
    
    /**
     * 停止摄像头
     */
    public void stopCamera() {
        if (cameraController != null) {
            cameraController.stopCamera();
        }
    }
    
    /**
     * 启动音频录制
     */
    public void startAudioRecording() {
        if (audioCaptureManager != null) {
            audioCaptureManager.startRecording();
        }
    }
    
    /**
     * 停止音频录制
     */
    public void stopAudioRecording() {
        if (audioCaptureManager != null) {
            audioCaptureManager.stopRecording();
        }
    }
    
    /**
     * 启用远程输入
     */
    public void enableRemoteInput() {
        if (remoteControlManager != null) {
            remoteControlManager.enableRemoteInput();
        }
    }
    
    /**
     * 禁用远程输入
     */
    public void disableRemoteInput() {
        if (remoteControlManager != null) {
            remoteControlManager.disableRemoteInput();
        }
    }
    
    /**
     * 处理服务器命令
     */
    public void processServerCommand(String command) {
        if (remoteControlManager != null) {
            remoteControlManager.processServerCommand(command);
        }
    }
    
    /**
     * 打开指定应用
     */
    public void openApp(String appName) {
        if (remoteControlManager != null) {
            remoteControlManager.openApp(appName);
        }
    }
    
    /**
     * 启用文件访问权限
     */
    public void enableFileAccess() {
        if (remoteControlManager != null) {
            remoteControlManager.enableFileAccess();
        }
    }
    
    /**
     * 禁用文件访问权限
     */
    public void disableFileAccess() {
        if (remoteControlManager != null) {
            remoteControlManager.disableFileAccess();
        }
    }
    
    /**
     * 处理服务器文件上传
     */
    public void handleServerFileUpload(String fileName, byte[] fileData, String fileType) {
        if (remoteControlManager != null) {
            remoteControlManager.handleServerFileUpload(fileName, fileData, fileType);
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
        
        // 麦克风权限
        if (audioCaptureManager != null) {
            status.append("麦克风权限: ").append(audioCaptureManager.isRecording() ? "正在录制" : "未开启").append("\n");
        }
        
        // 摄像头权限
        if (cameraController != null) {
            status.append("摄像头权限: ").append(cameraController.isCameraOpen() ? "正在捕获" : "未开启").append("\n");
        }
        
        // 远程输入权限
        if (remoteControlManager != null) {
            status.append("远程输入权限: ").append(remoteControlManager.isRemoteInputEnabled() ? "已启用" : "未启用").append("\n");
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
