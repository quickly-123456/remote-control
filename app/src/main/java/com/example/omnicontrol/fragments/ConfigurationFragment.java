package com.example.omnicontrol.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.omnicontrol.R;
import com.example.omnicontrol.databinding.FragmentConfigurationBinding;
import com.example.omnicontrol.utils.SystemInfoManager;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ConfigurationFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConfigurationFragment extends Fragment {
    
    private FragmentConfigurationBinding binding;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentConfigurationBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 初始化系统信息管理器
        SystemInfoManager systemInfoManager = new SystemInfoManager(requireContext());
        
        // 获取并更新系统信息
        updateSystemInfo(systemInfoManager);
        
        // 设置点击事件
        setupClickListeners();
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
        android.util.Log.d("ConfigSystemInfo", "Device: " + deviceInfo.brand + " " + deviceInfo.model);
        android.util.Log.d("ConfigSystemInfo", "Network: " + (networkInfo.isConnected ? "已连接" : "未连接"));
        android.util.Log.d("ConfigSystemInfo", "Battery: " + batteryInfo.level + "% (" + batteryInfo.status + ")");
        android.util.Log.d("ConfigSystemInfo", "IP: " + networkInfo.ipAddress);
        
        // 更新UI显示真实的系统信息
        try {
            // 更新设备名称
            String deviceName = deviceInfo.brand + "-" + deviceInfo.model;
            binding.tvConfigDeviceName.setText(deviceName);
            
            // 更新网络类型
            binding.tvConfigNetworkType.setText(networkInfo.networkType);
            
            // 更新上次登录时间（使用当前时间）
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String currentTime = sdf.format(new Date());
            binding.tvConfigLastLogin.setText(currentTime);
            
        } catch (Exception e) {
            android.util.Log.e("ConfigSystemInfo", "更新UI失败: " + e.getMessage());
        }
    }
    
    private void setupClickListeners() {
        // 设置功能选项点击事件
        binding.layoutUpdatePassword.setOnClickListener(v -> {
            // 直接导航到更新密码页面
            Navigation.findNavController(v).navigate(R.id.action_main_to_update_password);
        });

        
        // 退出登录按钮点击事件
        binding.btnLogout.setOnClickListener(v -> {
            // 清除用户登录状态并返回登录页面
            Navigation.findNavController(v).navigate(R.id.action_main_to_auth);
        });
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
