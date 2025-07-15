package com.example.omnicontrol.models;

public class ConnectionRecord {
    private String ipAddress;
    private String connectionTime;
    private String status;
    private String duration;
    
    public ConnectionRecord(String ipAddress, String connectionTime, String status, String duration) {
        this.ipAddress = ipAddress;
        this.connectionTime = connectionTime;
        this.status = status;
        this.duration = duration;
    }
    
    public String getIpAddress() {
        return ipAddress;
    }
    
    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }
    
    public String getConnectionTime() {
        return connectionTime;
    }
    
    public void setConnectionTime(String connectionTime) {
        this.connectionTime = connectionTime;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getDuration() {
        return duration;
    }
    
    public void setDuration(String duration) {
        this.duration = duration;
    }
}
