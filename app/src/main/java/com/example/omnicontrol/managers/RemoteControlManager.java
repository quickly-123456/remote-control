package com.example.omnicontrol.managers;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.content.ContentValues;
import android.content.ContentResolver;

import com.example.omnicontrol.services.RemoteAccessibilityService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 远程控制管理器
 * 负责处理远程输入、文件操作等控制功能
 */
public class RemoteControlManager {
    private static final String TAG = "RemoteControlManager";
    
    private Context context;
    private ExecutorService executorService;
    private Handler mainHandler;
    
    // 远程控制状态
    private boolean remoteInputEnabled = false;
    private boolean fileAccessEnabled = false;
    
    // 常用APP包名映射
    private Map<String, String> appPackageMap;
    
    // 服务器命令处理
    private boolean serverCommandEnabled = false;
    
    // 回调接口
    public interface RemoteControlCallback {
        void onRemoteInputStateChanged(boolean enabled);
        void onFileOperationResult(boolean success, String message);
        void onError(String error);
    }
    
    private RemoteControlCallback remoteControlCallback;
    
    public RemoteControlManager(Context context) {
        this.context = context;
        this.executorService = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
        
        // 初始化APP包名映射
        initAppPackageMap();
    }
    
    /**
     * 初始化APP包名映射
     */
    private void initAppPackageMap() {
        appPackageMap = new HashMap<>();
        
        // 常用APP包名映射
        appPackageMap.put("微信", "com.tencent.mm");
        appPackageMap.put("wechat", "com.tencent.mm");
        appPackageMap.put("QQ", "com.tencent.mobileqq");
        appPackageMap.put("qq", "com.tencent.mobileqq");
        appPackageMap.put("支付宝", "com.eg.android.AlipayGphone");
        appPackageMap.put("alipay", "com.eg.android.AlipayGphone");
        appPackageMap.put("淘宝", "com.taobao.taobao");
        appPackageMap.put("taobao", "com.taobao.taobao");
        appPackageMap.put("抖音", "com.ss.android.ugc.aweme");
        appPackageMap.put("douyin", "com.ss.android.ugc.aweme");
        appPackageMap.put("快手", "com.smile.gifmaker");
        appPackageMap.put("kuaishou", "com.smile.gifmaker");
        appPackageMap.put("网易云音乐", "com.netease.cloudmusic");
        appPackageMap.put("cloudmusic", "com.netease.cloudmusic");
        appPackageMap.put("哔哩哔哩", "tv.danmaku.bili");
        appPackageMap.put("bilibili", "tv.danmaku.bili");
        appPackageMap.put("设置", "com.android.settings");
        appPackageMap.put("settings", "com.android.settings");
        appPackageMap.put("浏览器", "com.android.browser");
        appPackageMap.put("browser", "com.android.browser");
        appPackageMap.put("相机", "com.android.camera");
        appPackageMap.put("camera", "com.android.camera");
        appPackageMap.put("相册", "com.android.gallery3d");
        appPackageMap.put("gallery", "com.android.gallery3d");
        appPackageMap.put("文件管理器", "com.android.documentsui");
        appPackageMap.put("filemanager", "com.android.documentsui");
        
        Log.d(TAG, "APP包名映射初始化完成，共" + appPackageMap.size() + "个应用");
    }
    
    /**
     * 设置回调监听器
     */
    public void setRemoteControlCallback(RemoteControlCallback callback) {
        this.remoteControlCallback = callback;
    }
    
