package com.example.omnicontrol.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

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
    
    // WebSocket管理器
    private WebSocketManager webSocketManager;
    
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
        
        // 初始化WebSocket管理器
        this.webSocketManager = new WebSocketManager(this.context);
        
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
        if (response.isSuccess()) {
            try {
                Permissions permissions = Permissions.fromJson(response.getPermissions());
                currentPermissions = permissions;
                cachePermissions(permissions);
                
                Log.d(TAG, "权限获取成功: " + response.getPermissions());
                
                if (listener != null) {
                    listener.onPermissionsLoaded(permissions);
                }
            } catch (Exception e) {
                String error = "权限数据解析失败: " + e.getMessage();
                Log.e(TAG, error);
                if (listener != null) {
                    listener.onPermissionError(error);
                }
            }
        } else {
            String error = "获取权限失败: " + response.getMessage();
            Log.e(TAG, error);
            if (listener != null) {
                listener.onPermissionError(error);
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
                
                // 断开WebSocket连接
                if (webSocketManager != null) {
                    webSocketManager.disconnect();
                }
                
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
     * 更新单个权限（带防抖）
     */
    public void updatePermission(String phone, String permissionType, boolean enabled) {
        if (currentPermissions == null) {
            Log.e(TAG, "当前权限状态为空，无法更新");
            return;
        }
        
        // 验证权限依赖关系
        if (!validatePermissionDependency(permissionType, enabled)) {
            return; // 依赖验证失败，不允许更新
        }
        
        Log.d(TAG, String.format("正在更新权限: %s = %s", permissionType, enabled));
        
        // 更新当前权限状态
        switch (permissionType) {
            case "microphone":
                currentPermissions.setMicrophone(enabled ? 1 : 0);
                handleMicrophonePermission(enabled);
                break;
            case "camera":
                currentPermissions.setCamera(enabled ? 1 : 0);
                handleCameraPermission(enabled);
                break;
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
        }
        
        // 缓存权限到本地
        cachePermissions(currentPermissions);
        
        // 处理WebSocket连接状态
        handleWebSocketConnection();
        
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
     * 上传权限到服务器
     */
    private void uploadPermissionsToServer(String phone, Permissions permissions) {
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
     * 处理WebSocket连接状态
     */
    private void handleWebSocketConnection() {
        if (currentPermissions.getScreen() == 1) {
            // 屏幕权限开启时建立WebSocket连接
            if (webSocketManager != null && !webSocketManager.isConnected()) {
                UserManager userManager = new UserManager(context);
                String userPhone = userManager.getCurrentUsername();
                String userId = userManager.getSuperID();
                if (userPhone != null && userId != null) {
                    
                    // 设置WebSocket连接状态监听器
                    webSocketManager.setConnectionStateListener(new WebSocketManager.ConnectionStateListener() {
                        @Override
                        public void onConnectionStateChanged(int state) {
                            if (state == RDTDefine.ConnectionState.CONNECTED) {
                                Log.i(TAG, "🌐 WebSocket连接成功，开始启用权限相关功能");
                                // WebSocket连接成功后，重新检查并启用已开启的权限功能
                                checkAndEnableActivePermissions();
                            }
                        }
                        
                        @Override
                        public void onScreenDataSent(long frameNumber, int dataSize) {}
                        
                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "WebSocket连接错误: " + error);
                        }
                    });
                    
                    webSocketManager.connect(userPhone, userId);
                    Log.i(TAG, "🌐 屏幕权限开启，建立WebSocket连接");
                    
                    // 将WebSocket管理器传递给音频和摄像头管理器
                    if (audioCaptureManager != null) {
                        audioCaptureManager.setWebSocketManager(webSocketManager);
                    }
                    if (cameraController != null) {
                        cameraController.setWebSocketManager(webSocketManager);
                    }
                }
            } else if (webSocketManager != null && webSocketManager.isConnected()) {
                // WebSocket已连接，直接启用权限功能
                checkAndEnableActivePermissions();
            }
        } else {
            // 屏幕权限关闭时断开WebSocket连接
            if (webSocketManager != null && webSocketManager.isConnected()) {
                webSocketManager.disconnect();
                Log.i(TAG, "🔌 屏幕权限关闭，断开WebSocket连接");
            }
        }
    }
    
    /**
     * 检查并启用当前已开启的权限功能
     */
    private void checkAndEnableActivePermissions() {
        if (webSocketManager == null || !webSocketManager.isConnected()) {
            Log.w(TAG, "WebSocket未连接，无法启用权限功能");
            return;
        }
        
        // 检查麦克风权限
        if (currentPermissions.getMicrophone() == 1) {
            handleMicrophonePermission(true);
            Log.i(TAG, "🎤 WebSocket连接后重新启用麦克风权限");
        }
        
        // 检查摄像头权限
        if (currentPermissions.getCamera() == 1) {
            handleCameraPermission(true);
            Log.i(TAG, "📷 WebSocket连接后重新启用摄像头权限");
        }
    }
    
    /**
     * 处理麦克风权限变化
     */
    private void handleMicrophonePermission(boolean enabled) {
        if (audioCaptureManager != null) {
            if (enabled && webSocketManager != null && webSocketManager.isConnected()) {
                audioCaptureManager.enableWebSocketPush();
                audioCaptureManager.startRecording();
                Log.i(TAG, "🎤 麦克风权限开启，启动音频采集和WebSocket传输");
            } else {
                audioCaptureManager.stopRecording();
                audioCaptureManager.disableWebSocketPush();
                Log.i(TAG, "🔇 麦克风权限关闭，停止音频采集");
            }
        }
    }
    
    /**
     * 处理摄像头权限变化
     */
    private void handleCameraPermission(boolean enabled) {
        if (cameraController != null) {
            if (enabled && webSocketManager != null && webSocketManager.isConnected()) {
                cameraController.startCamera();
                cameraController.enableWebSocketPush();
                Log.i(TAG, "📷 摄像头权限开启，启动摄像头采集和WebSocket传输");
            } else {
                cameraController.disableWebSocketPush();
                cameraController.stopCamera();
                Log.i(TAG, "📷 摄像头权限关闭，停止摄像头采集");
            }
        }
    }
    
    /**
     * 获取WebSocket管理器
     */
    public WebSocketManager getWebSocketManager() {
        return webSocketManager;
    }
    
    /**
     * 处理屏幕捕获权限变化
     */
    private void handleScreenCapturePermission(boolean enabled) {
        try {
            if (enabled) {
                Log.i(TAG, "🎬 屏幕权限已开启，屏幕捕获已由UI层处理授权启动");
                // 注意：实际的startCapture已由HomeFragment在获得用户授权后调用
                // 这里只是为了记录日志和确保地址缓存更新
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
}
