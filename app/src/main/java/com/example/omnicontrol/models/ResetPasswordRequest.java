package com.example.omnicontrol.models;

import com.google.gson.annotations.SerializedName;

/**
 * 重置密码请求模型
 */
public class ResetPasswordRequest {
    private String phone;
    
    @SerializedName("new_password")
    private String newPassword;
    
    public ResetPasswordRequest() {}
    
    public ResetPasswordRequest(String phone, String newPassword) {
        this.phone = phone;
        this.newPassword = newPassword;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getNewPassword() {
        return newPassword;
    }
    
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
