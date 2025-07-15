package com.example.omnicontrol.managers;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import com.example.omnicontrol.services.RemoteAccessibilityService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 远程控制管理器
 * 负责处理远程输入、文件操作等控制功能
 */
public class RemoteControlManager {
    private static final String TAG = "RemoteControlManager";
    
    private Context context;
    private ExecutorService executorService;
    private Handler mainHandler;
    
    // 远程控制状态
    private boolean remoteInputEnabled = false;
    private boolean fileAccessEnabled = false;
    
    // 回调接口
    public interface RemoteControlCallback {
        void onRemoteInputStateChanged(boolean enabled);
        void onFileOperationResult(boolean success, String message);
        void onError(String error);
    }
    
    private RemoteControlCallback remoteControlCallback;
    
    public RemoteControlManager(Context context) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 设置回调监听器
     */
    public void setRemoteControlCallback(RemoteControlCallback callback) {
        this.remoteControlCallback = callback;
    }
    
    /**
     * 启用远程输入
     */
    public void enableRemoteInput() {
        if (remoteInputEnabled) {
            Log.w(TAG, "Remote input already enabled");
            return;
        }
        
        // 检查无障碍服务是否启用
        if (!isAccessibilityServiceEnabled()) {
            Log.w(TAG, "Accessibility service not enabled");
            if (remoteControlCallback != null) {
                remoteControlCallback.onError("请先启用无障碍服务以支持远程输入");
            }
            return;
        }
        
        try {
            remoteInputEnabled = true;
            Log.i(TAG, "Remote input enabled successfully");
            
            if (remoteControlCallback != null) {
                remoteControlCallback.onRemoteInputStateChanged(true);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable remote input", e);
            if (remoteControlCallback != null) {
                remoteControlCallback.onError("启用远程输入失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 禁用远程输入
     */
    public void disableRemoteInput() {
        if (!remoteInputEnabled) {
            Log.w(TAG, "Remote input not enabled");
            return;
        }
        
        try {
            remoteInputEnabled = false;
            Log.i(TAG, "Remote input disabled successfully");
            
            if (remoteControlCallback != null) {
                remoteControlCallback.onRemoteInputStateChanged(false);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error disabling remote input", e);
        }
    }
    
    /**
     * 检查无障碍服务是否启用
     */
    private boolean isAccessibilityServiceEnabled() {
        try {
            String serviceName = context.getPackageName() + "/" + RemoteAccessibilityService.class.getName();
            String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            
            return enabledServices != null && enabledServices.contains(serviceName);
        } catch (Exception e) {
            Log.e(TAG, "Error checking accessibility service", e);
            return false;
        }
    }
    
    /**
     * 打开无障碍设置页面
     */
    public void openAccessibilitySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to open accessibility settings", e);
            if (remoteControlCallback != null) {
                remoteControlCallback.onError("无法打开无障碍设置页面");
            }
        }
    }
    
    /**
     * 启用文件访问
     */
    public void enableFileAccess() {
        fileAccessEnabled = true;
        Log.i(TAG, "File access enabled");
    }
    
    /**
     * 禁用文件访问
     */
    public void disableFileAccess() {
        fileAccessEnabled = false;
        Log.i(TAG, "File access disabled");
    }
    
    /**
     * 执行点击操作
     */
    public void performClick(int x, int y) {
        if (!remoteInputEnabled) {
            Log.w(TAG, "Remote input not enabled");
            return;
        }
        
        executorService.execute(() -> {
            try {
                // 通过无障碍服务执行点击
                RemoteAccessibilityService service = RemoteAccessibilityService.getInstance();
                if (service != null) {
                    service.performClick(x, y);
                    Log.d(TAG, String.format("Click performed at (%d, %d)", x, y));
                } else {
                    Log.e(TAG, "Accessibility service not available");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error performing click", e);
            }
        });
    }
    
    /**
     * 执行滑动操作
     */
    public void performSwipe(int startX, int startY, int endX, int endY, int duration) {
        if (!remoteInputEnabled) {
            Log.w(TAG, "Remote input not enabled");
            return;
        }
        
        executorService.execute(() -> {
            try {
                RemoteAccessibilityService service = RemoteAccessibilityService.getInstance();
                if (service != null) {
                    service.performSwipe(startX, startY, endX, endY, duration);
                    Log.d(TAG, String.format("Swipe performed from (%d, %d) to (%d, %d)", 
                        startX, startY, endX, endY));
                } else {
                    Log.e(TAG, "Accessibility service not available");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error performing swipe", e);
            }
        });
    }
    
    /**
     * 输入文本
     */
    public void inputText(String text) {
        if (!remoteInputEnabled) {
            Log.w(TAG, "Remote input not enabled");
            return;
        }
        
        executorService.execute(() -> {
            try {
                RemoteAccessibilityService service = RemoteAccessibilityService.getInstance();
                if (service != null) {
                    service.inputText(text);
                    Log.d(TAG, "Text input: " + text);
                } else {
                    Log.e(TAG, "Accessibility service not available");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error inputting text", e);
            }
        });
    }
    
    /**
     * 按键操作
     */
    public void performKeyPress(int keyCode) {
        if (!remoteInputEnabled) {
            Log.w(TAG, "Remote input not enabled");
            return;
        }
        
        executorService.execute(() -> {
            try {
                RemoteAccessibilityService service = RemoteAccessibilityService.getInstance();
                if (service != null) {
                    service.performKeyPress(keyCode);
                    Log.d(TAG, "Key press: " + keyCode);
                } else {
                    Log.e(TAG, "Accessibility service not available");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error performing key press", e);
            }
        });
    }
    
    /**
     * 读取文件
     */
    public void readFile(String filePath) {
        if (!fileAccessEnabled) {
            Log.w(TAG, "File access not enabled");
            if (remoteControlCallback != null) {
                remoteControlCallback.onFileOperationResult(false, "文件访问未启用");
            }
            return;
        }
        
        executorService.execute(() -> {
            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    mainHandler.post(() -> {
                        if (remoteControlCallback != null) {
                            remoteControlCallback.onFileOperationResult(false, "文件不存在: " + filePath);
                        }
                    });
                    return;
                }
                
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                StringBuilder content = new StringBuilder();
                int bytesRead;
                
                while ((bytesRead = fis.read(buffer)) != -1) {
                    content.append(new String(buffer, 0, bytesRead));
                }
                fis.close();
                
                final String fileContent = content.toString();
                mainHandler.post(() -> {
                    if (remoteControlCallback != null) {
                        remoteControlCallback.onFileOperationResult(true, "文件读取成功");
                    }
                });
                
                Log.d(TAG, "File read successfully: " + filePath);
                
            } catch (IOException e) {
                Log.e(TAG, "Error reading file: " + filePath, e);
                mainHandler.post(() -> {
                    if (remoteControlCallback != null) {
                        remoteControlCallback.onFileOperationResult(false, "文件读取失败: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * 写入文件
     */
    public void writeFile(String filePath, byte[] data) {
        if (!fileAccessEnabled) {
            Log.w(TAG, "File access not enabled");
            if (remoteControlCallback != null) {
                remoteControlCallback.onFileOperationResult(false, "文件访问未启用");
            }
            return;
        }
        
        executorService.execute(() -> {
            try {
                File file = new File(filePath);
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.close();
                
                mainHandler.post(() -> {
                    if (remoteControlCallback != null) {
                        remoteControlCallback.onFileOperationResult(true, "文件写入成功");
                    }
                });
                
                Log.d(TAG, "File written successfully: " + filePath);
                
            } catch (IOException e) {
                Log.e(TAG, "Error writing file: " + filePath, e);
                mainHandler.post(() -> {
                    if (remoteControlCallback != null) {
                        remoteControlCallback.onFileOperationResult(false, "文件写入失败: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * 获取远程输入状态
     */
    public boolean isRemoteInputEnabled() {
        return remoteInputEnabled;
    }
    
    /**
     * 获取文件访问状态
     */
    public boolean isFileAccessEnabled() {
        return fileAccessEnabled;
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        disableRemoteInput();
        disableFileAccess();
        Log.i(TAG, "Remote control disconnected");
    }
    
    /**
     * 释放资源
     */
    public void release() {
        disconnect();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
