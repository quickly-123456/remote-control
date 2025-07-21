package com.zhongkong.app.model;

public class Device {
    private String name;
    private String phoneNumber;
    private boolean connected;

    public Device(String name, String phoneNumber, boolean connected) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.connected = connected;
    }

    public String getName() { return name; }
    public String getPhoneNumber() { return phoneNumber; }
    public boolean isConnected() { return connected; }
    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}