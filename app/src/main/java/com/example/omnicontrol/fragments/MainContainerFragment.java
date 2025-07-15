package com.example.omnicontrol.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.omnicontrol.R;
import com.example.omnicontrol.databinding.FragmentMainContainerBinding;

public class MainContainerFragment extends Fragment {
    
    private static final String KEY_CURRENT_TAB = "current_tab";
    
    private FragmentMainContainerBinding binding;
    private HomeFragment deviceFragment;
    private ConfigurationFragment configurationFragment;
    private AboutFragment aboutFragment;
    private int currentTabId = R.id.nav_device; // 默认选中设备页面
    private boolean isSettingTab = false; // 防止递归调用的标志
    
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentMainContainerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 恢复保存的tab状态
        if (savedInstanceState != null) {
            currentTabId = savedInstanceState.getInt(KEY_CURRENT_TAB, R.id.nav_device);
        }
        
        // 初始化Fragment
        deviceFragment = new HomeFragment(); // 设备页面使用HomeFragment
        configurationFragment = new ConfigurationFragment();
        aboutFragment = new AboutFragment();
        
        // 设置底部导航点击事件
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            if (isSettingTab) {
                return true; // 如果是程序设置，则忽略此事件
            }
            
            int itemId = item.getItemId();
            currentTabId = itemId; // 更新当前tab ID
            if (itemId == R.id.nav_device) {
                showFragment(deviceFragment);
                return true;
            } else if (itemId == R.id.nav_configuration) {
                showFragment(configurationFragment);
                return true;
            } else if (itemId == R.id.nav_about) {
                showFragment(aboutFragment);
                return true;
            }
            return false;
        });
        
        // 设置当前选中的tab并显示对应内容
        isSettingTab = true;
        binding.bottomNavigation.setSelectedItemId(currentTabId);
        isSettingTab = false;
        
        Fragment targetFragment = getFragmentByTabId(currentTabId);
        if (targetFragment != null) {
            showFragment(targetFragment);
        }
    }
    
    private void showFragment(Fragment fragment) {
        FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
    
    private Fragment getFragmentByTabId(int tabId) {
        if (tabId == R.id.nav_device) {
            return deviceFragment;
        } else if (tabId == R.id.nav_configuration) {
            return configurationFragment;
        } else if (tabId == R.id.nav_about) {
            return aboutFragment;
        }
        return deviceFragment; // 默认返回设备页面
    }
    
    // 获取当前选中的tab ID
    public int getCurrentTabId() {
        return currentTabId;
    }
    
    // 设置选中的tab
    public void setCurrentTab(int tabId) {
        if (binding != null) {
            currentTabId = tabId; // 更新当前tab ID
            
            // 设置底部导航选中状态
            isSettingTab = true;
            binding.bottomNavigation.setSelectedItemId(tabId);
            isSettingTab = false;
            
            // 同时切换到对应的Fragment内容
            Fragment targetFragment = getFragmentByTabId(tabId);
            if (targetFragment != null) {
                showFragment(targetFragment);
            }
        }
    }
    
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(KEY_CURRENT_TAB, currentTabId);
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
