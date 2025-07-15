package com.example.omnicontrol.network;

import com.example.omnicontrol.models.ApiResponse;
import com.example.omnicontrol.models.LoginRequest;
import com.example.omnicontrol.models.PermissionsRequest;
import com.example.omnicontrol.models.PermissionsResponse;
import com.example.omnicontrol.models.RegisterRequest;
import com.example.omnicontrol.models.ResetPasswordRequest;
import com.example.omnicontrol.models.SetPermissionsRequest;
import com.example.omnicontrol.models.UpdatePasswordRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * API接口定义
 */
public interface ApiService {
    
    /**
     * 用户注册
     * @param request 注册请求参数
     * @return 注册响应
     */
    @POST("api/register")
    Call<ApiResponse<String>> register(@Body RegisterRequest request);
    
    /**
     * 用户登录
     * @param request 登录请求参数
     * @return 登录响应，包含用户信息
     */
    @POST("api/login")
    Call<ApiResponse<String>> login(@Body LoginRequest request);
    
    /**
     * 重置密码（忘记密码）
     * @param request 重置密码请求参数
     * @return 重置密码响应
     */
    @POST("api/resetpassword")
    Call<ApiResponse<String>> resetPassword(@Body ResetPasswordRequest request);
    
    /**
     * 更新密码（知道当前密码）
     * @param request 更新密码请求参数
     * @return 更新密码响应
     */
    @POST("api/updatepassword")
    Call<ApiResponse<String>> updatePassword(@Body UpdatePasswordRequest request);
    
    /**
     * 获取权限
     * @param request 获取权限请求参数
     * @return 权限响应
     */
    @POST("api/getpermissions")
    Call<PermissionsResponse> getPermissions(@Body PermissionsRequest request);
    
    /**
     * 设置权限
     * @param request 设置权限请求参数
     * @return 权限响应
     */
    @POST("api/setpermissions")
    Call<PermissionsResponse> setPermissions(@Body SetPermissionsRequest request);
}
