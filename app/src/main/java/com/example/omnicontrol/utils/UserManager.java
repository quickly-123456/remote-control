package com.example.omnicontrol.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Patterns;

public class UserManager {
    private static final String PREF_NAME = "user_prefs";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_PARENT_ID = "parent_id";
    private static final String KEY_SUPER_ID = "super_id";
    
    private SharedPreferences sharedPreferences;
    
    public UserManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public boolean register(String username, String password, String confirmPassword) {
        // 验证输入
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            return false;
        }
        
        if (username.length() < 3) {
            return false;
        }
        
        if (password.length() < 6) {
            return false;
        }
        
        if (!password.equals(confirmPassword)) {
            return false;
        }
        
        // 检查用户名是否已存在
        if (isUserExists(username)) {
            return false;
        }
        
        // 保存用户信息
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_PASSWORD, password);
        editor.apply();
        
        return true;
    }
    
    public boolean login(String username, String password) {
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            return false;
        }
        
        String savedUsername = sharedPreferences.getString(KEY_USERNAME, "");
        String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");
        
        if (username.equals(savedUsername) && password.equals(savedPassword)) {
            // 登录成功，保存登录状态
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(KEY_IS_LOGGED_IN, true);
            editor.apply();
            return true;
        }
        
        return false;
    }
    
    public void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(KEY_IS_LOGGED_IN, false);
        editor.apply();
    }
    
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    public String getCurrentUsername() {
        return sharedPreferences.getString(KEY_USERNAME, "");
    }
    
    /**
     * 保存当前用户信息并设置登录状态
     */
    public void saveCurrentUser(String username) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USERNAME, username);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }
    
    private boolean isUserExists(String username) {
        String savedUsername = sharedPreferences.getString(KEY_USERNAME, "");
        return username.equals(savedUsername);
    }
    
    public String getRegisterError(String username, String password, String confirmPassword) {
        if (TextUtils.isEmpty(username)) {
            return "用户名不能为空";
        }
        
        if (username.length() < 3) {
            return "用户名至少需要3个字符";
        }
        
        if (TextUtils.isEmpty(password)) {
            return "密码不能为空";
        }
        
        if (password.length() < 6) {
            return "密码至少需要6个字符";
        }
        
        if (!password.equals(confirmPassword)) {
            return "两次输入的密码不一致";
        }
        
        if (isUserExists(username)) {
            return "用户名已存在";
        }
        
        return null;
    }
    
    public String getLoginError(String username, String password) {
        if (TextUtils.isEmpty(username)) {
            return "用户名不能为空";
        }
        
        if (TextUtils.isEmpty(password)) {
            return "密码不能为空";
        }
        
        return "用户名或密码错误";
    }
    
    public boolean updatePassword(String currentPassword, String newPassword) {
        String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");
        
        // 验证当前密码
        if (!currentPassword.equals(savedPassword)) {
            return false;
        }
        
        // 更新密码
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_PASSWORD, newPassword);
        editor.apply();
        
        return true;
    }
    
    public String getUpdatePasswordError(String currentPassword, String newPassword, String confirmPassword) {
        if (TextUtils.isEmpty(currentPassword)) {
            return "请输入当前密码";
        }
        
        if (TextUtils.isEmpty(newPassword)) {
            return "请输入新密码";
        }
        
        if (newPassword.length() < 6) {
            return "新密码长度至少6位";
        }
        
        if (!newPassword.equals(confirmPassword)) {
            return "两次输入的新密码不一致";
        }
        
        String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");
        if (!currentPassword.equals(savedPassword)) {
            return "当前密码错误";
        }
        
        return null;
    }
    
    /**
     * 保存上级ID
     */
    public void saveParentId(String parentId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_PARENT_ID, parentId);
        editor.apply();
    }
    
    /**
     * 获取当前用户的上级ID
     */
    public String getParentId() {
        return sharedPreferences.getString(KEY_PARENT_ID, "");
    }
    
    /**
     * 扩展的保存用户信息方法，包含上级ID
     */
    public void saveCurrentUser(String username, String parentId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USERNAME, username);
        editor.putString(KEY_PARENT_ID, parentId);
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }
    
    /**
     * 保存Super ID（登录接口返回的super_id）
     */
    public void saveSuperID(String superId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_SUPER_ID, superId);
        editor.apply();
    }
    
    /**
     * 获取Super ID
     */
    public String getSuperID() {
        return sharedPreferences.getString(KEY_SUPER_ID, "");
    }
    
    /**
     * 扩展的保存用户信息方法，包含super_id
     */
    public void saveCurrentUser(String username, String superId, boolean isSuperId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USERNAME, username);
        if (isSuperId) {
            editor.putString(KEY_SUPER_ID, superId);
        } else {
            editor.putString(KEY_PARENT_ID, superId);
        }
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.apply();
    }
}