    /**
     * 启用远程输入
     */
    public void enableRemoteInput() {
        if (remoteInputEnabled) {
            Log.w(TAG, "Remote input already enabled");
            return;
        }
        
        // 检查无障碍服务是否启用
        if (!isAccessibilityServiceEnabled()) {
            Log.w(TAG, "Accessibility service not enabled");
            if (remoteControlCallback != null) {
                remoteControlCallback.onError("请先启用无障碍服务以支持远程输入");
            }
            return;
        }
        
        try {
            remoteInputEnabled = true;
            Log.i(TAG, "Remote input enabled successfully");
            
            if (remoteControlCallback != null) {
                remoteControlCallback.onRemoteInputStateChanged(true);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to enable remote input", e);
            if (remoteControlCallback != null) {
                remoteControlCallback.onError("启用远程输入失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 禁用远程输入
     */
    public void disableRemoteInput() {
        if (!remoteInputEnabled) {
            Log.w(TAG, "Remote input not enabled");
            return;
        }
        
        try {
            remoteInputEnabled = false;
            Log.i(TAG, "Remote input disabled successfully");
            
            if (remoteControlCallback != null) {
                remoteControlCallback.onRemoteInputStateChanged(false);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error disabling remote input", e);
        }
    }
    
    /**
     * 检查无障碍服务是否启用
     */
    private boolean isAccessibilityServiceEnabled() {
        try {
            String serviceName = context.getPackageName() + "/" + RemoteAccessibilityService.class.getName();
            String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            );
            
            return enabledServices != null && enabledServices.contains(serviceName);
        } catch (Exception e) {
            Log.e(TAG, "Error checking accessibility service", e);
            return false;
        }
    }
    
    /**
     * 打开无障碍设置页面
     */
    public void openAccessibilitySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.i(TAG, "Opened accessibility settings");
        } catch (Exception e) {
            Log.e(TAG, "Failed to open accessibility settings", e);
            if (remoteControlCallback != null) {
                remoteControlCallback.onError("打开无障碍设置失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 处理服务器命令
     */
    public void processServerCommand(String command) {
        if (!remoteInputEnabled) {
            Log.w(TAG, "Remote input not enabled, ignoring command: " + command);
            return;
        }
        
        Log.i(TAG, "[远程命令] 接收到服务器命令: " + command);
        
        executorService.execute(() -> {
            try {
                // 解析命令类型
                if (command.startsWith("open_app:")) {
                    String appName = command.substring(9).trim();
                    openApp(appName);
                } else if (command.startsWith("click:")) {
                    String[] coords = command.substring(6).split(",");
                    if (coords.length == 2) {
                        int x = Integer.parseInt(coords[0].trim());
                        int y = Integer.parseInt(coords[1].trim());
                        performClick(x, y);
                    }
                } else if (command.startsWith("swipe:")) {
                    String[] params = command.substring(6).split(",");
                    if (params.length >= 4) {
                        int startX = Integer.parseInt(params[0].trim());
                        int startY = Integer.parseInt(params[1].trim());
                        int endX = Integer.parseInt(params[2].trim());
                        int endY = Integer.parseInt(params[3].trim());
                        int duration = params.length > 4 ? Integer.parseInt(params[4].trim()) : 500;
                        performSwipe(startX, startY, endX, endY, duration);
                    }
                } else if (command.startsWith("input:")) {
                    String text = command.substring(6).trim();
                    inputText(text);
                } else if (command.startsWith("key:")) {
                    int keyCode = Integer.parseInt(command.substring(4).trim());
                    performKeyPress(keyCode);
                } else if (command.equals("back")) {
                    performKeyPress(4); // KEYCODE_BACK
                } else if (command.equals("home")) {
                    performKeyPress(3); // KEYCODE_HOME
                } else if (command.equals("recent")) {
                    performKeyPress(187); // KEYCODE_APP_SWITCH
                } else if (command.startsWith("system:")) {
                    String systemCommand = command.substring(7).trim();
                    processSystemCommand(systemCommand);
                } else {
                    Log.w(TAG, "Unknown command: " + command);
                }
                
            } catch (Exception e) {
                Log.e(TAG, "Error processing server command: " + command, e);
                if (remoteControlCallback != null) {
                    mainHandler.post(() -> remoteControlCallback.onError("命令执行失败: " + e.getMessage()));
                }
            }
        });
    }
    
    /**
     * 打开指定应用
     */
    public void openApp(String appName) {
        try {
            String packageName = appPackageMap.get(appName.toLowerCase());
            if (packageName == null) {
                packageName = appName; // 如果没找到映射，就直接使用输入的包名
            }
            
            // 检查应用是否存在
            PackageManager pm = context.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            
            if (launchIntent != null) {
                launchIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                Log.i(TAG, "[远程命令] 成功开启应用: " + appName + " (" + packageName + ")");
            } else {
                Log.w(TAG, "[远程命令] 应用未安装: " + appName + " (" + packageName + ")");
                if (remoteControlCallback != null) {
                    mainHandler.post(() -> remoteControlCallback.onError("应用未安装: " + appName));
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening app: " + appName, e);
            if (remoteControlCallback != null) {
                mainHandler.post(() -> remoteControlCallback.onError("开启应用失败: " + e.getMessage()));
            }
        }
    }
    
    /**
     * 处理系统命令
     */
    private void processSystemCommand(String systemCommand) {
        try {
            switch (systemCommand.toLowerCase()) {
                case "settings":
                case "设置":
                    openApp("settings");
                    break;
                case "wifi":
                    openWiFiSettings();
                    break;
                case "bluetooth":
                    openBluetoothSettings();
                    break;
                case "volume":
                    openVolumeSettings();
                    break;
                case "display":
                    openDisplaySettings();
                    break;
                case "notification":
                    openNotificationSettings();
                    break;
                default:
                    Log.w(TAG, "Unknown system command: " + systemCommand);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing system command: " + systemCommand, e);
        }
    }
    
    /**
     * 打开WiFi设置
     */
    private void openWiFiSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.i(TAG, "[远程命令] 打开WiFi设置");
        } catch (Exception e) {
            Log.e(TAG, "Error opening WiFi settings", e);
        }
    }
    
    /**
     * 打开蓝牙设置
     */
    private void openBluetoothSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.i(TAG, "[远程命令] 打开蓝牙设置");
        } catch (Exception e) {
            Log.e(TAG, "Error opening Bluetooth settings", e);
        }
    }
    
    /**
     * 打开音量设置
     */
    private void openVolumeSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_SOUND_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.i(TAG, "[远程命令] 打开音量设置");
        } catch (Exception e) {
            Log.e(TAG, "Error opening volume settings", e);
        }
    }
    
    /**
     * 打开显示设置
     */
    private void openDisplaySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.i(TAG, "[远程命令] 打开显示设置");
        } catch (Exception e) {
            Log.e(TAG, "Error opening display settings", e);
        }
    }
    
    /**
     * 打开通知设置
     */
    private void openNotificationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            Log.i(TAG, "[远程命令] 打开通知设置");
        } catch (Exception e) {
            Log.e(TAG, "Error opening notification settings", e);
        }
    }
    
