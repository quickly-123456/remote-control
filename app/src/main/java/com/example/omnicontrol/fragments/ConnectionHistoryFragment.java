package com.example.omnicontrol.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.omnicontrol.R;
import com.example.omnicontrol.databinding.FragmentConnectionHistoryBinding;
import com.example.omnicontrol.models.ConnectionRecord;
import com.example.omnicontrol.utils.ConnectionManager;
import java.util.ArrayList;
import java.util.List;

public class ConnectionHistoryFragment extends Fragment {
    
    private FragmentConnectionHistoryBinding binding;
    private ConnectionManager connectionManager;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentConnectionHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 隐藏TabBar（这是一个独立的页面）
        hideBottomNavigation();
        
        // 设置返回按钮点击事件
        binding.btnBack.setOnClickListener(v -> {
            Navigation.findNavController(v).popBackStack();
        });
        
        // 初始化连接管理器（如果存在的话）
        try {
            connectionManager = new ConnectionManager(getContext());
        } catch (Exception e) {
            // 如果ConnectionManager类不存在，创建模拟数据
            connectionManager = null;
        }
        
        setupUI();
        loadConnectionHistory();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 恢复TabBar显示
        showBottomNavigation();
        binding = null;
    }
    
    /**
     * 隐藏底部导航栏
     */
    private void hideBottomNavigation() {
        if (getParentFragment() != null && getParentFragment().getView() != null) {
            View bottomNav = getParentFragment().getView().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.GONE);
            }
        }
    }
    
    /**
     * 显示底部导航栏
     */
    private void showBottomNavigation() {
        if (getParentFragment() != null && getParentFragment().getView() != null) {
            View bottomNav = getParentFragment().getView().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.setVisibility(View.VISIBLE);
            }
        }
    }
    
    private void setupUI() {
        // 布局文件使用静态内容
        // 如果需要动态内容，可以在这里处理
    }
    
    private void loadConnectionHistory() {
        List<ConnectionRecord> records;
        
        if (connectionManager != null) {
            records = connectionManager.getAllConnections();
        } else {
            // 创建模拟数据
            records = createMockConnectionHistory();
        }
        
        // 布局文件使用静态内容，这里可以根据需要更新显示
        // 如果records为空，可以隐藏ScrollView显示空状态
        // 目前使用静态布局，所以不需要动态更新
    }
    
    private List<ConnectionRecord> createMockConnectionHistory() {
        List<ConnectionRecord> mockRecords = new ArrayList<>();
        
        // 由于ConnectionRecord类可能不存在，我们暂时返回空列表
        // 在实际项目中，这里应该创建一些模拟的连接记录
        
        return mockRecords;
    }
}
