package com.example.omnicontrol.models;

import com.google.gson.annotations.SerializedName;

/**
 * 用户模型
 */
public class User {
    @SerializedName("id")
    private int id;
    
    @SerializedName("phone")
    private String phone;
    
    @SerializedName("password")
    private String password;
    
    @SerializedName("status")
    private int status;
    
    public User() {}
    
    public User(int id, String phone, String password, int status) {
        this.id = id;
        this.phone = phone;
        this.password = password;
        this.status = status;
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
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
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public boolean isActive() {
        return status == 0;
    }
    
    public boolean isBlocked() {
        return status == 1;
    }
    
    public boolean isDeleted() {
        return status == 2;
    }
}
