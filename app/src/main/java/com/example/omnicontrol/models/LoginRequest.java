package com.example.omnicontrol.models;

/**
 * 登录请求模型
 */
public class LoginRequest {
    private String phone;
    private String password;
    
    public LoginRequest() {}
    
    public LoginRequest(String phone, String password) {
        this.phone = phone;
        this.password = password;
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
}