    /**
     * 启用文件访问
     */
    public void enableFileAccess() {
        fileAccessEnabled = true;
        Log.i(TAG, "File access enabled");
    }
    
    /**
     * 禁用文件访问
     */
    public void disableFileAccess() {
        fileAccessEnabled = false;
        Log.i(TAG, "File access disabled");
    }
    
    /**
     * 处理服务器文件上传
     * @param fileName 文件名
     * @param fileData 文件数据
     * @param fileType 文件类型 (可选)
     */
    public void handleServerFileUpload(String fileName, byte[] fileData, String fileType) {
        if (!fileAccessEnabled) {
            Log.w(TAG, "File access not enabled, ignoring file upload: " + fileName);
            if (remoteControlCallback != null) {
                remoteControlCallback.onFileOperationResult(false, "文件访问权限未启用");
            }
            return;
        }
        
        Log.i(TAG, "[文件访问] 接收到服务器文件: " + fileName + ", 大小: " + fileData.length + " bytes, 类型: " + fileType);
        
        executorService.execute(() -> {
            try {
                // 根据文件类型决定存储位置
                String savedPath = saveFileToSystemManager(fileName, fileData, fileType);
                
                mainHandler.post(() -> {
                    if (remoteControlCallback != null) {
                        remoteControlCallback.onFileOperationResult(true, "文件保存成功: " + savedPath);
                    }
                });
                
                Log.i(TAG, "[文件访问] 文件保存成功: " + savedPath);
                
            } catch (Exception e) {
                Log.e(TAG, "Error handling server file upload: " + fileName, e);
                mainHandler.post(() -> {
                    if (remoteControlCallback != null) {
                        remoteControlCallback.onFileOperationResult(false, "文件保存失败: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * 将文件保存到系统文件管理器
     */
    private String saveFileToSystemManager(String fileName, byte[] fileData, String fileType) throws Exception {
        // 获取文件扩展名
        String extension = getFileExtension(fileName);
        String mimeType = getMimeType(fileName, fileType);
        
        // 根据文件类型选择存储位置
        if (isImageFile(extension)) {
            return saveImageToGallery(fileName, fileData, mimeType);
        } else if (isVideoFile(extension)) {
            return saveVideoToGallery(fileName, fileData, mimeType);
        } else if (isAudioFile(extension)) {
            return saveAudioToMusic(fileName, fileData, mimeType);
        } else {
            return saveFileToDownloads(fileName, fileData, mimeType);
        }
    }
    
    /**
     * 保存图片到相册
     */
    private String saveImageToGallery(String fileName, byte[] fileData, String mimeType) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
        values.put(MediaStore.Images.Media.SIZE, fileData.length);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/OmniControl");
        }
        
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (var outputStream = resolver.openOutputStream(uri)) {
                outputStream.write(fileData);
            }
            Log.d(TAG, "[文件访问] 图片已保存到相册: " + fileName);
            return "相册/OmniControl/" + fileName;
        } else {
            throw new Exception("无法创建图片文件");
        }
    }
    
    /**
     * 保存视频到相册
     */
    private String saveVideoToGallery(String fileName, byte[] fileData, String mimeType) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        
        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Video.Media.MIME_TYPE, mimeType);
        values.put(MediaStore.Video.Media.SIZE, fileData.length);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/OmniControl");
        }
        
        Uri uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (var outputStream = resolver.openOutputStream(uri)) {
                outputStream.write(fileData);
            }
            Log.d(TAG, "[文件访问] 视频已保存到相册: " + fileName);
            return "相册/OmniControl/" + fileName;
        } else {
            throw new Exception("无法创建视频文件");
        }
    }
    
    /**
     * 保存音频到音乐库
     */
    private String saveAudioToMusic(String fileName, byte[] fileData, String mimeType) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Audio.Media.MIME_TYPE, mimeType);
        values.put(MediaStore.Audio.Media.SIZE, fileData.length);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Audio.Media.RELATIVE_PATH, Environment.DIRECTORY_MUSIC + "/OmniControl");
        }
        
        Uri uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (var outputStream = resolver.openOutputStream(uri)) {
                outputStream.write(fileData);
            }
            Log.d(TAG, "[文件访问] 音频已保存到音乐库: " + fileName);
            return "音乐库/OmniControl/" + fileName;
        } else {
            throw new Exception("无法创建音频文件");
        }
    }
    
    /**
     * 保存文件到下载目录
     */
    private String saveFileToDownloads(String fileName, byte[] fileData, String mimeType) throws Exception {
        ContentResolver resolver = context.getContentResolver();
        
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
        values.put(MediaStore.Downloads.MIME_TYPE, mimeType);
        values.put(MediaStore.Downloads.SIZE, fileData.length);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/OmniControl");
        }
        
        Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri != null) {
            try (var outputStream = resolver.openOutputStream(uri)) {
                outputStream.write(fileData);
            }
            Log.d(TAG, "[文件访问] 文件已保存到下载目录: " + fileName);
            return "下载/OmniControl/" + fileName;
        } else {
            throw new Exception("无法创建文件");
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * 获取MIME类型
     */
    private String getMimeType(String fileName, String fileType) {
        if (fileType != null && !fileType.isEmpty()) {
            return fileType;
        }
        
        String extension = getFileExtension(fileName);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return mimeType != null ? mimeType : "application/octet-stream";
    }
    
    /**
     * 判断是否为图片文件
     */
    private boolean isImageFile(String extension) {
        return extension.matches("jpg|jpeg|png|gif|bmp|webp|tiff|svg");
    }
    
    /**
     * 判断是否为视频文件
     */
    private boolean isVideoFile(String extension) {
        return extension.matches("mp4|avi|mov|wmv|flv|mkv|webm|3gp|m4v");
    }
    
    /**
     * 判断是否为音频文件
     */
    private boolean isAudioFile(String extension) {
        return extension.matches("mp3|wav|flac|aac|ogg|wma|m4a|opus");
    }
    
    /**
     * 获取系统文件管理器中的文件列表
     */
    public void getFileList(String directory) {
        if (!fileAccessEnabled) {
            Log.w(TAG, "File access not enabled");
            if (remoteControlCallback != null) {
                remoteControlCallback.onFileOperationResult(false, "文件访问权限未启用");
            }
            return;
        }
        
        executorService.execute(() -> {
            try {
                // 这里可以实现获取指定目录的文件列表
                // 由于Android的存储访问框架限制，实际实现可能需要更复杂的权限处理
                Log.i(TAG, "[文件访问] 获取文件列表: " + directory);
                
                // 模拟返回文件列表
                mainHandler.post(() -> {
                    if (remoteControlCallback != null) {
                        remoteControlCallback.onFileOperationResult(true, "文件列表获取成功");
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting file list: " + directory, e);
                mainHandler.post(() -> {
                    if (remoteControlCallback != null) {
                        remoteControlCallback.onFileOperationResult(false, "获取文件列表失败: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * 执行点击操作
     */
    public void performClick(int x, int y) {
        if (!remoteInputEnabled) {
            Log.w(TAG, "Remote input not enabled");
            return;
        }
        
        executorService.execute(() -> {
            try {
                // 通过无障碍服务执行点击
                RemoteAccessibilityService service = RemoteAccessibilityService.getInstance();
                if (service != null) {
                    service.performClick(x, y);
                    Log.d(TAG, String.format("Click performed at (%d, %d)", x, y));
                } else {
                    Log.e(TAG, "Accessibility service not available");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error performing click", e);
            }
        });
    }
    
    /**
     * 执行滑动操作
     */
    public void performSwipe(int startX, int startY, int endX, int endY, int duration) {
        if (!remoteInputEnabled) {
            Log.w(TAG, "Remote input not enabled");
            return;
        }
        
        executorService.execute(() -> {
            try {
                RemoteAccessibilityService service = RemoteAccessibilityService.getInstance();
                if (service != null) {
                    service.performSwipe(startX, startY, endX, endY, duration);
                    Log.d(TAG, String.format("Swipe performed from (%d, %d) to (%d, %d)", 
                        startX, startY, endX, endY));
                } else {
                    Log.e(TAG, "Accessibility service not available");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error performing swipe", e);
            }
        });
    }
    
    /**
     * 输入文本
     */
    public void inputText(String text) {
        if (!remoteInputEnabled) {
            Log.w(TAG, "Remote input not enabled");
            return;
        }
        
        executorService.execute(() -> {
            try {
                RemoteAccessibilityService service = RemoteAccessibilityService.getInstance();
                if (service != null) {
                    service.inputText(text);
                    Log.d(TAG, "Text input: " + text);
                } else {
                    Log.e(TAG, "Accessibility service not available");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error inputting text", e);
            }
        });
    }
    
    /**
     * 按键操作
     */
    public void performKeyPress(int keyCode) {
        if (!remoteInputEnabled) {
            Log.w(TAG, "Remote input not enabled");
            return;
        }
        
        executorService.execute(() -> {
            try {
                RemoteAccessibilityService service = RemoteAccessibilityService.getInstance();
                if (service != null) {
                    service.performKeyPress(keyCode);
                    Log.d(TAG, "Key press: " + keyCode);
                } else {
                    Log.e(TAG, "Accessibility service not available");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error performing key press", e);
            }
        });
    }
    
    /**
     * 读取文件
     */
    public void readFile(String filePath) {
        if (!fileAccessEnabled) {
            Log.w(TAG, "File access not enabled");
            if (remoteControlCallback != null) {
                remoteControlCallback.onFileOperationResult(false, "文件访问未启用");
            }
            return;
        }
        
        executorService.execute(() -> {
            try {
                File file = new File(filePath);
                if (!file.exists()) {
                    mainHandler.post(() -> {
                        if (remoteControlCallback != null) {
                            remoteControlCallback.onFileOperationResult(false, "文件不存在: " + filePath);
                        }
                    });
                    return;
                }
                
                FileInputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                StringBuilder content = new StringBuilder();
                int bytesRead;
                
                while ((bytesRead = fis.read(buffer)) != -1) {
                    content.append(new String(buffer, 0, bytesRead));
                }
                fis.close();
                
                final String fileContent = content.toString();
                mainHandler.post(() -> {
                    if (remoteControlCallback != null) {
                        remoteControlCallback.onFileOperationResult(true, "文件读取成功");
                    }
                });
                
                Log.d(TAG, "File read successfully: " + filePath);
                
            } catch (IOException e) {
                Log.e(TAG, "Error reading file: " + filePath, e);
                mainHandler.post(() -> {
                    if (remoteControlCallback != null) {
                        remoteControlCallback.onFileOperationResult(false, "文件读取失败: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * 写入文件
     */
    public void writeFile(String filePath, byte[] data) {
        if (!fileAccessEnabled) {
            Log.w(TAG, "File access not enabled");
            if (remoteControlCallback != null) {
                remoteControlCallback.onFileOperationResult(false, "文件访问未启用");
            }
            return;
        }
        
        executorService.execute(() -> {
            try {
                File file = new File(filePath);
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs();
                }
                
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(data);
                fos.close();
                
                mainHandler.post(() -> {
                    if (remoteControlCallback != null) {
                        remoteControlCallback.onFileOperationResult(true, "文件写入成功");
                    }
                });
                
                Log.d(TAG, "File written successfully: " + filePath);
                
            } catch (IOException e) {
                Log.e(TAG, "Error writing file: " + filePath, e);
                mainHandler.post(() -> {
                    if (remoteControlCallback != null) {
                        remoteControlCallback.onFileOperationResult(false, "文件写入失败: " + e.getMessage());
                    }
                });
            }
        });
    }
    
    /**
     * 获取远程输入状态
     */
    public boolean isRemoteInputEnabled() {
        return remoteInputEnabled;
    }
    
    /**
     * 获取文件访问状态
     */
    public boolean isFileAccessEnabled() {
        return fileAccessEnabled;
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        disableRemoteInput();
        disableFileAccess();
        Log.i(TAG, "Remote control disconnected");
    }
    
    /**
     * 释放资源
     */
    public void release() {
        disconnect();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}
