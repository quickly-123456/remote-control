package com.example.omnicontrol.managers;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicLong;

// WebSocket实时推送相关导入
import com.example.omnicontrol.utils.WebSocketManager;
import com.example.omnicontrol.utils.RDTDefine;



/**
 * 屏幕捕获管理器 - 定时截图+WebP上传方案
 * 功能：25fps定时截图，WebP压缩，实时上传服务器
 */
public class ScreenCaptureManager {
    private static final String TAG = "ScreenCaptureManager";
    
    // 定时器配置
    private static final int TARGET_FPS = 25;
    private static final long CAPTURE_INTERVAL_MS = 1000 / TARGET_FPS; // 40ms
    
    // WebP压缩配置
    private static final int WEBP_QUALITY = 80; // WebP质量 (0-100)
    
    // 缩放配置
    private static final float SCALE_RATIO = 0.5f; // 缩放比例 (0.1-1.0)
    private static final boolean ENABLE_SCALING = true; // 是否启用缩放
    
    // Log输出配置
    private static final boolean ENABLE_BASE64_LOG = false; // 禁用Base64图片数据日志输出
    
    private Context context;
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    
    // 屏幕参数
    private int screenWidth;
    private int screenHeight;
    private int screenDpi;
    
    // 线程管理
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private Handler mainHandler;
    
    // 定时器
    private Handler captureHandler;
    private Runnable captureRunnable;
    
    // Base64编码器
    private java.util.Base64.Encoder base64Encoder;
    
    // 统计数据
    private AtomicLong frameCount = new AtomicLong(0);
    private AtomicLong totalDataSize = new AtomicLong(0);
    private long captureStartTime;
    
    // 状态控制
    private volatile boolean isCapturing = false;
    
    private volatile boolean enableWebSocketPush = true; // 是否启用WebSocket推送
    
    // 统计报告
    private Handler statsHandler;
    private Runnable statsRunnable;
    private static final long STATS_INTERVAL = 3000; // 3秒统计一次
    
    // 数据回调接口（保持兼容性）
    public interface ScreenDataCallback {
        void onScreenData(byte[] data);
        void onError(String error);
    }
    
    private ScreenDataCallback screenDataCallback;
    
    // MediaProjection 回调（Android 14+ 必需）
    private MediaProjection.Callback mediaProjectionCallback = new MediaProjection.Callback() {
        @Override
        public void onStop() {
            Log.i(TAG, "📱 MediaProjection已停止 - 系统已关闭屏幕共享");
            if (isCapturing) {
                // 自动停止捕获
                stopCapture();
            }
            
            // 通知权限管理器关闭屏幕共享权限
            try {
                // 在主线程中更新权限状态
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    try {
                        // 获取当前用户信息
                        com.example.omnicontrol.utils.UserManager userManager = 
                            new com.example.omnicontrol.utils.UserManager(context);
                        String phone = userManager.getCurrentUsername();
                        
                        if (phone != null && !phone.isEmpty()) {
                            // 通过PermissionManager更新屏幕权限为false
                            com.example.omnicontrol.utils.PermissionManager permissionManager = 
                                com.example.omnicontrol.utils.PermissionManager.getInstance(context);
                            if (permissionManager != null) {
                                permissionManager.updatePermission(phone, "screen", false);
                                Log.i(TAG, "✅ 已自动关闭屏幕共享权限开关");
                            }
                        } else {
                            Log.w(TAG, "无法获取用户信息，跳过权限更新");
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "关闭权限开关失败: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "更新权限状态失败: " + e.getMessage());
            }
        }
        
        @Override
        public void onCapturedContentResize(int width, int height) {
            Log.i(TAG, String.format("📱 屏幕尺寸变化: %dx%d", width, height));
            // 可以在此处处理屏幕尺寸变化
        }
        
        @Override
        public void onCapturedContentVisibilityChanged(boolean isVisible) {
            Log.i(TAG, "📱 屏幕可见性变化: " + (isVisible ? "可见" : "不可见"));
        }
    };
    
    public ScreenCaptureManager(Context context) {
        this.context = context;
        this.mediaProjectionManager = (MediaProjectionManager) 
            context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        
        // 初始化组件
        initScreenParams();
        initHandlers();
        initBase64Encoder();
        initCaptureTimer();
        initStatsReporting();
        initWebSocket(); // 初始化WebSocket管理器
        
        Log.i(TAG, "📷 ScreenCaptureManager初始化完成 - 定时截图+WebSocket实时推送模式");
    }
    
    /**
     * 初始化屏幕参数
     */
    private void initScreenParams() {
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getRealMetrics(metrics);
        
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDpi = metrics.densityDpi;
        
        Log.d(TAG, String.format("📱 屏幕参数: %dx%d, DPI=%d", screenWidth, screenHeight, screenDpi));
    }
    
    /**
     * 初始化Handler
     */
    private void initHandlers() {
        // 后台线程用于截图处理
        backgroundThread = new HandlerThread("ScreenCapture-Background");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
        
        // 主线程Handler
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 定时器Handler
        captureHandler = new Handler(Looper.getMainLooper());
        
        Log.d(TAG, "⚙️ Handler初始化完成");
    }
    
    /**
     * 初始化Base64编码器
     */
    private void initBase64Encoder() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            base64Encoder = java.util.Base64.getEncoder();
        }
        
        Log.d(TAG, "📝 Base64编码器初始化完成");
    }
    
