package com.example.omnicontrol.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import androidx.core.content.ContextCompat;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * 系统信息管理类 - 获取设备真实信息
 */
public class SystemInfoManager {
    
    private Context context;
    
    public SystemInfoManager(Context context) {
        this.context = context;
    }
    
    /**
     * 获取设备基本信息
     */
    public DeviceInfo getDeviceInfo() {
        DeviceInfo deviceInfo = new DeviceInfo();
        
        // 设备制造商和型号
        deviceInfo.manufacturer = Build.MANUFACTURER;
        deviceInfo.model = Build.MODEL;
        deviceInfo.brand = Build.BRAND;
        
        // 系统版本信息
        deviceInfo.androidVersion = Build.VERSION.RELEASE;
        deviceInfo.apiLevel = Build.VERSION.SDK_INT;
        deviceInfo.buildNumber = Build.DISPLAY;
        
        // 设备ID和序列号
        deviceInfo.deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                deviceInfo.serialNumber = Build.getSerial();
            } catch (SecurityException e) {
                deviceInfo.serialNumber = "需要权限";
            }
        } else {
            deviceInfo.serialNumber = Build.SERIAL;
        }
        
        return deviceInfo;
    }
    
    /**
     * 获取网络信息
     */
    public NetworkInfo getNetworkInfo() {
        NetworkInfo info = new NetworkInfo();
        
        try {
            // 检查网络访问权限
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_NETWORK_STATE) 
                != PackageManager.PERMISSION_GRANTED) {
                info.isConnected = false;
                info.networkType = "权限不足";
                info.ipAddress = "需要网络权限";
                return info;
            }
            
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            
            if (activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                info.isConnected = true;
                info.networkType = activeNetwork.getTypeName();
                
                if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                    // 检查WiFi权限
                    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_WIFI_STATE) 
                        == PackageManager.PERMISSION_GRANTED) {
                        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                        
                        if (wifiInfo != null) {
                            info.wifiSSID = wifiInfo.getSSID().replace("\"", "");
                            info.wifiStrength = wifiInfo.getRssi();
                        }
                    }
                }
                
                // 获取IP地址
                try {
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                        NetworkInterface intf = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof java.net.Inet4Address) {
                                info.ipAddress = inetAddress.getHostAddress();
                                break;
                            }
                        }
                    }
                } catch (SocketException e) {
                    info.ipAddress = "无法获取";
                }
            } else {
                info.isConnected = false;
                info.networkType = "未连接";
                info.ipAddress = "无";
            }
        } catch (SecurityException e) {
            // 权限不足时的处理
            info.isConnected = false;
            info.networkType = "权限不足";
            info.ipAddress = "需要网络权限";
            android.util.Log.w("SystemInfoManager", "Network permission denied: " + e.getMessage());
        } catch (Exception e) {
            // 其他异常处理
            info.isConnected = false;
            info.networkType = "获取失败";
            info.ipAddress = "错误";
            android.util.Log.e("SystemInfoManager", "Error getting network info: " + e.getMessage());
        }
        
        return info;
    }
    
    /**
     * 获取电池信息
     */
    public BatteryInfo getBatteryInfo() {
        BatteryInfo batteryInfo = new BatteryInfo();
        
        BatteryManager batteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
        
        if (batteryManager != null) {
            // 电池电量
            batteryInfo.level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            
            // 电池状态
            int status = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS);
            switch (status) {
                case BatteryManager.BATTERY_STATUS_CHARGING:
                    batteryInfo.status = "充电中";
                    break;
                case BatteryManager.BATTERY_STATUS_DISCHARGING:
                    batteryInfo.status = "放电中";
                    break;
                case BatteryManager.BATTERY_STATUS_FULL:
                    batteryInfo.status = "已充满";
                    break;
                case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                    batteryInfo.status = "未充电";
                    break;
                default:
                    batteryInfo.status = "未知";
                    break;
            }
            
            // 电池健康状态
            batteryInfo.health = getBatteryHealth();
            
            // 电池温度 (摄氏度)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                int temperature = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);
                batteryInfo.temperature = temperature / 10.0f; // 转换为摄氏度
            }
        }
        
        return batteryInfo;
    }
    
    /**
     * 获取权限状态
     */
    public PermissionStatus getPermissionStatus() {
        PermissionStatus permissionStatus = new PermissionStatus();
        
        // 检查关键权限
        permissionStatus.cameraPermission = checkPermission(Manifest.permission.CAMERA);
        permissionStatus.microphonePermission = checkPermission(Manifest.permission.RECORD_AUDIO);
        permissionStatus.locationPermission = checkPermission(Manifest.permission.ACCESS_FINE_LOCATION);
        permissionStatus.storagePermission = checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissionStatus.phonePermission = checkPermission(Manifest.permission.READ_PHONE_STATE);
        permissionStatus.contactsPermission = checkPermission(Manifest.permission.READ_CONTACTS);
        permissionStatus.smsPermission = checkPermission(Manifest.permission.READ_SMS);
        
        // 检查特殊权限
        permissionStatus.overlayPermission = Settings.canDrawOverlays(context);
        permissionStatus.deviceAdminPermission = isDeviceAdmin();
        permissionStatus.accessibilityPermission = isAccessibilityServiceEnabled();
        
        return permissionStatus;
    }
    
    // 辅助方法
    private boolean checkPermission(String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }
    
    private String getIPAddress() {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<java.net.InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (java.net.InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;
                        if (isIPv4) {
                            return sAddr;
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "0.0.0.0";
    }
    
    private String getBatteryHealth() {
        // 这里可以通过BroadcastReceiver获取更详细的电池健康信息
        return "良好";
    }
    
    private boolean isDeviceAdmin() {
        // 检查是否为设备管理员
        return false; // 需要具体实现
    }
    
    private boolean isAccessibilityServiceEnabled() {
        // 检查无障碍服务是否启用
        return false; // 需要具体实现
    }
    
    // 数据模型类
    public static class DeviceInfo {
        public String manufacturer;
        public String model;
        public String brand;
        public String androidVersion;
        public int apiLevel;
        public String buildNumber;
        public String deviceId;
        public String serialNumber;
    }
    
    public static class NetworkInfo {
        public boolean isConnected;
        public String networkType;
        public String subType;
        public String wifiSSID;
        public int wifiStrength;
        public int linkSpeed;
        public String operatorName;
        public int mobileNetworkType;
        public String ipAddress;
    }
    
    public static class BatteryInfo {
        public int level;
        public String status;
        public String health;
        public float temperature;
    }
    
    public static class PermissionStatus {
        public boolean cameraPermission;
        public boolean microphonePermission;
        public boolean locationPermission;
        public boolean storagePermission;
        public boolean phonePermission;
        public boolean contactsPermission;
        public boolean smsPermission;
        public boolean overlayPermission;
        public boolean deviceAdminPermission;
        public boolean accessibilityPermission;
    }
}
