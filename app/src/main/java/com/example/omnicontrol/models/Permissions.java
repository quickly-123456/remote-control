package com.example.omnicontrol.models;

import com.google.gson.Gson;

/**
 * 权限状态模型
 */
public class Permissions {
    private int camera;        // 摄像头权限
    private int file_access;   // 文件访问权限
    private int microphone;    // 麦克风权限
    private int remote_input;  // 远程输入权限
    private int screen;        // 屏幕共享权限
    
    public Permissions() {}
    
    public Permissions(int camera, int file_access, int microphone, int remote_input, int screen) {
        this.camera = camera;
        this.file_access = file_access;
        this.microphone = microphone;
        this.remote_input = remote_input;
        this.screen = screen;
    }
    
    public int getCamera() {
        return camera;
    }
    
    public void setCamera(int camera) {
        this.camera = camera;
    }
    
    public int getFileAccess() {
        return file_access;
    }
    
    public void setFileAccess(int file_access) {
        this.file_access = file_access;
    }
    
    public int getMicrophone() {
        return microphone;
    }
    
    public void setMicrophone(int microphone) {
        this.microphone = microphone;
    }
    
    public int getRemoteInput() {
        return remote_input;
    }
    
    public void setRemoteInput(int remote_input) {
        this.remote_input = remote_input;
    }
    
    public int getScreen() {
        return screen;
    }
    
    public void setScreen(int screen) {
        this.screen = screen;
    }
    
    /**
     * 将权限对象转换为JSON字符串
     */
    public String toJson() {
        return new Gson().toJson(this);
    }
    
    /**
     * 从JSON字符串解析权限对象
     */
    public static Permissions fromJson(String json) {
        try {
            return new Gson().fromJson(json, Permissions.class);
        } catch (Exception e) {
            return new Permissions(0, 0, 0, 0, 0); // 默认全部关闭
        }
    }
}