    /**
     * 初始化定时截图器
     */
    private void initCaptureTimer() {
        captureRunnable = new Runnable() {
            @Override
            public void run() {
                if (isCapturing) {
                    captureScreenshot();
                    // 安排下一次截图
                    captureHandler.postDelayed(this, CAPTURE_INTERVAL_MS);
                }
            }
        };
        
        Log.d(TAG, String.format("⏰ 定时器初始化完成 - %dFPS (%dms间隔)", TARGET_FPS, CAPTURE_INTERVAL_MS));
    }
    
    /**
     * 初始化统计报告
     */
    private void initStatsReporting() {
        if (statsHandler == null) {
            statsHandler = new Handler(Looper.getMainLooper());
        }
        
        statsRunnable = new Runnable() {
            @Override
            public void run() {
                if (isCapturing) {
                    logCaptureStats();
                    // 继续下一次统计
                    if (statsHandler != null) {
                        statsHandler.postDelayed(this, STATS_INTERVAL);
                    }
                }
            }
        };
        
        Log.d(TAG, "📅 统计报告初始化完成");
    }
    
    /**
     * 初始化WebSocket管理器
     */
    private void initWebSocket() {
        WebSocketManager webSocketManager = WebSocketManager.instance();
        
        // 设置WebSocket连接状态监听器
        webSocketManager.setConnectionStateListener(new WebSocketManager.ConnectionStateListener() {
            @Override
            public void onConnectionStateChanged(int state) {
                Log.i(TAG, "🌐 WebSocket状态变化: " + RDTDefine.getConnectionStateDescription(state));
                
                // 如果正在截图且WebSocket连接成功，重置统计数据
                if (state == RDTDefine.ConnectionState.CONNECTED && isCapturing) {
                    webSocketManager.resetStats();
                }
            }
            
            @Override
            public void onScreenDataSent(long frameNumber, int dataSize) {
                // WebSocket发送成功的回调，可以在这里添加额外的统计逻辑
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "❌ WebSocket错误: " + error);
                if (screenDataCallback != null) {
                    screenDataCallback.onError("WebSocket错误: " + error);
                }
            }
        });
        
