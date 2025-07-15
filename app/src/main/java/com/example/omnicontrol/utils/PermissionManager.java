package com.example.omnicontrol.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.example.omnicontrol.managers.ScreenCaptureManager;
import com.example.omnicontrol.models.Permissions;
import com.example.omnicontrol.models.PermissionsRequest;
import com.example.omnicontrol.models.PermissionsResponse;
import com.example.omnicontrol.models.SetPermissionsRequest;
import com.example.omnicontrol.network.NetworkService;
import com.example.omnicontrol.services.RemoteControlService;

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
            Log.e(TAG, "手机号为空，无法获取权限");
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
     * 更新单个权限（带防抖）
     */
    public void updatePermission(String phone, String permissionType, boolean enabled) {
        if (currentPermissions == null) {
            Log.e(TAG, "当前权限状态为空，无法更新");
            return;
        }
        
        // 更新本地权限状态
        switch (permissionType) {
            case "camera":
                currentPermissions.setCamera(enabled ? 1 : 0);
                break;
            case "file_access":
                currentPermissions.setFileAccess(enabled ? 1 : 0);
                break;
            case "microphone":
                currentPermissions.setMicrophone(enabled ? 1 : 0);
                break;
            case "remote_input":
                currentPermissions.setRemoteInput(enabled ? 1 : 0);
                break;
            case "screen":
                currentPermissions.setScreen(enabled ? 1 : 0);
                // 立即控制屏幕捕获功能
                handleScreenCapturePermission(enabled);
                break;
            default:
                Log.w(TAG, "未知权限类型: " + permissionType);
                return;
        }
        
        // 立即更新缓存
        cachePermissions(currentPermissions);
        
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
}
