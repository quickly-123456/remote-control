package com.example.omnicontrol.fragments;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.omnicontrol.databinding.FragmentHomeBinding;
import com.example.omnicontrol.managers.ScreenCaptureManager;
import com.example.omnicontrol.services.ScreenCaptureService;
import com.example.omnicontrol.utils.SystemInfoManager;
import com.example.omnicontrol.utils.PermissionManager;
import com.example.omnicontrol.utils.UserManager;
import com.example.omnicontrol.models.Permissions;


public class HomeFragment extends Fragment implements PermissionManager.PermissionChangeListener {
    
    private static final String TAG = "HomeFragment";
    
    private FragmentHomeBinding binding;
    private PermissionManager permissionManager;
    private boolean isUpdatingUI = false; // 防止UI更新时触发权限上传
    
    // 屏幕捕获相关
    private ActivityResultLauncher<Intent> screenCapturePermissionLauncher;
    private boolean pendingScreenPermission = false;
    
    // ScreenCaptureService相关
    private ScreenCaptureService screenCaptureService;
    private boolean isServiceBound = false;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Log.i(TAG, "✅ ScreenCaptureService已连接");
            ScreenCaptureService.ScreenCaptureBinder binder = (ScreenCaptureService.ScreenCaptureBinder) service;
            screenCaptureService = binder.getService();
            isServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.w(TAG, "⚠️ ScreenCaptureService连接断开");
            screenCaptureService = null;
            isServiceBound = false;
        }
    };
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化屏幕捕获权限处理器
        initializeScreenCapturePermissionLauncher();
        
        // 初始化系统信息管理器
        SystemInfoManager systemInfoManager = new SystemInfoManager(requireContext());
        
        // 初始化权限管理器
        permissionManager = PermissionManager.getInstance(requireContext());
        permissionManager.setPermissionChangeListener(this);
        
        // 获取并更新系统信息
        updateSystemInfo(systemInfoManager);
        
        // 从缓存加载权限状态
        loadPermissionsFromCache();
        
        // 从服务器获取最新权限状态
        fetchPermissionsFromServer();
        
        // 设置权限开关监听事件
        setupPermissionSwitchListeners();
    }
    
    /**
     * 更新系统信息显示
     */
    private void updateSystemInfo(SystemInfoManager systemInfoManager) {
        // 获取设备信息
        SystemInfoManager.DeviceInfo deviceInfo = systemInfoManager.getDeviceInfo();
        SystemInfoManager.NetworkInfo networkInfo = systemInfoManager.getNetworkInfo();
        SystemInfoManager.BatteryInfo batteryInfo = systemInfoManager.getBatteryInfo();
        
        // 在日志中输出系统信息
        android.util.Log.d("SystemInfo", "Device: " + deviceInfo.brand + " " + deviceInfo.model);
        android.util.Log.d("SystemInfo", "Network: " + (networkInfo.isConnected ? "已连接" : "未连接"));
        android.util.Log.d("SystemInfo", "Battery: " + batteryInfo.level + "% (" + batteryInfo.status + ")");
        android.util.Log.d("SystemInfo", "IP: " + networkInfo.ipAddress);
        
        // 更新UI显示真实的系统信息
        try {
            // 更新连接状态
            binding.tvConnectionStatus.setText(networkInfo.isConnected ? "已连接" : "未连接");
            
            // 更新设备名称
            String deviceName = deviceInfo.brand + "-" + deviceInfo.model;
            binding.tvDeviceName.setText(deviceName);
            
            // 更新电池电量
            String batteryText = "电量: " + batteryInfo.level + "%";
            binding.tvBatteryLevel.setText(batteryText);
            
            // 更新最后在线时间（使用当前时间）
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String currentTime = sdf.format(new Date());
            binding.tvLastOnlineTime.setText(currentTime);
            
            // 更新IP地址
            String ipText = "IP: " + (networkInfo.ipAddress.isEmpty() ? "未知" : networkInfo.ipAddress);
            binding.tvIpAddress.setText(ipText);
            
            // 更新网络类型
            String networkText = "网络: " + networkInfo.networkType;
            binding.tvNetworkType.setText(networkText);
            
        } catch (Exception e) {
            android.util.Log.e("SystemInfo", "更新UI失败: " + e.getMessage());
        }
    }
    
    /**
     * 从缓存加载权限状态
     */
    private void loadPermissionsFromCache() {
        Permissions permissions = permissionManager.getCurrentPermissions();
        if (permissions != null) {
            updateUIWithPermissions(permissions);
        }
    }
    
    /**
     * 从服务器获取权限状态
     */
    private void fetchPermissionsFromServer() {
        UserManager userManager = new UserManager(requireContext());
        String phone = userManager.getCurrentUsername();
        
        if (phone != null && !phone.isEmpty()) {
            permissionManager.fetchPermissions(phone);
        } else {
            Toast.makeText(getContext(), "用户信息不完整，无法获取权限状态", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 设置权限开关监听器
     */
    private void setupPermissionSwitchListeners() {
        UserManager userManager = new UserManager(requireContext());
        String phone = userManager.getCurrentUsername();
        
        if (phone == null || phone.isEmpty()) {
            Toast.makeText(getContext(), "用户信息不完整，权限功能不可用", Toast.LENGTH_SHORT).show();
            return;
        }
        
        binding.switchScreenShare.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingUI) {
                if (isChecked) {
                    // 开启屏幕共享时需要用户授权
                    requestScreenCapturePermission(phone);
                } else {
                    // 关闭屏幕共享时停止捕获并更新权限
                    stopScreenCapture();
                    permissionManager.updatePermission(phone, "screen", false);
                    Toast.makeText(getContext(), "屏幕共享已关闭", Toast.LENGTH_SHORT).show();
                }
            }
        });
        
        binding.switchPageView.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingUI) {
                permissionManager.updatePermission(phone, "microphone", isChecked);
                String status = isChecked ? "已开启" : "已关闭";
                Toast.makeText(getContext(), "麦克风权限" + status, Toast.LENGTH_SHORT).show();
            }
        });
        
        binding.switchCamera.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingUI) {
                permissionManager.updatePermission(phone, "camera", isChecked);
                String status = isChecked ? "已开启" : "已关闭";
                Toast.makeText(getContext(), "摄像头权限" + status, Toast.LENGTH_SHORT).show();
            }
        });
        
        binding.switchRemoteInput.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingUI) {
                permissionManager.updatePermission(phone, "remote_input", isChecked);
                String status = isChecked ? "已开启" : "已关闭";
                Toast.makeText(getContext(), "远程输入权限" + status, Toast.LENGTH_SHORT).show();
            }
        });
        
        binding.switchFileAccess.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isUpdatingUI) {
                permissionManager.updatePermission(phone, "file_access", isChecked);
                String status = isChecked ? "已开启" : "已关闭";
                Toast.makeText(getContext(), "文件访问权限" + status, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 使用权限数据更新UI
     */
    private void updateUIWithPermissions(Permissions permissions) {
        isUpdatingUI = true;
        
        // 更新开关状态（不触发监听器）
        binding.switchScreenShare.setChecked(permissions.getScreen() == 1);
        binding.switchPageView.setChecked(permissions.getMicrophone() == 1);
        binding.switchCamera.setChecked(permissions.getCamera() == 1);
        binding.switchRemoteInput.setChecked(permissions.getRemoteInput() == 1);
        binding.switchFileAccess.setChecked(permissions.getFileAccess() == 1);
        
        isUpdatingUI = false;
    }
    
    // PermissionManager.PermissionChangeListener 接口实现
    @Override
    public void onPermissionsLoaded(Permissions permissions) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                updateUIWithPermissions(permissions);
                Toast.makeText(getContext(), "权限状态已更新", Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    @Override
    public void onPermissionsUpdated(Permissions permissions) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                updateUIWithPermissions(permissions);
            });
        }
    }
    
    @Override
    public void onPermissionError(String error) {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
            });
        }
    }
    
    /**
     * 初始化屏幕捕获权限处理器
     */
    private void initializeScreenCapturePermissionLauncher() {
        screenCapturePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    Log.i(TAG, "✅ 屏幕捕获权限授权成功");
                    
                    // 通过ScreenCaptureService启动捕获
                    if (isServiceBound && screenCaptureService != null && data != null) {
                        ScreenCaptureManager screenManager = screenCaptureService.getScreenCaptureManager();
                        if (screenManager != null) {
                            screenManager.startCapture(result.getResultCode(), data);
                            
                            // 更新权限状态
                            if (pendingScreenPermission) {
                                UserManager userManager = new UserManager(requireContext());
                                String phone = userManager.getCurrentUsername();
                                if (phone != null) {
                                    permissionManager.updatePermission(phone, "screen", true);
                                    Toast.makeText(getContext(), "屏幕共享已开启", Toast.LENGTH_SHORT).show();
                                }
                                pendingScreenPermission = false;
                            }
                        } else {
                            Log.e(TAG, "❌ ScreenCaptureManager未初始化");
                            revertScreenSwitchState();
                        }
                    } else {
                        Log.e(TAG, "❌ ScreenCaptureService未连接或数据为空");
                        revertScreenSwitchState();
                    }
                } else {
                    Log.w(TAG, "🚫 用户拒绝了屏幕捕获权限");
                    Toast.makeText(getContext(), "需要屏幕捕获权限才能开启屏幕共享", Toast.LENGTH_LONG).show();
                    revertScreenSwitchState();
                    pendingScreenPermission = false;
                }
            }
        );
    }
    
    /**
     * 请求屏幕捕获权限
     */
    private void requestScreenCapturePermission(String phone) {
        try {
            Log.i(TAG, "📱 请求屏幕捕获权限");
            
            // 先启动并绑定ScreenCaptureService
            startAndBindScreenCaptureService();
            
            MediaProjectionManager mediaProjectionManager = 
                (MediaProjectionManager) requireContext().getSystemService(requireContext().MEDIA_PROJECTION_SERVICE);
            
            if (mediaProjectionManager != null) {
                Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
                pendingScreenPermission = true;
                screenCapturePermissionLauncher.launch(captureIntent);
            } else {
                Log.e(TAG, "❌ MediaProjectionManager未初始化");
                Toast.makeText(getContext(), "系统不支持屏幕捕获功能", Toast.LENGTH_LONG).show();
                revertScreenSwitchState();
            }
        } catch (Exception e) {
            Log.e(TAG, "请求屏幕捕获权限失败: " + e.getMessage(), e);
            Toast.makeText(getContext(), "请求屏幕捕获权限失败", Toast.LENGTH_LONG).show();
            revertScreenSwitchState();
        }
    }
    
    /**
     * 启动并绑定ScreenCaptureService
     */
    private void startAndBindScreenCaptureService() {
        try {
            Log.i(TAG, "🚀 启动ScreenCaptureService");
            Intent serviceIntent = new Intent(requireContext(), ScreenCaptureService.class);
            
            // 使用Android 14+兼容的前台服务启动方式
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // API 34+
                    // Android 14+ 需要特殊的启动方式
                    requireContext().startForegroundService(serviceIntent);
                    Log.i(TAG, "✅ 使用Android 14+方式启动前台服务");
                } else {
                    // 兼容低版本Android
                    requireContext().startForegroundService(serviceIntent);
                    Log.i(TAG, "✅ 使用传统方式启动前台服务");
                }
            } catch (SecurityException e) {
                Log.e(TAG, "❌ 前台服务启动失败 - 权限错误: " + e.getMessage(), e);
                Toast.makeText(getContext(), "权限不足，无法启动屏幕捕获服务", Toast.LENGTH_LONG).show();
                return;
            }
            
            // 绑定服务
            if (!isServiceBound) {
                boolean bindResult = requireContext().bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
                Log.i(TAG, bindResult ? "✅ 服务绑定请求成功" : "❌ 服务绑定请求失败");
            }
        } catch (Exception e) {
            Log.e(TAG, "启动ScreenCaptureService失败: " + e.getMessage(), e);
            Toast.makeText(getContext(), "启动屏幕捕获服务失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 停止屏幕捕获
     */
    private void stopScreenCapture() {
        try {
            if (isServiceBound && screenCaptureService != null) {
                ScreenCaptureManager screenManager = screenCaptureService.getScreenCaptureManager();
                if (screenManager != null) {
                    screenManager.stopCapture();
                    Log.i(TAG, "⏹️ 屏幕捕获已停止");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "停止屏幕捕获失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 恢复屏幕开关状态
     */
    private void revertScreenSwitchState() {
        isUpdatingUI = true;
        binding.switchScreenShare.setChecked(false);
        isUpdatingUI = false;
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        
        // 停止屏幕捕获
        stopScreenCapture();
        
        // 解绑服务
        if (isServiceBound && getContext() != null) {
            try {
                getContext().unbindService(serviceConnection);
                isServiceBound = false;
                Log.i(TAG, "✅ ScreenCaptureService已解绑");
            } catch (Exception e) {
                Log.e(TAG, "解绑ScreenCaptureService失败: " + e.getMessage(), e);
            }
        }
        
        if (permissionManager != null) {
            permissionManager.setPermissionChangeListener(null);
        }
        binding = null;
    }

}
