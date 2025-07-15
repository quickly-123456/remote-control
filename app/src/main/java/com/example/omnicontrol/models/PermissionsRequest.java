package com.example.omnicontrol.models;

/**
 * 获取权限请求模型
 */
public class PermissionsRequest {
    private String phone;
    
    public PermissionsRequest() {}
    
    public PermissionsRequest(String phone) {
        this.phone = phone;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
}
