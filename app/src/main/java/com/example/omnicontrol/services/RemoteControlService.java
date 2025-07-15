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
            
            Log.i(TAG, "Remote control service started successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start remote control service", e);
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
