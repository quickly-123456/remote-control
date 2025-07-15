package com.example.omnicontrol.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.example.omnicontrol.models.ConnectionRecord;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ConnectionManager {
    private static final String PREF_NAME = "connection_prefs";
    private static final String KEY_CONNECTIONS = "connections";
    
    private SharedPreferences sharedPreferences;
    private Gson gson;
    
    public ConnectionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }
    
    public void addConnection(String deviceName, String ipAddress, boolean isConnected) {
        List<ConnectionRecord> connections = getAllConnections();
        
        // 格式化当前时间
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        
        ConnectionRecord newRecord = new ConnectionRecord(
            ipAddress,
            currentTime,
            isConnected ? "已连接" : "已断开",
            "00:00" // 默认持续时间
        );
        
        connections.add(0, newRecord); // 添加到列表开头（最新的在前）
        
        // 保持最多50条记录
        if (connections.size() > 50) {
            connections = connections.subList(0, 50);
        }
        
        saveConnections(connections);
    }
    
    public List<ConnectionRecord> getAllConnections() {
        String connectionsJson = sharedPreferences.getString(KEY_CONNECTIONS, null);
        if (connectionsJson == null) {
            return getDefaultConnections();
        }
        
        Type listType = new TypeToken<List<ConnectionRecord>>(){}.getType();
        List<ConnectionRecord> connections = gson.fromJson(connectionsJson, listType);
        return connections != null ? connections : new ArrayList<>();
    }
    
    public void clearAllConnections() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(KEY_CONNECTIONS);
        editor.apply();
    }
    
    public void deleteConnection(int index) {
        List<ConnectionRecord> connections = getAllConnections();
        if (index >= 0 && index < connections.size()) {
            connections.remove(index);
            saveConnections(connections);
        }
    }
    
    public int getTotalConnections() {
        return getAllConnections().size();
    }
    
    public int getSuccessfulConnections() {
        List<ConnectionRecord> connections = getAllConnections();
        int count = 0;
        for (ConnectionRecord record : connections) {
            if ("已连接".equals(record.getStatus())) {
                count++;
            }
        }
        return count;
    }
    
    public int getFailedConnections() {
        return getTotalConnections() - getSuccessfulConnections();
    }
    
    private void saveConnections(List<ConnectionRecord> connections) {
        String connectionsJson = gson.toJson(connections);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_CONNECTIONS, connectionsJson);
        editor.apply();
    }
    
    private List<ConnectionRecord> getDefaultConnections() {
        List<ConnectionRecord> defaultConnections = new ArrayList<>();
        
        // 添加一些示例连接记录
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());
        long now = System.currentTimeMillis();
        
        defaultConnections.add(new ConnectionRecord(
            "192.168.1.100", 
            sdf.format(new Date(now - 1000000)), 
            "已连接",
            "05:30"
        ));
        
        defaultConnections.add(new ConnectionRecord(
            "192.168.0.50", 
            sdf.format(new Date(now - 2000000)), 
            "已断开",
            "02:15"
        ));
        
        defaultConnections.add(new ConnectionRecord(
            "192.168.1.200", 
            sdf.format(new Date(now - 3000000)), 
            "已连接",
            "08:45"
        ));
        
        defaultConnections.add(new ConnectionRecord(
            "192.168.1.110", 
            sdf.format(new Date(now - 4000000)), 
            "已断开",
            "01:30"
        ));
        
        defaultConnections.add(new ConnectionRecord(
            "192.168.1.150", 
            sdf.format(new Date(now - 5000000)), 
            "已连接",
            "03:20"
        ));
        
        saveConnections(defaultConnections);
        return defaultConnections;
    }
}