        Log.d(TAG, "🌐 WebSocket管理器初始化完成");
    }
    
    /**
     * 启用/禁用WebSocket推送
     */
    public void setWebSocketPushEnabled(boolean enabled) {
        this.enableWebSocketPush = enabled;
        Log.i(TAG, "🌐 WebSocket推送: " + (enabled ? "已启用" : "已禁用"));
    }
    
    /**
     * 获取WebSocket连接状态
     */
    public int getWebSocketState() {
        WebSocketManager webSocketManager = WebSocketManager.instance();
        return webSocketManager != null ? webSocketManager.getConnectionState() : 
               RDTDefine.ConnectionState.DISCONNECTED;
    }
    
    /**
     * 获取WebSocket统计信息
     */
    public String getWebSocketStats() {
        WebSocketManager webSocketManager = WebSocketManager.instance();
        if (webSocketManager == null) {
            return "WebSocket未初始化";
        }
        
        long sentFrames = webSocketManager.getSentFrames();
        long sentBytes = webSocketManager.getSentBytes();
        int state = webSocketManager.getConnectionState();
        
        return String.format("状态: %s | 已发送: %d帧 | 数据量: %.1fMB",
                RDTDefine.getConnectionStateDescription(state),
                sentFrames,
                sentBytes / 1024.0f / 1024.0f);
    }
    
    /**
     * 截取屏幕截图
     */
    private void captureScreenshot() {
        if (!isCapturing || imageReader == null) {
            return;
        }
        
        Image image = null;
        try {
            // 使用acquireNextImage()而不是acquireLatestImage()以避免缓冲区问题
            image = imageReader.acquireNextImage();
            if (image != null) {
                // 在后台线程中处理，确保快速释放当前线程
                final Image finalImage = image;
                backgroundHandler.post(() -> processScreenshot(finalImage));
                image = null; // 防止在catch中重复关闭
            } else {
                // 没有可用图像，可能是缓冲区为空
                Log.d(TAG, "没有可用的截图图像");
            }
        } catch (IllegalStateException e) {
            // 处理缓冲区相关问题
            String errorMsg = e.getMessage();
            if (errorMsg != null && (errorMsg.contains("maxImages") || errorMsg.contains("dequeueBuffer"))) {
                Log.w(TAG, "ImageReader缓冲区问题，尝试清理: " + errorMsg);
                // 安全清理缓冲区
                clearImageReaderBuffer();
            } else {
                Log.e(TAG, "截图获取失败: " + errorMsg, e);
            }
        } catch (UnsupportedOperationException e) {
            Log.w(TAG, "ImageReader操作不支持，跳过此次截图: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "截图获取失败: " + e.getMessage(), e);
        } finally {
            // 确保在异常情况下也能释放资源
            if (image != null) {
                try {
                    image.close();
                } catch (Exception e) {
                    Log.w(TAG, "释放图像资源失败: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * 安全清理ImageReader缓冲区
     */
    private void clearImageReaderBuffer() {
        if (imageReader == null) return;
        
        try {
            int clearedCount = 0;
            // 清理所有待处理的图像
            while (clearedCount < 5) { // 最多清理5个，防止无限循环
                Image oldImage = null;
                try {
                    oldImage = imageReader.acquireNextImage();
                    if (oldImage != null) {
                        oldImage.close();
                        clearedCount++;
                    } else {
                        break; // 没有更多图像
                    }
                } catch (Exception e) {
                    if (oldImage != null) {
                        oldImage.close();
                    }
                    break; // 清理完成或出现异常
                }
            }
            if (clearedCount > 0) {
                Log.i(TAG, "已清理ImageReader缓冲区，释放了" + clearedCount + "个图像");
            }
        } catch (Exception e) {
            Log.w(TAG, "清理ImageReader缓冲区失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理截图数据
     */
    private void processScreenshot(Image image) {
        try {
            long startTime = System.currentTimeMillis();
            
            // 转Bitmap
            Bitmap originalBitmap = imageToBitmap(image);
            image.close();
            
            if (originalBitmap == null) {
                Log.w(TAG, "截图转Bitmap失败");
                return;
            }
            
            // 记录原始尺寸
            int originalWidth = originalBitmap.getWidth();
            int originalHeight = originalBitmap.getHeight();
            
            // 缩放处理
            Bitmap processedBitmap = originalBitmap;
            int finalWidth = originalWidth;
            int finalHeight = originalHeight;
            
            if (ENABLE_SCALING && SCALE_RATIO < 1.0f) {
                finalWidth = (int) (originalWidth * SCALE_RATIO);
                finalHeight = (int) (originalHeight * SCALE_RATIO);
                
                // 使用双线性插值缩放
                processedBitmap = Bitmap.createScaledBitmap(originalBitmap, finalWidth, finalHeight, true);
                originalBitmap.recycle(); // 回收原始bitmap
                
                Log.v(TAG, String.format("🔄 缩放处理: %dx%d -> %dx%d (%.1f%%)", 
                    originalWidth, originalHeight, finalWidth, finalHeight, SCALE_RATIO * 100));
            }
            
            // 压缩为WebP
            byte[] webpData = compressToWebP(processedBitmap);
            processedBitmap.recycle();
            
            if (webpData == null || webpData.length == 0) {
                Log.w(TAG, "WebP压缩失败");
                return;
            }
            
            long processingTime = System.currentTimeMillis() - startTime;
            long currentFrame = frameCount.incrementAndGet();
            totalDataSize.addAndGet(webpData.length);
            
            // 详细帧处理日志
            String scaleInfo = ENABLE_SCALING && SCALE_RATIO < 1.0f ? 
                String.format(" [缩放: %dx%d->%dx%d]", originalWidth, originalHeight, finalWidth, finalHeight) : "";
            
            Log.i(TAG, String.format(
                "📷 Frame #%d: %dx%d -> WebP %.1fKB%s (处理耗时: %dms)", 
                currentFrame, finalWidth, finalHeight, 
                webpData.length / 1024.0f, scaleInfo, processingTime
            ));
            
            // WebSocket实时推送
            WebSocketManager webSocketManager = WebSocketManager.instance();
            if (enableWebSocketPush && webSocketManager != null && webSocketManager.isConnected()) {
                webSocketManager.sendScreenData(webpData, finalWidth, finalHeight);
                Log.d(TAG, String.format(
                    "🌐 WebSocket发送: Frame #%d | %.1fKB (%dx%d) -> %s", 
                    currentFrame, webpData.length / 1024.0f, finalWidth, finalHeight, RDTDefine.WS_SERVER_URL
                ));
            }
            
            // 回调数据（保持兼容性）
            if (screenDataCallback != null) {
                screenDataCallback.onScreenData(webpData);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "处理截图数据失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将Image转换为Bitmap
     */
    private Bitmap imageToBitmap(Image image) {
        try {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            int pixelStride = planes[0].getPixelStride();
            int rowStride = planes[0].getRowStride();
            int rowPadding = rowStride - pixelStride * screenWidth;
            
            Bitmap bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride, 
                screenHeight, 
                Bitmap.Config.ARGB_8888
            );
            bitmap.copyPixelsFromBuffer(buffer);
            
            return bitmap;
        } catch (Exception e) {
            Log.e(TAG, "Image转Bitmap失败: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 压缩Bitmap为WebP格式
     */
    private byte[] compressToWebP(Bitmap bitmap) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            boolean success = bitmap.compress(Bitmap.CompressFormat.WEBP, WEBP_QUALITY, outputStream);
            
            if (success) {
                return outputStream.toByteArray();
            } else {
                Log.e(TAG, "WebP压缩失败");
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "WebP压缩异常: " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * 输出WebP数据到Log
     */
    private void logWebPData(byte[] webpData, long frameNumber) {
        try {
            // 基本信息日志
            Log.i(TAG, String.format(
                "📄 Frame #%d WebP Data: %d bytes (%.1fKB)", 
                frameNumber, webpData.length, webpData.length / 1024.0f
            ));
            
            // 如果启用Base64输出
            if (ENABLE_BASE64_LOG && base64Encoder != null) {
                String base64Data = base64Encoder.encodeToString(webpData);
                
                // 分段输出Base64数据（避免Log截断）
                int chunkSize = 3000; // 每段最大3000字符
                int totalChunks = (base64Data.length() + chunkSize - 1) / chunkSize;
                
                Log.i(TAG, String.format(
                    "💻 Frame #%d Base64 Start: %d chars, %d chunks", 
                    frameNumber, base64Data.length(), totalChunks
                ));
                
                for (int i = 0; i < totalChunks; i++) {
                    int start = i * chunkSize;
                    int end = Math.min(start + chunkSize, base64Data.length());
                    String chunk = base64Data.substring(start, end);
                    
                    Log.d(TAG, String.format(
                        "[WEBP-BASE64] Frame #%d Chunk %d/%d: %s", 
                        frameNumber, i + 1, totalChunks, chunk
                    ));
                }
                
                Log.i(TAG, String.format(
                    "✅ Frame #%d Base64 Complete: %d chunks logged", 
                    frameNumber, totalChunks
                ));
            }
            
        } catch (Exception e) {
            Log.e(TAG, String.format("❌ Frame #%d Log输出失败: %s", frameNumber, e.getMessage()), e);
        }
    }
    
    /**
     * 输出捕获统计信息
     */
    private void logCaptureStats() {
        try {
            long currentFrames = frameCount.get();
            long currentDataSize = totalDataSize.get();
            long elapsed = System.currentTimeMillis() - captureStartTime;
            
            if (elapsed > 0) {
                float currentFPS = currentFrames * 1000.0f / elapsed;
                float dataRate = currentDataSize * 8.0f / 1024.0f / 1024.0f / (elapsed / 1000.0f);
                float avgFrameSize = currentFrames > 0 ? currentDataSize / 1024.0f / currentFrames : 0;
                
                Log.i(TAG, String.format(
                    "📈 [STATS] 帧数:%d | FPS:%.1f | 数据:%.1fMB | 码率:%.1fMbps | 平均:%.1fKB/帧 | 运行:%.1fs",
                    currentFrames, currentFPS, currentDataSize / 1024.0f / 1024.0f, 
                    dataRate, avgFrameSize, elapsed / 1000.0f
                ));
            }
        } catch (Exception e) {
            Log.e(TAG, "统计输出失败", e);
        }
    }
    
    /**
     * 停止后台线程
     */
    private void stopBackgroundThread() {
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "停止后台线程失败", e);
            }
        }
    }
    
    /**
     * 设置数据回调
     */
    public void setScreenDataCallback(ScreenDataCallback callback) {
        this.screenDataCallback = callback;
    }
    
    /**
     * 准备屏幕捕获（需要用户授权）
     * 此方法需要在Activity中调用，获取用户授权
     */
    public Intent createScreenCaptureIntent() {
        return mediaProjectionManager.createScreenCaptureIntent();
    }
    
    /**
     * 启动屏幕捕获
     * @param resultCode 授权结果码
     * @param data 授权数据
     */
    public void startCapture(int resultCode, Intent data) {
        if (isCapturing) {
            Log.w(TAG, "🔄 屏幕捕获已在运行中");
            return;
        }
        
        try {
            Log.i(TAG, "🚀 启动屏幕捕获 - Android 14+兼容模式");
            Log.i(TAG, "📱 系统版本: Android " + android.os.Build.VERSION.RELEASE + " (API " + android.os.Build.VERSION.SDK_INT + ")");
            
            // Android 14+ 需要在前台服务中创建MediaProjection
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                Log.i(TAG, "🚫 Android 14+ 检测到，需要特殊处理MediaProjection");
                
                // 检查是否在前台服务上下文中
                if (!(context instanceof android.app.Service)) {
                    Log.e(TAG, "❌ Android 14+要求MediaProjection在前台服务中创建");
                    handleStartCaptureError("系统要求错误: Android 14+需要在前台服务中运行屏幕捕获", 
                        new SecurityException("MediaProjection requires foreground service on Android 14+"));
                    return;
                }
            }
            
            // 获取MediaProjection对象（加强错误处理）
            try {
                Log.i(TAG, "🔍 正在创建MediaProjection...");
                mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
                
                if (mediaProjection == null) {
                    Log.e(TAG, "❌ MediaProjection对象为null");
                    handleStartCaptureError("MediaProjection创建失败：返回null对象", null);
                    return;
                }
                
                Log.i(TAG, "✅ MediaProjection创建成功");
                
            } catch (SecurityException e) {
                Log.e(TAG, "❌ MediaProjection创建安全异常: " + e.getMessage(), e);
                handleStartCaptureError("安全权限错误: " + e.getMessage() + 
                    "\n\u8bf7确保应用在前台运行并具有正确的服务类型", e);
                return;
            } catch (Exception e) {
                Log.e(TAG, "❌ MediaProjection创建异常: " + e.getMessage(), e);
                handleStartCaptureError("MediaProjection创建失败: " + e.getMessage(), e);
                return;
            }
            
            Log.i(TAG, "📺 MediaProjection创建成功");
            
            // 注册MediaProjection回调（Android 14+必需）
            try {
                mediaProjection.registerCallback(mediaProjectionCallback, backgroundHandler);
                Log.i(TAG, "✅ MediaProjection回调已注册");
            } catch (Exception e) {
                Log.e(TAG, "MediaProjection回调注册失败: " + e.getMessage());
                if (screenDataCallback != null) {
                    screenDataCallback.onError("MediaProjection回调注册失败: " + e.getMessage());
                }
                return;
            }
            
            // 创建ImageReader - 用于截图 (使用2个缓冲区以兼容更多设备)
            imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
            
            // 创建VirtualDisplay
            virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture-Timer",
                screenWidth, screenHeight, screenDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null, backgroundHandler
            );
            
            // 重置统计数据
            frameCount.set(0);
            totalDataSize.set(0);
            captureStartTime = System.currentTimeMillis();
            
            isCapturing = true;
            
            // 启动定时截图
            captureHandler.post(captureRunnable);
            
            // 启动统计报告
            if (statsHandler != null && statsRunnable != null) {
                statsHandler.post(statsRunnable);
            }
            
            Log.i(TAG, String.format(
                "✅ 屏幕捕获启动成功 - %dx%d@%dDPI | %dFPS | WebP质量:%d%% | WebSocket:%s",
                screenWidth, screenHeight, screenDpi, TARGET_FPS, WEBP_QUALITY,
                enableWebSocketPush ? "已启用" : "已禁用"
            ));
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 启动屏幕捕获失败", e);
            handleStartCaptureError("启动屏幕捕获失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 处理屏幕捕获启动失败，通知UI自动关闭权限对话框
     */
    private void handleStartCaptureError(String errorMessage, Exception exception) {
        Log.e(TAG, "🚨 屏幕捕获失败处理: " + errorMessage);
        
        // 重置状态
        isCapturing = false;
        
        // 清理资源
        if (mediaProjection != null) {
            try {
                mediaProjection.stop();
                mediaProjection = null;
            } catch (Exception e) {
                Log.w(TAG, "清理MediaProjection异常", e);
            }
        }
        
        // 通知回调（重要：这会触发UI中的错误处理逻辑）
        if (screenDataCallback != null) {
            screenDataCallback.onError(errorMessage);
        }
        
        // 记录详细错误信息
        if (exception != null) {
            Log.e(TAG, "详细错误堆栈", exception);
        }
        
        // 特别处理Android 14+的MediaProjection错误
        if (exception instanceof SecurityException && 
            exception.getMessage() != null && 
            exception.getMessage().contains("Media projections require a foreground service")) {
            
            Log.e(TAG, "🚨 Android 14+ MediaProjection服务错误 - 请检查AndroidManifest.xml配置");
        }
    }
    
    /**
     * 启动屏幕捕获（无需授权，用于服务调用）
     */
    public void startCapture() {
        // 注意：定时截图模式需要先获得用户授权
        Log.w(TAG, "⚠️ 定时截图需要用户授权，请使用 startCapture(resultCode, data) 方法");
    }
    
    /**
     * 停止屏幕捕获
     */
    public void stopCapture() {
        if (!isCapturing) {
            Log.w(TAG, "🚫 屏幕捕获未启动");
            return;
        }
        
        try {
            Log.i(TAG, "🛑 停止定时截图模式");
            
            isCapturing = false;
            
            // 停止定时器
            if (captureHandler != null && captureRunnable != null) {
                captureHandler.removeCallbacks(captureRunnable);
            }
            
            // 停止统计报告
            if (statsHandler != null && statsRunnable != null) {
                statsHandler.removeCallbacks(statsRunnable);
            }
            
            // 释放资源
            if (virtualDisplay != null) {
                virtualDisplay.release();
                virtualDisplay = null;
            }
            
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
            
            // 解除注册MediaProjection回调
            if (mediaProjection != null) {
                try {
                    mediaProjection.unregisterCallback(mediaProjectionCallback);
                    Log.i(TAG, "🗑️ MediaProjection回调已解除注册");
                } catch (Exception e) {
                    Log.w(TAG, "MediaProjection回调解除注册失败: " + e.getMessage());
                }
                
                mediaProjection.stop();
                mediaProjection = null;
            }
            
            // 输出最终统计
            long frames = frameCount.get();
            long dataSize = totalDataSize.get();
            long totalTime = System.currentTimeMillis() - captureStartTime;
            
            Log.i(TAG, String.format(
                "🏁 截图停止 - 总帧数:%d | 数据量:%.1fMB | 总时间:%.1fs | 平均FPS:%.1f",
                frames, dataSize / 1024.0f / 1024.0f, totalTime / 1000.0f,
                totalTime > 0 ? frames * 1000.0f / totalTime : 0
            ));
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 停止屏幕捕获失败", e);
        }
    }
    
    // =========================
    // 新的定时截图架构不需要图像监听器
    // 原有的 imageAvailableListener 和 processImage 方法已被替换
    // =========================
    
    /**
     * 获取捕获状态
     */
    public boolean isCapturing() {
        return isCapturing;
    }
    
    /**
     * 释放资源
     */
    public void release() {
        try {
            Log.i(TAG, "🗑️ 释放 ScreenCaptureManager 资源");
            
            // 停止捕获
            stopCapture();
            
            // 停止后台线程
            stopBackgroundThread();
            
            // 清理Base64编码器
            base64Encoder = null;
            
            // 清理Handler
            if (captureHandler != null && captureRunnable != null) {
                captureHandler.removeCallbacks(captureRunnable);
            }
            
            if (statsHandler != null && statsRunnable != null) {
                statsHandler.removeCallbacks(statsRunnable);
            }
            
            Log.i(TAG, "✅ ScreenCaptureManager 资源释放完成");
        } catch (Exception e) {
            Log.e(TAG, "❌ 释放资源失败", e);
        }
    }
}
