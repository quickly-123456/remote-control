package com.example.omnicontrol.models;

import com.google.gson.annotations.SerializedName;

/**
 * 注册请求模型
 */
public class RegisterRequest {
    private String phone;
    private String password;
    
    @SerializedName("super_id")
    private String superId;
    
    public RegisterRequest() {}
    
    public RegisterRequest(String phone, String password, String superId) {
        this.phone = phone;
        this.password = password;
        this.superId = superId;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getSuperId() {
        return superId;
    }
    
    public void setSuperId(String superId) {
        this.superId = superId;
    }
}
