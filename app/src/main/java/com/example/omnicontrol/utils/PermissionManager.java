package com.example.omnicontrol.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.omnicontrol.fragments.HomeFragment;
import com.example.omnicontrol.managers.ScreenCaptureManager;
import com.example.omnicontrol.managers.AudioCaptureManager;
import com.example.omnicontrol.managers.CameraController;
import com.example.omnicontrol.models.Permissions;
import com.example.omnicontrol.models.PermissionsRequest;
import com.example.omnicontrol.models.PermissionsResponse;
import com.example.omnicontrol.models.SetPermissionsRequest;
import com.example.omnicontrol.network.NetworkService;
import com.example.omnicontrol.services.RemoteControlService;
import com.example.omnicontrol.utils.RDTProtocol;
import com.example.omnicontrol.utils.WebSocketManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 权限管理器 - 全局权限状态管理
 */
public class PermissionManager {
    private static final String TAG = "PermissionManager";
    private static final String PREFS_NAME = "permissions_prefs";
    private static final String KEY_PERMISSIONS = "user_permissions";
    
    private static PermissionManager instance;
    private Context context;
    private SharedPreferences sharedPreferences;
    private Permissions currentPermissions;
    private Handler debounceHandler;
    private Runnable debounceRunnable;
    private static final long DEBOUNCE_DELAY = 500; // 500ms 防抖延迟
    
    // 屏幕捕获管理器
    private ScreenCaptureManager screenCaptureManager;
    
    // 音频和摄像头管理器
    private AudioCaptureManager audioCaptureManager;
    private CameraController cameraController;
    
    // 权限变化监听器
    public interface PermissionChangeListener {
        void onPermissionsLoaded(Permissions permissions);
        void onPermissionsUpdated(Permissions permissions);
        void onPermissionError(String error);
    }
    
    private PermissionChangeListener listener;
    
    private PermissionManager(Context context) {
        this.context = context.getApplicationContext();
        this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.debounceHandler = new Handler(Looper.getMainLooper());
        
        // 初始化屏幕捕获管理器
        this.screenCaptureManager = new ScreenCaptureManager(this.context);
        
        // 初始化音频和摄像头管理器
        this.audioCaptureManager = new AudioCaptureManager(this.context);
        this.cameraController = new CameraController(this.context);
        
        loadCachedPermissions();
        
        // 确保RemoteControlService正在运行
        startRemoteControlService();
    }
    
    public static synchronized PermissionManager getInstance(Context context) {
        if (instance == null) {
            instance = new PermissionManager(context);
        }
        return instance;
    }
    
    /**
     * 设置权限变化监听器
     */
    public void setPermissionChangeListener(PermissionChangeListener listener) {
        this.listener = listener;
    }
    
    /**
     * 从缓存加载权限
     */
    private void loadCachedPermissions() {
        String permissionsJson = sharedPreferences.getString(KEY_PERMISSIONS, null);
        if (permissionsJson != null) {
            currentPermissions = Permissions.fromJson(permissionsJson);
            Log.d(TAG, "已加载缓存权限: " + permissionsJson);
            
            // 验证系统权限状态，确保显示正确
            validateAndUpdateSystemPermissions();
        } else {
            // 默认权限状态
            currentPermissions = new Permissions(0, 0, 0, 0, 0);
        }
    }
    
