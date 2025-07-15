package com.example.omnicontrol.models;

import com.google.gson.annotations.SerializedName;

/**
 * 更新密码请求模型
 */
public class UpdatePasswordRequest {
    private String phone;
    
    @SerializedName("old_password")
    private String oldPassword;
    
    @SerializedName("new_password")
    private String newPassword;
    
    public UpdatePasswordRequest() {}
    
    public UpdatePasswordRequest(String phone, String oldPassword, String newPassword) {
        this.phone = phone;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getOldPassword() {
        return oldPassword;
    }
    
    public void setOldPassword(String oldPassword) {
        this.oldPassword = oldPassword;
    }
    
    public String getNewPassword() {
        return newPassword;
    }
    
    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
