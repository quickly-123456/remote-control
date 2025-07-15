package com.example.omnicontrol.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.omnicontrol.databinding.FragmentStatisticsBinding;
import com.example.omnicontrol.utils.ConnectionManager;
import com.example.omnicontrol.utils.UserManager;
import java.text.DecimalFormat;

public class StatisticsFragment extends Fragment {
    
    private FragmentStatisticsBinding binding;
    private ConnectionManager connectionManager;
    private UserManager userManager;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        connectionManager = new ConnectionManager(getContext());
        userManager = new UserManager(getContext());
        
        setupStatistics();
    }
    
    private void setupStatistics() {
        // 连接统计
        int totalConnections = connectionManager.getTotalConnections();
        int successfulConnections = connectionManager.getSuccessfulConnections();
        int failedConnections = connectionManager.getFailedConnections();
        
        // 计算成功率
        double successRate = totalConnections > 0 ? 
            (double) successfulConnections / totalConnections * 100 : 0;
        
        DecimalFormat df = new DecimalFormat("#.#");
        
        // 模拟数据传输量（基于连接次数）
        double dataTransferred = totalConnections * 0.5 + Math.random() * 10;
        
        // 模拟在线时长（基于成功连接）
        long uptimeHours = successfulConnections * 2 + (long)(Math.random() * 50);
        
        // 设置统计数据
        binding.tvTotalConnections.setText(String.valueOf(totalConnections));
        binding.tvDataTransferred.setText(df.format(dataTransferred) + " GB");
        binding.tvUptime.setText(df.format(successRate) + "%");
        binding.tvActiveUsers.setText(String.valueOf(uptimeHours) + "h");
        
        // 显示详细信息
        updateDetailedStats(totalConnections, successfulConnections, failedConnections);
    }
    
    private void updateDetailedStats(int total, int successful, int failed) {
        // 如果布局中有更多统计显示控件，可以在这里设置
        String currentUser = userManager.getCurrentUsername();
        if (!currentUser.isEmpty()) {
            // 可以显示当前用户的连接统计
            // binding.tvCurrentUser.setText("当前用户: " + currentUser);
        }
        
        // 可以添加更多统计信息
        // binding.tvSuccessfulConnections.setText("成功连接: " + successful);
        // binding.tvFailedConnections.setText("失败连接: " + failed);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
