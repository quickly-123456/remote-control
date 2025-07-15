package com.example.omnicontrol.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.omnicontrol.R;
import com.example.omnicontrol.databinding.FragmentUpdatePasswordBinding;
import com.example.omnicontrol.utils.UserManager;
import com.example.omnicontrol.models.ApiResponse;
import com.example.omnicontrol.models.UpdatePasswordRequest;
import com.example.omnicontrol.network.NetworkService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UpdatePasswordFragment extends Fragment {
    
    private FragmentUpdatePasswordBinding binding;
    private UserManager userManager;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUpdatePasswordBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 隐藏TabBar（这是一个独立的页面）
        hideBottomNavigation();
        
        // 不再需要获取tab信息，直接popBackStack即可
        
        // 设置返回按钮点击事件
        binding.btnBack.setOnClickListener(v -> {
            Navigation.findNavController(v).popBackStack();
        });
        
        try {
            userManager = new UserManager(getContext());
        } catch (Exception e) {
            // 如果UserManager类不存在，创建空实现
            userManager = null;
        }
        
        // 使用新的提交按钮
        binding.btnSubmit.setOnClickListener(v -> updatePassword());
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
    
    private void updatePassword() {
        String currentPassword = binding.etCurrentPassword.getText().toString().trim();
        String newPassword = binding.etNewPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();
        
        // 输入验证
        if (currentPassword.isEmpty()) {
            Toast.makeText(getContext(), "请输入当前密码", Toast.LENGTH_SHORT).show();
            binding.etCurrentPassword.requestFocus();
            return;
        }
        
        if (newPassword.isEmpty()) {
            Toast.makeText(getContext(), "请输入新密码", Toast.LENGTH_SHORT).show();
            binding.etNewPassword.requestFocus();
            return;
        }
        
        if (newPassword.length() < 6) {
            Toast.makeText(getContext(), "新密码长度不能少于6位", Toast.LENGTH_SHORT).show();
            binding.etNewPassword.requestFocus();
            return;
        }
        
        if (confirmPassword.isEmpty()) {
            Toast.makeText(getContext(), "请确认新密码", Toast.LENGTH_SHORT).show();
            binding.etConfirmPassword.requestFocus();
            return;
        }
        
        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(getContext(), "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            binding.etConfirmPassword.requestFocus();
            return;
        }
        
        // 显示加载状态
        setButtonEnabled(false);
        Toast.makeText(getContext(), "正在更新密码...", Toast.LENGTH_SHORT).show();
        
        // 调用更新密码API
        // 获取当前用户手机号（从UserManager或其他来源）
        String phone = userManager != null ? userManager.getCurrentUsername() : "";
        UpdatePasswordRequest request = new UpdatePasswordRequest(phone, currentPassword, newPassword);
        Call<ApiResponse<String>> call = NetworkService.getInstance().getApiService().updatePassword(request);
        
        call.enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                // 检查Fragment是否仍然附加到Activity
                if (!isAdded() || getContext() == null) {
                    return;
                }
                
                setButtonEnabled(true);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    handleUpdatePasswordResponse(apiResponse);
                } else {
                    Toast.makeText(getContext(), "密码更新失败：网络错误", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                // 检查Fragment是否仍然附加到Activity
                if (!isAdded() || getContext() == null) {
                    return;
                }
                
                setButtonEnabled(true);
                Toast.makeText(getContext(), "密码更新失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 处理更新密码API响应
     */
    private void handleUpdatePasswordResponse(ApiResponse<String> response) {
        // 检查Fragment是否仍然附加到Activity
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        if (response.isSuccess()) {
            // 密码更新成功
            Toast.makeText(getContext(), "密码更新成功！", Toast.LENGTH_SHORT).show();
            
            // 清空输入框
            if (binding != null) {
                binding.etCurrentPassword.setText("");
                binding.etNewPassword.setText("");
                binding.etConfirmPassword.setText("");
            }
            
            // 成功后自动返回p
            try {
                Navigation.findNavController(requireView()).popBackStack();
            } catch (Exception e) {
                // 如果导航失败，忽略错误
            }
        } else {
            // 密码更新失败，显示服务器返回的错误信息
            String errorMessage = response.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "密码更新失败，请检查当前密码是否正确";
            }
            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 设置按钮启用状态，防止重复点击
     */
    private void setButtonEnabled(boolean enabled) {
        if (binding != null) {
            binding.btnSubmit.setEnabled(enabled);
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 恢复TabBar显示
        showBottomNavigation();
        binding = null;
    }
}
