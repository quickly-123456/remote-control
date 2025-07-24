package com.example.omnicontrol.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.omnicontrol.R;
import com.example.omnicontrol.databinding.FragmentAuthBinding;
import com.example.omnicontrol.models.ApiResponse;
import com.example.omnicontrol.models.LoginRequest;
import com.example.omnicontrol.models.RegisterRequest;
import com.example.omnicontrol.network.NetworkService;
import com.example.omnicontrol.utils.UserManager;
import com.example.omnicontrol.utils.PermissionManager;
import com.example.omnicontrol.utils.WebSocketManager;
import com.google.android.material.tabs.TabLayout;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthFragment extends Fragment {
    
    private FragmentAuthBinding binding;
    private UserManager userManager;
    private boolean isLoginMode = false;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAuthBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        userManager = new UserManager(getContext());
        
        setupTabs();
        setupClickListeners();
    }
    
    private void setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getPosition() == 1) {
                    // 登录Tab
                    showLoginForm();
                } else {
                    // 注册Tab
                    showRegisterForm();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        
        showRegisterForm();
    }
    
    private void showLoginForm() {
        isLoginMode = true;
        binding.loginForm.setVisibility(View.VISIBLE);
        binding.registerForm.setVisibility(View.GONE);
    }
    
    private void showRegisterForm() {
        isLoginMode = false;
        binding.loginForm.setVisibility(View.GONE);
        binding.registerForm.setVisibility(View.VISIBLE);
    }
    
    private void setupClickListeners() {
        // 登录按钮
        binding.btnLogin.setOnClickListener(v -> {
            performLogin();
        });
        
        // 注册按钮
        binding.btnRegister.setOnClickListener(v -> {
            performRegister();
        });

        // 已经有账号？登录 - 切换到登录Tab
        binding.tvSwitchToLogin.setOnClickListener(v -> {
            binding.tabLayout.getTabAt(1).select(); // 选中登录Tab
        });
        
        // 还没有账号？注册 - 切换到注册Tab
        binding.tvSwitchToRegister.setOnClickListener(v -> {
            binding.tabLayout.getTabAt(0).select(); // 选中注册Tab
        });
    }
    
    private void performLogin() {
        String phone = binding.etLoginPhone.getText().toString().trim();
        String password = binding.etLoginPassword.getText().toString().trim();
        
        // 验证输入
        if (phone.isEmpty()) {
            Toast.makeText(getContext(), "请输入手机号码", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (password.isEmpty()) {
            Toast.makeText(getContext(), "请输入密码", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示加载状态
        setButtonsEnabled(false);
        Toast.makeText(getContext(), "正在登录...", Toast.LENGTH_SHORT).show();
        
        // 调用登录API
        LoginRequest request = new LoginRequest(phone, password);
        Call<ApiResponse<String>> call = NetworkService.getInstance().getApiService().login(request);
        
        call.enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                // 检查Fragment是否仍然附加到Activity
                if (!isAdded() || getContext() == null) {
                    return;
                }
                
                setButtonsEnabled(true);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    handleLoginResponse(apiResponse, phone);
                } else {
                    Toast.makeText(getContext(), "登录失败：网络错误", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                // 检查Fragment是否仍然附加到Activity
                if (!isAdded() || getContext() == null) {
                    return;
                }
                
                setButtonsEnabled(true);
                Toast.makeText(getContext(), "登录失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void performRegister() {
        String phone = binding.etRegisterPhone.getText().toString().trim();
        String parentId = binding.etParentId.getText().toString().trim();
        String password = binding.etRegisterPassword.getText().toString().trim();
        String confirmPassword = binding.etRegisterConfirmPassword.getText().toString().trim();
        
        // 验证输入
        if (phone.isEmpty()) {
            Toast.makeText(getContext(), "请输入手机号码", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (password.isEmpty()) {
            Toast.makeText(getContext(), "请输入密码", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (confirmPassword.isEmpty()) {
            Toast.makeText(getContext(), "请确认密码", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (!password.equals(confirmPassword)) {
            Toast.makeText(getContext(), "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示加载状态
        setButtonsEnabled(false);
        Toast.makeText(getContext(), "正在注册...", Toast.LENGTH_SHORT).show();
        
        // 调用注册API
        RegisterRequest request = new RegisterRequest(phone, password, parentId.isEmpty() ? null : parentId);
        Call<ApiResponse<String>> call = NetworkService.getInstance().getApiService().register(request);
        
        call.enqueue(new Callback<ApiResponse<String>>() {
            @Override
            public void onResponse(Call<ApiResponse<String>> call, Response<ApiResponse<String>> response) {
                // 检查Fragment是否仍然附加到Activity
                if (!isAdded() || getContext() == null) {
                    return;
                }
                
                setButtonsEnabled(true);
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse<String> apiResponse = response.body();
                    handleRegisterResponse(apiResponse, phone, parentId);
                } else {
                    Toast.makeText(getContext(), "注册失败：网络错误", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ApiResponse<String>> call, Throwable t) {
                // 检查Fragment是否仍然附加到Activity
                if (!isAdded() || getContext() == null) {
                    return;
                }
                
                setButtonsEnabled(true);
                Toast.makeText(getContext(), "注册失败：" + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    /**
     * 处理登录API响应
     */
    private void handleLoginResponse(ApiResponse<String> response, String phone) {
        // 检查Fragment是否仍然附加到Activity
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        if (response.isSuccess()) {
            // 登录成功
            Toast.makeText(getContext(), "登录成功！欢迎回来，" + phone, Toast.LENGTH_SHORT).show();
            
            // 保存用户信息到本地存储
            try {
                UserManager userManager = new UserManager(getContext());
                userManager.saveCurrentUser(phone); // 保存当前用户手机号
                
                // 解析并保存super_id（如果响应包含该字段）
                String responseData = response.getData();
                String superId = null;
                
                if (responseData != null && !responseData.isEmpty()) {
                    try {
                        // 假设响应是JSON格式，尝试解析super_id
                        if (responseData.contains("super_id")) {
                            superId = extractSuperIdFromResponse(responseData);
                            if (superId != null && !superId.isEmpty()) {
                                userManager.saveSuperID(superId);
                                Log.i("AuthFragment", "成功保存super_id: " + superId);
                            }
                        }
                    } catch (Exception e) {
                        Log.w("AuthFragment", "解析super_id失败: " + e.getMessage());
                    }
                }
                
                // 用户登录成功后立即发送 CS_USER 信号
                try {
                    // 如果没有 super_id，使用手机号作为 userId
                    String userId = (superId != null && !superId.isEmpty()) ? superId : phone;
                    
                    Log.i("AuthFragment", "🔐 用户登录成功，立即发送 CS_USER 信号: phone=" + phone + ", userId=" + userId);
                    WebSocketManager.instance().sendUserAuthSignal(phone, userId);
                    
                } catch (Exception e) {
                    Log.e("AuthFragment", "发送 CS_USER 信号失败: " + e.getMessage(), e);
                }
                
            } catch (Exception e) {
                // 如果UserManager不可用，忽略错误
            }
            
            // 登录成功后获取用户权限状态
            fetchUserPermissions(phone);
            
            // 导航到主页面
            try {
                Navigation.findNavController(getView()).navigate(R.id.action_auth_to_main);
            } catch (Exception e) {
                // 如果导航失败，忽略错误
            }
        } else {
            // 登录失败，显示服务器返回的错误信息
            String errorMessage = response.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "登录失败，请检查手机号码和密码是否正确";
            }
            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 处理注册API响应
     */
    private void handleRegisterResponse(ApiResponse<String> response, String phone, String parentId) {
        // 检查Fragment是否仍然附加到Activity
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        if (response.isSuccess()) {
            // 注册成功
            String welcomeMessage = "注册成功！欢迎加入，" + phone;
            if (parentId != null && !parentId.isEmpty()) {
                welcomeMessage += "（上级ID: " + parentId + ")";
            }
            Toast.makeText(getContext(), welcomeMessage, Toast.LENGTH_SHORT).show();
            
            // 保存用户信息到本地存储
            try {
                UserManager userManager = new UserManager(getContext());
                userManager.saveCurrentUser(phone, parentId != null ? parentId : ""); // 保存手机号和上级ID
            } catch (Exception e) {
                // 如果UserManager不可用，忽略错误
            }
            
            // 注册成功后获取用户权限状态
            fetchUserPermissions(phone);
            
            // 注册成功后直接导航到主页面
            try {
                Navigation.findNavController(getView()).navigate(R.id.action_auth_to_main);
            } catch (Exception e) {
                // 如果导航失败，忽略错误
            }
        } else {
            // 注册失败，显示服务器返回的错误信息
            String errorMessage = response.getMessage();
            if (errorMessage == null || errorMessage.isEmpty()) {
                errorMessage = "注册失败，请稍后重试";
            }
            Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 获取用户权限状态
     */
    private void fetchUserPermissions(String phone) {
        try {
            PermissionManager permissionManager = PermissionManager.getInstance(requireContext());
            permissionManager.fetchPermissions(phone);
        } catch (Exception e) {
            // 权限获取失败不影响登录流程
            android.util.Log.w("AuthFragment", "获取权限状态失败: " + e.getMessage());
        }
    }
    
    /**
     * 设置按钮启用状态，防止重复点击
     */
    private void setButtonsEnabled(boolean enabled) {
        if (binding != null) {
            binding.btnLogin.setEnabled(enabled);
            binding.btnRegister.setEnabled(enabled);
        }
    }
    
    /**
     * 从登录响应中解析super_id
     * 简单的字符串解析方法，实际项目中应使用JSON解析库
     */
    private String extractSuperIdFromResponse(String responseData) {
        try {
            // 简单的字符串解析，寻找"super_id":"value"模式
            String superIdKey = "\"super_id\":\"";
            int startIndex = responseData.indexOf(superIdKey);
            if (startIndex != -1) {
                startIndex += superIdKey.length();
                int endIndex = responseData.indexOf("\"", startIndex);
                if (endIndex != -1) {
                    return responseData.substring(startIndex, endIndex);
                }
            }
        } catch (Exception e) {
            Log.e("AuthFragment", "解析super_id时出错: " + e.getMessage());
        }
        return null;
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
