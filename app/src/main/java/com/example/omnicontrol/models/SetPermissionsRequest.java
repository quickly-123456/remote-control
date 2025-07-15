package com.example.omnicontrol.models;

/**
 * 设置权限请求模型
 */
public class SetPermissionsRequest {
    private String phone;
    private String permissions;
    
    public SetPermissionsRequest() {}
    
    public SetPermissionsRequest(String phone, String permissions) {
        this.phone = phone;
        this.permissions = permissions;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getPermissions() {
        return permissions;
    }
    
    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
}
