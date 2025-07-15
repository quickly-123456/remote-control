package com.example.omnicontrol.models;

/**
 * 权限响应模型
 */
public class PermissionsResponse {
    private String message;
    private String permissions;
    private int result;
    
    public PermissionsResponse() {}
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getPermissions() {
        return permissions;
    }
    
    public void setPermissions(String permissions) {
        this.permissions = permissions;
    }
    
    public int getResult() {
        return result;
    }
    
    public void setResult(int result) {
        this.result = result;
    }
    
    public boolean isSuccess() {
        return result == 0;
    }
}
