package com.example.omnicontrol.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class ConfigurationManager {
    private static final String PREF_NAME = "config_prefs";
    
    // 网络配置
    private static final String KEY_SERVER_IP = "server_ip";
    private static final String KEY_SERVER_PORT = "server_port";
    private static final String KEY_CONNECTION_TIMEOUT = "connection_timeout";
    private static final String KEY_AUTO_RECONNECT = "auto_reconnect";
    
    // 显示设置
    private static final String KEY_SCREEN_QUALITY = "screen_quality";
    private static final String KEY_FRAME_RATE = "frame_rate";
    private static final String KEY_COLOR_DEPTH = "color_depth";
    
    // 安全设置
    private static final String KEY_ENCRYPTION_ENABLED = "encryption_enabled";
    private static final String KEY_AUTH_REQUIRED = "auth_required";
    
    // 其他设置
    private static final String KEY_SOUND_ENABLED = "sound_enabled";
    private static final String KEY_VIBRATION_ENABLED = "vibration_enabled";
    private static final String KEY_KEEP_SCREEN_ON = "keep_screen_on";
    
    private SharedPreferences sharedPreferences;
    
    public ConfigurationManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        setDefaultValues();
    }
    
    private void setDefaultValues() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        
        // 设置默认网络配置
        if (!sharedPreferences.contains(KEY_SERVER_IP)) {
            editor.putString(KEY_SERVER_IP, "192.168.1.100");
        }
        if (!sharedPreferences.contains(KEY_SERVER_PORT)) {
            editor.putString(KEY_SERVER_PORT, "5900");
        }
        if (!sharedPreferences.contains(KEY_CONNECTION_TIMEOUT)) {
            editor.putString(KEY_CONNECTION_TIMEOUT, "30");
        }
        if (!sharedPreferences.contains(KEY_AUTO_RECONNECT)) {
            editor.putBoolean(KEY_AUTO_RECONNECT, true);
        }
        
        // 设置默认显示配置
        if (!sharedPreferences.contains(KEY_SCREEN_QUALITY)) {
            editor.putString(KEY_SCREEN_QUALITY, "高质量");
        }
        if (!sharedPreferences.contains(KEY_FRAME_RATE)) {
            editor.putString(KEY_FRAME_RATE, "30 FPS");
        }
        if (!sharedPreferences.contains(KEY_COLOR_DEPTH)) {
            editor.putString(KEY_COLOR_DEPTH, "24位");
        }
        
        // 设置默认安全配置
        if (!sharedPreferences.contains(KEY_ENCRYPTION_ENABLED)) {
            editor.putBoolean(KEY_ENCRYPTION_ENABLED, true);
        }
        if (!sharedPreferences.contains(KEY_AUTH_REQUIRED)) {
            editor.putBoolean(KEY_AUTH_REQUIRED, true);
        }
        
        // 设置默认其他配置
        if (!sharedPreferences.contains(KEY_SOUND_ENABLED)) {
            editor.putBoolean(KEY_SOUND_ENABLED, true);
        }
        if (!sharedPreferences.contains(KEY_VIBRATION_ENABLED)) {
            editor.putBoolean(KEY_VIBRATION_ENABLED, true);
        }
        if (!sharedPreferences.contains(KEY_KEEP_SCREEN_ON)) {
            editor.putBoolean(KEY_KEEP_SCREEN_ON, false);
        }
        
        editor.apply();
    }
    
    // 网络配置
    public String getServerIp() {
        return sharedPreferences.getString(KEY_SERVER_IP, "192.168.1.100");
    }
    
    public void setServerIp(String ip) {
        sharedPreferences.edit().putString(KEY_SERVER_IP, ip).apply();
    }
    
    public String getServerPort() {
        return sharedPreferences.getString(KEY_SERVER_PORT, "5900");
    }
    
    public void setServerPort(String port) {
        sharedPreferences.edit().putString(KEY_SERVER_PORT, port).apply();
    }
    
    public String getConnectionTimeout() {
        return sharedPreferences.getString(KEY_CONNECTION_TIMEOUT, "30");
    }
    
    public void setConnectionTimeout(String timeout) {
        sharedPreferences.edit().putString(KEY_CONNECTION_TIMEOUT, timeout).apply();
    }
    
    public boolean isAutoReconnectEnabled() {
        return sharedPreferences.getBoolean(KEY_AUTO_RECONNECT, true);
    }
    
    public void setAutoReconnectEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_RECONNECT, enabled).apply();
    }
    
    // 显示设置
    public String getScreenQuality() {
        return sharedPreferences.getString(KEY_SCREEN_QUALITY, "高质量");
    }
    
    public void setScreenQuality(String quality) {
        sharedPreferences.edit().putString(KEY_SCREEN_QUALITY, quality).apply();
    }
    
    public String getFrameRate() {
        return sharedPreferences.getString(KEY_FRAME_RATE, "30 FPS");
    }
    
    public void setFrameRate(String frameRate) {
        sharedPreferences.edit().putString(KEY_FRAME_RATE, frameRate).apply();
    }
    
    public String getColorDepth() {
        return sharedPreferences.getString(KEY_COLOR_DEPTH, "24位");
    }
    
    public void setColorDepth(String colorDepth) {
        sharedPreferences.edit().putString(KEY_COLOR_DEPTH, colorDepth).apply();
    }
    
    // 安全设置
    public boolean isEncryptionEnabled() {
        return sharedPreferences.getBoolean(KEY_ENCRYPTION_ENABLED, true);
    }
    
    public void setEncryptionEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_ENCRYPTION_ENABLED, enabled).apply();
    }
    
    public boolean isAuthRequired() {
        return sharedPreferences.getBoolean(KEY_AUTH_REQUIRED, true);
    }
    
    public void setAuthRequired(boolean required) {
        sharedPreferences.edit().putBoolean(KEY_AUTH_REQUIRED, required).apply();
    }
    
    // 其他设置
    public boolean isSoundEnabled() {
        return sharedPreferences.getBoolean(KEY_SOUND_ENABLED, true);
    }
    
    public void setSoundEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_SOUND_ENABLED, enabled).apply();
    }
    
    public boolean isVibrationEnabled() {
        return sharedPreferences.getBoolean(KEY_VIBRATION_ENABLED, true);
    }
    
    public void setVibrationEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_VIBRATION_ENABLED, enabled).apply();
    }
    
    public boolean isKeepScreenOn() {
        return sharedPreferences.getBoolean(KEY_KEEP_SCREEN_ON, false);
    }
    
    public void setKeepScreenOn(boolean keepOn) {
        sharedPreferences.edit().putBoolean(KEY_KEEP_SCREEN_ON, keepOn).apply();
    }
    
    // 重置所有配置
    public void resetToDefaults() {
        sharedPreferences.edit().clear().apply();
        setDefaultValues();
    }
    
    // 验证IP地址格式
    public boolean isValidIpAddress(String ip) {
        if (ip == null || ip.isEmpty()) {
            return false;
        }
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            return false;
        }
        for (String part : parts) {
            try {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }
    
    // 验证端口号
    public boolean isValidPort(String port) {
        try {
            int portNum = Integer.parseInt(port);
            return portNum >= 1 && portNum <= 65535;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