    /**
     * 缓存权限到本地
     */
    private void cachePermissions(Permissions permissions) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_PERMISSIONS, permissions.toJson());
        editor.apply();
        Log.d(TAG, "已缓存权限: " + permissions.toJson());
    }
    
    /**
     * 获取当前权限状态
     */
    public Permissions getCurrentPermissions() {
        return currentPermissions;
    }
    
    /**
     * 从服务器获取权限
     */
    public void fetchPermissions(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            Log.e(TAG, "手机号为空，无法更新权限");
            return;
        }
        
        Log.d(TAG, "正在获取权限，手机号: " + phone);
        
        PermissionsRequest request = new PermissionsRequest(phone);
        Call<PermissionsResponse> call = NetworkService.getInstance().getApiService().getPermissions(request);
        
        call.enqueue(new Callback<PermissionsResponse>() {
            @Override
            public void onResponse(Call<PermissionsResponse> call, Response<PermissionsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PermissionsResponse permissionsResponse = response.body();
                    handlePermissionsResponse(permissionsResponse);
                } else {
                    String error = "获取权限失败：网络错误";
                    Log.e(TAG, error);
                    if (listener != null) {
                        listener.onPermissionError(error);
                    }
                }
            }
            
            @Override
            public void onFailure(Call<PermissionsResponse> call, Throwable t) {
                String error = "获取权限失败：" + t.getMessage();
                Log.e(TAG, error);
                if (listener != null) {
                    listener.onPermissionError(error);
                }
            }
        });
    }
    
    /**
     * 处理权限响应
     */
    private void handlePermissionsResponse(PermissionsResponse response) {
        if (response != null && response.isSuccess() && response.getPermissions() != null) {
            // 解析JSON字符串为权限对象
            currentPermissions = Permissions.fromJson(response.getPermissions());
            cachePermissions(currentPermissions);
            
            Log.i(TAG, "✅ 权限更新成功: " + response.getMessage());
            Log.d(TAG, "当前权限状态: " + currentPermissions.toJson());
            
            // 在处理权限前先验证系统权限状态
            validateAndUpdateSystemPermissions();
            
            // 处理各项权限（使用新的统一处理方式）
            handleScreenCapturePermission(currentPermissions.getScreen() == 1);
            
            // 麦克风和摄像头使用新的统一处理逻辑
            if (currentPermissions.getMicrophone() == 1) {
                // 启动时检查系统权限并启动功能
                if (checkRuntimePermission(android.Manifest.permission.RECORD_AUDIO)) {
                    startFeature("microphone");
                }
            } else {
                stopFeature("microphone");
            }
            
            if (currentPermissions.getCamera() == 1) {
                // 启动时检查系统权限并启动功能
                if (checkRuntimePermission(android.Manifest.permission.CAMERA)) {
                    startFeature("camera");
                }
            } else {
                stopFeature("camera");
            }
            
            // 通知监听器
            if (listener != null) {
                listener.onPermissionsUpdated(currentPermissions);
            }
            
        } else {
            String errorMsg = "权限获取失败: " + (response != null ? response.getMessage() : "未知错误");
            Log.e(TAG, errorMsg);
            if (listener != null) {
                listener.onPermissionError(errorMsg);
            }
        }
    }
    
    /**
     * 验证权限依赖关系
     * @param permissionType 权限类型
     * @param enabled 是否启用
     * @return 是否允许该权限变更
     */
    private boolean validatePermissionDependency(String permissionType, boolean enabled) {
        // 如果要启用非屏幕权限，必须先启用屏幕权限
        if (enabled && !"screen".equals(permissionType)) {
            if (currentPermissions.getScreen() == 0) {
                Log.w(TAG, String.format("❌ 权限依赖检查失败：%s权限需要先启用屏幕权限", permissionType));
                if (listener != null) {
                    listener.onPermissionError(String.format("%s权限需要先启用屏幕权限", getPermissionDisplayName(permissionType)));
                }
                return false;
            }
        }
        
        // 如果要关闭屏幕权限，需要先关闭其他依赖权限
        if (!enabled && "screen".equals(permissionType)) {
            if (currentPermissions.getMicrophone() == 1 || 
                currentPermissions.getCamera() == 1 || 
                currentPermissions.getRemoteInput() == 1 || 
                currentPermissions.getFileAccess() == 1) {
                
                Log.w(TAG, "⚠️ 关闭屏幕权限前自动关闭依赖权限");
                
                // 自动关闭其他权限
                Permissions newPermissions = new Permissions(
                    0, // camera
                    0, // file_access  
                    0, // microphone
                    0, // remote_input
                    0  // screen
                );
                
                currentPermissions = newPermissions;
                cachePermissions(newPermissions);
                
                if (listener != null) {
                    listener.onPermissionsUpdated(newPermissions);
                }
            }
        }
        
        return true;
    }
    
    /**
     * 获取权限显示名称
     */
    private String getPermissionDisplayName(String permissionType) {
        switch (permissionType) {
            case "microphone": return "麦克风";
            case "camera": return "摄像头";
            case "remote_input": return "远程控制";
            case "file_access": return "文件访问";
            case "screen": return "屏幕共享";
            default: return permissionType;
        }
    }
    
    /**
     * 更新单个权限 - 简化版本（移除防抖复杂逻辑）
     * 由UI层调用，统一处理所有权限类型
     */
    public void updatePermission(String phone, String permissionType, boolean enabled) {
        if (currentPermissions == null) {
            Log.e(TAG, "当前权限状态为空，无法更新");
            return;
        }
        
        Log.d(TAG, String.format("🔄 收到权限更新请求: %s = %s", permissionType, enabled));
        
        // 对于麦克风和摄像头，使用新的统一处理逻辑
        if ("microphone".equals(permissionType) || "camera".equals(permissionType)) {
            onPermissionCheckboxClicked(permissionType, enabled);
            return;
        }
        
        // 其他权限类型（屏幕共享、远程控制等）保持原有逻辑
        switch (permissionType) {
            case "remote_input":
                currentPermissions.setRemoteInput(enabled ? 1 : 0);
                break;
            case "file_access":
                currentPermissions.setFileAccess(enabled ? 1 : 0);
                break;
            case "screen":
                currentPermissions.setScreen(enabled ? 1 : 0);
                handleScreenCapturePermission(enabled);
                break;
            default:
                Log.w(TAG, "未知的权限类型: " + permissionType);
                return;
        }
        
        // 缓存权限到本地
        cachePermissions(currentPermissions);

        checkAndEnableActivePermissions();

        // 通知UI更新
        if (listener != null) {
            listener.onPermissionsUpdated(currentPermissions);
        }
        
        // 取消之前的防抖任务
        if (debounceRunnable != null) {
            debounceHandler.removeCallbacks(debounceRunnable);
        }
        
        // 创建新的防抖任务
        debounceRunnable = () -> {
            Log.d(TAG, "防抖延迟结束，开始上传权限到服务器");
            uploadPermissionsToServer(phone, currentPermissions);
        };
        
        // 延迟执行
        debounceHandler.postDelayed(debounceRunnable, DEBOUNCE_DELAY);
        
        Log.d(TAG, "权限已更新: " + permissionType + " = " + enabled + ", 将在 " + DEBOUNCE_DELAY + "ms 后上传到服务器");
    }
    
    /**
     * 设置权限（上传到服务器）
     * 这是 uploadPermissionsToServer 的别名方法，用于兼容现有调用
     */
    public void setPermissions(String phone, Permissions permissions) {
        uploadPermissionsToServer(phone, permissions);
    }
    
    /**
     * 上传权限到服务器
     */
    public void uploadPermissionsToServer(String phone, Permissions permissions) {
        Log.d(TAG, "上传权限到服务器，手机号: " + phone + ", 权限: " + permissions.toJson());
        
        SetPermissionsRequest request = new SetPermissionsRequest(phone, permissions.toJson());
        Call<PermissionsResponse> call = NetworkService.getInstance().getApiService().setPermissions(request);
        
        call.enqueue(new Callback<PermissionsResponse>() {
            @Override
            public void onResponse(Call<PermissionsResponse> call, Response<PermissionsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PermissionsResponse permissionsResponse = response.body();
                    if (permissionsResponse.isSuccess()) {
                        Log.d(TAG, "权限上传成功");
                        // 服务器返回的权限状态可能与本地不同，以服务器为准
                        try {
                            Permissions serverPermissions = Permissions.fromJson(permissionsResponse.getPermissions());
                            currentPermissions = serverPermissions;
                            cachePermissions(serverPermissions);
                            
                            if (listener != null) {
                                listener.onPermissionsUpdated(serverPermissions);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "服务器权限数据解析失败: " + e.getMessage());
                        }
                    } else {
                        String error = "权限更新失败: " + permissionsResponse.getMessage();
                        Log.e(TAG, error);
                        if (listener != null) {
                            listener.onPermissionError(error);
                        }
                    }
                } else {
                    String error = "权限更新失败：网络错误";
                    Log.e(TAG, error);
                    if (listener != null) {
                        listener.onPermissionError(error);
                    }
                }
            }
            
            @Override
            public void onFailure(Call<PermissionsResponse> call, Throwable t) {
                String error = "权限更新失败：" + t.getMessage();
                Log.e(TAG, error);
                if (listener != null) {
                    listener.onPermissionError(error);
                }
            }
        });
    }
    
    /**
     * 清除权限缓存
     */
    public void clearPermissions() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_PERMISSIONS);
        editor.apply();
        currentPermissions = new Permissions(0, 0, 0, 0, 0);
        Log.d(TAG, "权限缓存已清除");
    }
    
    /**
     * 检查并启用当前已开启的权限功能 - 简化版本
     */
    private void checkAndEnableActivePermissions() {
        Log.i(TAG, "🔄 检查并启用当前已开启的权限功能");

        if (currentPermissions.getScreen() == 1) {
            handleScreenCapturePermission(true);
        }
        
        // 检查麦克风权限（使用新的统一逻辑）
        if (currentPermissions.getMicrophone() == 1) {
            if (checkRuntimePermission(android.Manifest.permission.RECORD_AUDIO)) {
                startFeature("microphone");
                Log.i(TAG, "🎤 重新启用麦克风功能");
            } else {
                Log.w(TAG, "⚠️ 麦克风系统权限未授予，无法重新启用");
            }
        }
        
        // 检查摄像头权限（使用新的统一逻辑）
        if (currentPermissions.getCamera() == 1) {
            if (checkRuntimePermission(android.Manifest.permission.CAMERA)) {
                startFeature("camera");
                Log.i(TAG, "📷 重新启用摄像头功能");
            } else {
                Log.w(TAG, "⚠️ 摄像头系统权限未授予，无法重新启用");
            }
        }
    }
    
    /**
     * 统一处理权限点击事件 - 简化版本
     * @param permissionType "microphone" 或 "camera"
     * @param enabled 用户是否想要开启
     */
    public void onPermissionCheckboxClicked(String permissionType, boolean enabled) {
        Log.i(TAG, String.format("%s %s权限处理: %s", getPermissionEmoji(permissionType), 
            getPermissionDisplayName(permissionType), enabled ? "✅用户开启" : "❌用户关闭"));
        
        if (enabled) {
            // 用户想要开启功能
            handlePermissionEnable(permissionType);
        } else {
            // 用户想要关闭功能
            handlePermissionDisable(permissionType);
        }
    }
    
    /**
     * 处理权限开启请求
     */
    private void handlePermissionEnable(String permissionType) {
        String androidPermission = getAndroidPermission(permissionType);
        String displayName = getPermissionDisplayName(permissionType);
        
        // 检查系统权限
        if (checkRuntimePermission(androidPermission)) {
            // 有权限：直接启动功能 + 更新UI为开启
            Log.i(TAG, String.format("✅ %s系统权限已授予，直接启动功能", displayName));
            startFeature(permissionType);
            updatePermissionState(permissionType, true);
        } else {
            // 无权限：弹出系统对话框，UI保持关闭状态
            Log.w(TAG, String.format("❌ %s系统权限未授予，请求用户授权", displayName));
            requestRuntimePermission(androidPermission, permissionType);
            // 注意：UI状态不变，等待权限结果
        }
    }
    
    /**
     * 处理权限关闭请求
     */
    private void handlePermissionDisable(String permissionType) {
        Log.i(TAG, String.format("🔇 用户关闭%s权限，停止功能", getPermissionDisplayName(permissionType)));
        stopFeature(permissionType);
        updatePermissionState(permissionType, false);
    }
    
    /**
     * 启动具体功能
     */
    private void startFeature(String permissionType) {
        try {
            if ("microphone".equals(permissionType) && audioCaptureManager != null) {
                audioCaptureManager.enableWebSocketPush();
                audioCaptureManager.startRecording();
                Log.i(TAG, "🚀 麦克风功能已启动");
            } else if ("camera".equals(permissionType) && cameraController != null) {
                cameraController.startCamera();
                cameraController.enableWebSocketPush();
                Log.i(TAG, "🚀 摄像头功能已启动");
            }
        } catch (Exception e) {
            Log.e(TAG, String.format("❌ 启动%s功能失败", getPermissionDisplayName(permissionType)), e);
        }
    }
    
    /**
     * 停止具体功能
     */
    private void stopFeature(String permissionType) {
        try {
            if ("microphone".equals(permissionType) && audioCaptureManager != null) {
                audioCaptureManager.stopRecording();
                audioCaptureManager.disableWebSocketPush();
                Log.i(TAG, "🔇 麦克风功能已停止");
            } else if ("camera".equals(permissionType) && cameraController != null) {
                cameraController.disableWebSocketPush();
                cameraController.stopCamera();
                Log.i(TAG, "📷 摄像头功能已停止");
            }
        } catch (Exception e) {
            Log.e(TAG, String.format("❌ 停止%s功能失败", getPermissionDisplayName(permissionType)), e);
        }
    }
    
    /**
     * 权限授权结果处理 - 简化版本
     * 由MainActivity.onRequestPermissionsResult()调用
     */
    public void onPermissionResult(String permissionType, boolean granted) {
        String displayName = getPermissionDisplayName(permissionType);
        Log.i(TAG, String.format("%s %s权限授权结果: %s", getPermissionEmoji(permissionType), 
            displayName, granted ? "✅用户同意" : "❌用户拒绝"));
        
        if (granted) {
            // 用户同意：启动功能 + 更新UI为开启
            startFeature(permissionType);
            updatePermissionState(permissionType, true);
        } else {
            // 用户拒绝：停止，UI保持关闭
            Log.w(TAG, String.format("❌ 用户拒绝%s权限，功能无法启动", displayName));
            updatePermissionState(permissionType, false);
            
            // 通知用户权限被拒绝
            if (listener != null) {
                listener.onPermissionError(String.format("%s权限被拒绝，无法使用该功能", displayName));
            }
        }
    }
    
    /**
     * 更新权限状态并通知UI
     */
    private void updatePermissionState(String permissionType, boolean enabled) {
        try {
            // 获取用户信息
            com.example.omnicontrol.utils.UserManager userManager = 
                new com.example.omnicontrol.utils.UserManager(context);
            String phone = userManager.getCurrentUsername();
            
            if (phone != null && !phone.isEmpty()) {
                // 更新内部权限状态
                if (currentPermissions == null) {
                    currentPermissions = new Permissions(0, 0, 0, 0, 0);
                }
                
                if ("microphone".equals(permissionType)) {
                    currentPermissions.setMicrophone(enabled ? 1 : 0);
                } else if ("camera".equals(permissionType)) {
                    currentPermissions.setCamera(enabled ? 1 : 0);
                }
                
                // 缓存权限
                cachePermissions(currentPermissions);
                
                // 立即上传到服务器（简化版，无debounce）
                uploadPermissionsToServer(phone, currentPermissions);
                
                // 通知UI更新
                if (listener != null) {
                    listener.onPermissionsUpdated(currentPermissions);
                }
                
                Log.i(TAG, String.format("✅ %s权限状态已更新: %s", getPermissionDisplayName(permissionType), 
                    enabled ? "开启" : "关闭"));
            }
        } catch (Exception e) {
            Log.e(TAG, "更新权限状态失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理屏幕捕获权限变化 - 屏幕共享负责确保WebSocket连接
     */
    private void handleScreenCapturePermission(boolean enabled) {
        try {
            if (enabled) {
                Log.i(TAG, "🎬 屏幕权限已开启，屏幕捕获已由UI层处理授权启动");
                
                // 🔧 修复：屏幕共享是第一个启动的功能，负责确保WebSocket连接
                ensureWebSocketConnectionForScreenSharing();
                
                // 注意：实际的startCapture已由HomeFragment在获得用户授权后调用
                // 这里只是为了记录日志和确保地址缓存更新
                HomeFragment homeFragment = HomeFragment.instance();
                homeFragment.requestScreenCapturePermission();
            } else {
                Log.i(TAG, "🛑 屏幕权限已关闭，停止屏幕捕获功能");
                if (screenCaptureManager != null) {
                    screenCaptureManager.stopCapture();
                    Log.i(TAG, "✅ 屏幕捕获已停止");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "屏幕捕获权限处理错误: " + e.getMessage(), e);
        }
    }
    
    /**
     * 为屏幕共享确保WebSocket连接已建立
     * 屏幕共享是第一个必须启动的功能，应该负责建立WebSocket连接
     */
    private void ensureWebSocketConnectionForScreenSharing() {
        try {
            WebSocketManager webSocketManager = WebSocketManager.instance();
            if (webSocketManager != null) {
                if (webSocketManager.isConnected()) {
                    Log.i(TAG, "🌐 WebSocket已连接，屏幕共享和后续功能可以正常传输数据");
                } else {
                    Log.i(TAG, "🌐 WebSocket未连接，屏幕共享启动时建立连接...");
                    
                    // 尝试使用已保存的用户信息建立连接
                    com.example.omnicontrol.utils.UserManager userManager = 
                        new com.example.omnicontrol.utils.UserManager(context);
                    String phone = userManager.getCurrentUsername();
                    String userId = userManager.getSuperID();
                    
                    if (phone != null && !phone.isEmpty() && userId != null && !userId.isEmpty()) {
                        Log.i(TAG, String.format("🔐 使用已保存的用户信息建立WebSocket连接: phone=%s, userId=%s", phone, userId));
                        webSocketManager.sendUserAuthSignal(phone, userId);
                        
                        // 给连接一点时间建立
                        Thread.sleep(1000);
                        
                        if (webSocketManager.isConnected()) {
                            Log.i(TAG, "✅ WebSocket连接建立成功，所有功能的数据传输已准备就绪");
                        } else {
                            Log.w(TAG, "⚠️ WebSocket连接建立失败，数据将无法传输到服务器");
                        }
                    } else {
                        Log.w(TAG, "⚠️ 用户信息不完整，无法建立WebSocket连接");
                        Log.i(TAG, String.format("用户信息: phone=%s, userId=%s", phone, userId));
                    }
                }
            } else {
                Log.e(TAG, "❌ WebSocketManager实例为null");
            }
        } catch (Exception e) {
            Log.e(TAG, "确保屏幕共享WebSocket连接时发生异常: " + e.getMessage(), e);
        }
    }
    
    /**
     * 启动RemoteControlService
     */
    private void startRemoteControlService() {
        try {
            Intent serviceIntent = new Intent(context, RemoteControlService.class);
            serviceIntent.setAction("START_SERVICE");
            context.startForegroundService(serviceIntent);
            Log.d(TAG, "RemoteControlService启动请求已发送");
        } catch (Exception e) {
            Log.e(TAG, "启动RemoteControlService失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 获取屏幕捕获管理器（用于调试）
     */
    public ScreenCaptureManager getScreenCaptureManager() {
        return screenCaptureManager;
    }
    
    /**
     * 获取音频采集管理器
     */
    public AudioCaptureManager getAudioCaptureManager() {
        return audioCaptureManager;
    }
    
    /**
     * 获取摄像头控制器
     */
    public CameraController getCameraController() {
        return cameraController;
    }
    
    // ========================================
    // 辅助方法 - Helper Methods
    // ========================================
    
    /**
     * 获取权限的表情符号
     */
    private String getPermissionEmoji(String permissionType) {
        if ("microphone".equals(permissionType)) {
            return "🎤";
        } else if ("camera".equals(permissionType)) {
            return "📷";
        }
        return "🔒";
    }
    
    /**
     * 获取对应的Android系统权限
     */
    private String getAndroidPermission(String permissionType) {
        if ("microphone".equals(permissionType)) {
            return android.Manifest.permission.RECORD_AUDIO;
        } else if ("camera".equals(permissionType)) {
            return android.Manifest.permission.CAMERA;
        }
        return null;
    }
    
    /**
     * 检查Android运行时权限
     * @param permission 权限名称
     * @return 是否已授予权限
     */
    private boolean checkRuntimePermission(String permission) {
        try {
            int result = androidx.core.content.ContextCompat.checkSelfPermission(context, permission);
            boolean granted = result == android.content.pm.PackageManager.PERMISSION_GRANTED;
            Log.d(TAG, String.format("🔍 权限检查 %s: %s", 
                permission.substring(permission.lastIndexOf('.') + 1), 
                granted ? "✅已授予" : "❌未授予"));
            return granted;
        } catch (Exception e) {
            Log.e(TAG, "权限检查异常: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 验证并更新系统权限状态
     * 确保应用级权限状态与实际系统权限匹配
     */
    private void validateAndUpdateSystemPermissions() {
        if (currentPermissions == null) {
            return;
        }
        
        try {
            Log.i(TAG, "🔍 开始验证系统权限状态...");
            
            boolean needsUpdate = false;
            String phone = new com.example.omnicontrol.utils.UserManager(context).getCurrentUsername();
            
            // 检查麦克风权限
            if (currentPermissions.getMicrophone() == 1) {
                boolean hasSystemPermission = checkRuntimePermission(android.Manifest.permission.RECORD_AUDIO);
                if (!hasSystemPermission) {
                    Log.w(TAG, "❌ 麦克风应用权限已开启但系统权限未授予，自动关闭应用权限");
                    if (phone != null) {
                        updatePermission(phone, "microphone", false);
                        needsUpdate = true;
                    }
                }
            }
            
            // 检查摄像头权限
            if (currentPermissions.getCamera() == 1) {
                boolean hasSystemPermission = checkRuntimePermission(android.Manifest.permission.CAMERA);
                if (!hasSystemPermission) {
                    Log.w(TAG, "❌ 摄像头应用权限已开启但系统权限未授予，自动关闭应用权限");
                    if (phone != null) {
                        updatePermission(phone, "camera", false);
                        needsUpdate = true;
                    }
                }
            }
            
            if (needsUpdate) {
                Log.i(TAG, "✅ 权限状态已同步更新");
            } else {
                Log.i(TAG, "✅ 权限状态验证通过");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "验证系统权限状态失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 请求Android运行时权限（显示系统对话框）
     * @param permission 权限名称
     * @param displayName 权限显示名称
     */
    private void requestRuntimePermission(String permission, String displayName) {
        try {
            Log.i(TAG, String.format("📱 请求Android%s权限对话框...", displayName));
            
            // 获取MainActivity实例用于显示权限对话框
            com.example.omnicontrol.MainActivity mainActivity = 
                com.example.omnicontrol.MainActivity.getInstance();
            
            if (mainActivity != null) {
                // 根据权限类型调用相应的权限请求方法
                if (android.Manifest.permission.RECORD_AUDIO.equals(permission)) {
                    mainActivity.requestMicrophonePermission();
                    Log.i(TAG, "🎤 已调用麦克风权限对话框");
                    
                    // 显示友好的提示信息
                    if (listener != null) {
                        String message = String.format(
                            "正在请求Android系统%s权限\n\n" +
                            "📱 请在弹出的对话框中点击\"允许\"\n\n" +
                            "✅ 授权后请重新切换应用权限开关", 
                            displayName
                        );
                        listener.onPermissionError(message);
                    }
                    
                } else if (android.Manifest.permission.CAMERA.equals(permission)) {
                    mainActivity.requestCameraPermission();
                    Log.i(TAG, "📷 已调用摄像头权限对话框");
                    
                    // 显示友好的提示信息
                    if (listener != null) {
                        String message = String.format(
                            "正在请求Android系统%s权限\n\n" +
                            "📱 请在弹出的对话框中点击\"允许\"\n\n" +
                            "✅ 授权后请重新切换应用权限开关", 
                            displayName
                        );
                        listener.onPermissionError(message);
                    }
                    
                } else {
                    Log.w(TAG, "⚠️ 未知权限类型: " + permission);
                    fallbackToSettingsPage(permission, displayName);
                }
                
            } else {
                Log.w(TAG, "⚠️ MainActivity实例为null，使用备用方案");
                fallbackToSettingsPage(permission, displayName);
            }
            
            Log.i(TAG, String.format("📢 已触发Android%s权限请求", displayName));
            
        } catch (Exception e) {
            Log.e(TAG, "请求运行时权限失败: " + e.getMessage(), e);
            fallbackToSettingsPage(permission, displayName);
        }
    }
    
    /**
     * 备用方案：跳转到设置页面
     */
    private void fallbackToSettingsPage(String permission, String displayName) {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                Log.i(TAG, "🔗 正在跳转到应用设置页面(备用方案)...");
                context.startActivity(intent);
                
                if (listener != null) {
                    String message = String.format(
                        "需要Android系统%s权限\n\n" +
                        "📱 请在设置页面中：\n" +
                        "1. 点击 权限\n" +
                        "2. 开启 %s 权限\n\n" +
                        "✅ 开启后请重新切换应用权限开关", 
                        displayName, displayName
                    );
                    listener.onPermissionError(message);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "备用方案失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 自动关闭应用级权限并提供用户反馈
     * @param permissionType 权限类型
     * @param displayName 权限显示名称
     */
    private void autoDisablePermission(String permissionType, String displayName) {
        try {
            Log.w(TAG, String.format("⚠️ 自动关闭应用%s权限开关", displayName));
            
            // 获取用户信息
            com.example.omnicontrol.utils.UserManager userManager = 
                new com.example.omnicontrol.utils.UserManager(context);
            String phone = userManager.getCurrentUsername();
            
            if (phone != null && !phone.isEmpty()) {
                // 更新应用权限为false
                updatePermission(phone, permissionType, false);
                Log.i(TAG, String.format("✅ 已自动关闭应用%s权限开关", displayName));
                
                // 通知UI更新
                if (listener != null) {
                    // 获取更新后的权限状态并通知
                    fetchPermissions(phone);
                }
                
            } else {
                Log.w(TAG, "无法获取用户信息，跳过权限更新");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "自动关闭权限失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 公开方法：用户授权麦克风权限后自动启用功能
     */
    public void autoEnableMicrophoneAfterPermissionGranted() {
        try {
            Log.i(TAG, "🎤 用户授权麦克风权限，尝试自动启用功能...");
            
            // 获取用户信息
            com.example.omnicontrol.utils.UserManager userManager = 
                new com.example.omnicontrol.utils.UserManager(context);
            String phone = userManager.getCurrentUsername();
            
            if (phone != null && !phone.isEmpty()) {
                // 先尝试启动实际功能，只有成功后才更新UI状态
                boolean functionalityStarted = false;
                
                if (audioCaptureManager != null) {
                    WebSocketManager webSocketManager = WebSocketManager.instance();
                    if (webSocketManager != null && webSocketManager.isConnected()) {
                        try {
                            audioCaptureManager.enableWebSocketPush();
                            audioCaptureManager.startRecording();
                            
                            // 检查是否真的启动成功
                            if (audioCaptureManager.isRecording()) {
                                functionalityStarted = true;
                                Log.i(TAG, "🚀 麦克风功能成功启动！");
                            } else {
                                Log.w(TAG, "⚠️ 麦克风启动失败，不更新UI状态");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ 麦克风启动异常: " + e.getMessage(), e);
                        }
                    } else {
                        Log.w(TAG, "⚠️ WebSocket未连接，无法启动麦克风功能");
                    }
                } else {
                    Log.e(TAG, "❌ AudioCaptureManager为null，无法启动麦克风");
                }
                
                // 只有实际功能启动成功才更新UI状态
                if (functionalityStarted) {
                    if (currentPermissions == null) {
                        currentPermissions = new Permissions(0, 0, 0, 0, 0);
                    }
                    currentPermissions.setMicrophone(1);
                    cachePermissions(currentPermissions);
                    
                    Log.i(TAG, "✅ 麦克风功能启动成功，已更新UI状态为开启");
                    
                    // 上传权限到服务器
                    debounceHandler.postDelayed(() -> {
                        uploadPermissionsToServer(phone, currentPermissions);
                    }, DEBOUNCE_DELAY);
                    
                    // 通知UI更新
                    if (listener != null) {
                        listener.onPermissionsUpdated(currentPermissions);
                    }
                } else {
                    Log.w(TAG, "❌ 麦克风功能启动失败，UI状态保持不变");
                    
                    // 通知用户实际功能启动失败
                    if (listener != null) {
                        listener.onPermissionError("麦克风功能启动失败，请检查WebSocket连接状态");
                    }
                }
                
            } else {
                Log.w(TAG, "无法获取用户信息，跳过自动启用");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "自动启用麦克风功能失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 公开方法：用户授权摄像头权限后自动启用功能
     */
    public void autoEnableCameraAfterPermissionGranted() {
        try {
            Log.i(TAG, "📷 用户授权摄像头权限，尝试自动启用功能...");
            
            // 获取用户信息
            com.example.omnicontrol.utils.UserManager userManager = 
                new com.example.omnicontrol.utils.UserManager(context);
            String phone = userManager.getCurrentUsername();
            
            if (phone != null && !phone.isEmpty()) {
                // 先尝试启动实际功能，只有成功后才更新UI状态
                boolean functionalityStarted = false;
                
                if (cameraController != null) {
                    WebSocketManager webSocketManager = WebSocketManager.instance();
                    if (webSocketManager != null && webSocketManager.isConnected()) {
                        try {
                            cameraController.startCamera();
                            cameraController.enableWebSocketPush();
                            
                            // 检查是否真的启动成功
                            if (cameraController.isCameraOpen()) {
                                functionalityStarted = true;
                                Log.i(TAG, "🚀 摄像头功能成功启动！");
                            } else {
                                Log.w(TAG, "⚠️ 摄像头启动失败，不更新UI状态");
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "❌ 摄像头启动异常: " + e.getMessage(), e);
                        }
                    } else {
                        Log.w(TAG, "⚠️ WebSocket未连接，无法启动摄像头功能");
                    }
                } else {
                    Log.e(TAG, "❌ CameraController为null，无法启动摄像头");
                }
                
                // 只有实际功能启动成功才更新UI状态
                if (functionalityStarted) {
                    if (currentPermissions == null) {
                        currentPermissions = new Permissions(0, 0, 0, 0, 0);
                    }
                    currentPermissions.setCamera(1);
                    cachePermissions(currentPermissions);
                    
                    Log.i(TAG, "✅ 摄像头功能启动成功，已更新UI状态为开启");
                    
                    // 上传权限到服务器
                    debounceHandler.postDelayed(() -> {
                        uploadPermissionsToServer(phone, currentPermissions);
                    }, DEBOUNCE_DELAY);
                    
                    // 通知UI更新
                    if (listener != null) {
                        listener.onPermissionsUpdated(currentPermissions);
                    }
                } else {
                    Log.w(TAG, "❌ 摄像头功能启动失败，UI状态保持不变");
                    
                    // 通知用户实际功能启动失败
                    if (listener != null) {
                        listener.onPermissionError("摄像头功能启动失败，请检查WebSocket连接状态");
                    }
                }
                
            } else {
                Log.w(TAG, "无法获取用户信息，跳过自动启用");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "自动启用摄像头功能失败: " + e.getMessage(), e);
        }
    }
}
