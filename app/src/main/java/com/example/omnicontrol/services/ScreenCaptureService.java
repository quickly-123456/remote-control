package com.example.omnicontrol.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.omnicontrol.MainActivity;
import com.example.omnicontrol.R;
import com.example.omnicontrol.managers.ScreenCaptureManager;

/**
 * 屏幕捕获前台服务
 * 用于满足Android 14+对MediaProjection的前台服务要求
 */
public class ScreenCaptureService extends Service {
    private static final String TAG = "ScreenCaptureService";
    private static final String CHANNEL_ID = "screen_capture_channel";
    private static final int NOTIFICATION_ID = 1001;
    
    private ScreenCaptureManager screenCaptureManager;
    private final IBinder binder = new ScreenCaptureBinder();
    
    /**
     * Binder类，用于Activity与Service通信
     */
    public class ScreenCaptureBinder extends Binder {
        public ScreenCaptureService getService() {
            return ScreenCaptureService.this;
        }
    }
    
    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "🚀 ScreenCaptureService创建");
        
        // 创建通知渠道
        createNotificationChannel();
        
        // 初始化ScreenCaptureManager
        screenCaptureManager = new ScreenCaptureManager(this);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "🎬 启动前台服务用于屏幕捕获");
        
        try {
            // 使用Android 14+兼容的前台服务启动方式
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+
                // Android 14+ 需要明确指定服务类型
                startForeground(NOTIFICATION_ID, createNotification(), 
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
            } else {
                // 兼容低版本Android
                startForeground(NOTIFICATION_ID, createNotification());
            }
            Log.i(TAG, "✅ 前台服务启动成功");
        } catch (SecurityException e) {
            Log.e(TAG, "❌ 前台服务启动失败 - 权限错误: " + e.getMessage(), e);
            // 尝试不指定类型启动（回退方案）
            try {
                startForeground(NOTIFICATION_ID, createNotification());
                Log.i(TAG, "⚠️ 使用回退方案启动前台服务");
            } catch (Exception fallbackException) {
                Log.e(TAG, "❌ 回退方案也失败: " + fallbackException.getMessage(), fallbackException);
                stopSelf();
                return START_NOT_STICKY;
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 前台服务启动异常: " + e.getMessage(), e);
            stopSelf();
            return START_NOT_STICKY;
        }
        
        return START_STICKY; // 服务被杀死后自动重启
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
    
    @Override
    public void onDestroy() {
        Log.i(TAG, "🛑 ScreenCaptureService销毁");
        
        // 释放资源
        if (screenCaptureManager != null) {
            screenCaptureManager.release();
            screenCaptureManager = null;
        }
        
        super.onDestroy();
    }
    
    /**
     * 获取ScreenCaptureManager实例
     */
    public ScreenCaptureManager getScreenCaptureManager() {
        return screenCaptureManager;
    }
    
    /**
     * 创建通知渠道（Android 8.0+）
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "屏幕捕获服务",
                NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("用于屏幕捕获功能的前台服务");
            channel.setSound(null, null); // 不播放声音
            channel.enableVibration(false); // 不震动
            
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
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("屏幕捕获服务")
            .setContentText("正在进行屏幕捕获...")
            .setSmallIcon(R.mipmap.ic_launcher) // 使用应用图标
            .setContentIntent(pendingIntent)
            .setOngoing(true) // 不可滑动删除
            .setSilent(true) // 静默通知
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build();
    }
    
    /**
     * 更新通知内容
     */
    public void updateNotification(String content) {
        try {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("屏幕捕获服务")
                    .setContentText(content)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setOngoing(true)
                    .setSilent(true)
                    .build();
                    
                manager.notify(NOTIFICATION_ID, notification);
            }
        } catch (Exception e) {
            Log.e(TAG, "更新通知失败", e);
        }
    }
}
