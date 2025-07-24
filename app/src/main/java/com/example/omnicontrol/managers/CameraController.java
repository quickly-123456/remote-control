package com.example.omnicontrol.managers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

// WebSocket实时推送相关导入
import com.example.omnicontrol.utils.WebSocketManager;
import com.example.omnicontrol.utils.RDTDefine;
import com.example.omnicontrol.utils.RDTMessage;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 摄像头控制器
 * 负责管理摄像头的开启、关闭和画面捕获
 */
public class CameraController {
    private static final String TAG = "CameraController";
    
    private Context context;
    private android.hardware.camera2.CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private ImageReader imageReader;
    
    // 摄像头参数
    private String cameraId;
    private Size previewSize;
    
    // 后台线程
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    
    // 捕获状态
    private boolean isCameraOpen = false;
    
    // 日志输出时间控制
    private long lastLogTime = 0;
    
    // WebSocket实时推送（从PermissionManager获取）
    // WebSocketManager使用单例模式，不需要实例变量
    private volatile boolean enableWebSocketPush = false; // 是否启用WebSocket推送
    private byte[] latestImageData; // 最新的图像数据
    private final Object imageDataLock = new Object(); // 图像数据锁
    
    // 定时器配置（与ScreenCaptureManager一致）
    private static final int TARGET_FPS = 25;
    private static final long CAPTURE_INTERVAL_MS = 1000 / TARGET_FPS; // 40ms
    private Handler captureHandler;
    private Runnable captureRunnable;
    
    // WebP压缩配置
    private static final int WEBP_QUALITY = 80; // WebP质量 (0-100)
    
    // 统计数据
    private AtomicLong frameCount = new AtomicLong(0);
    private AtomicLong totalDataSize = new AtomicLong(0);
    private long captureStartTime;
    
    // 数据回调接口
    public interface CameraDataCallback {
        void onCameraData(byte[] data);
        void onError(String error);
    }
    
    // 数据回调（保持兼容性）
    private CameraDataCallback cameraDataCallback;
    
    public CameraController(Context context) {
        this.context = context;
        this.cameraManager = (android.hardware.camera2.CameraManager) 
            context.getSystemService(Context.CAMERA_SERVICE);
        
        // 初始化后台线程
        startBackgroundThread();
        
        // 选择摄像头
        initCamera();
        
        // 初始化定时采集器
        initCaptureTimer();
        
        Log.i(TAG, "📹 CameraController初始化完成 - WebSocket+RDT协议模式");
    }
    
    /**
     * 启动后台线程
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
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
                Log.e(TAG, "Error stopping background thread", e);
            }
        }
    }
    
    /**
     * 初始化摄像头
     */
    private void initCamera() {
        try {
            // 获取后置摄像头ID
            String[] cameraIds = cameraManager.getCameraIdList();
            for (String id : cameraIds) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                    cameraId = id;
                    break;
                }
            }
            
            if (cameraId == null && cameraIds.length > 0) {
                cameraId = cameraIds[0]; // 使用第一个可用摄像头
            }
            
            if (cameraId != null) {
                // 获取支持的预览尺寸
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Size[] sizes = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    .getOutputSizes(ImageFormat.JPEG);
                
                // 选择合适的预览尺寸（选择中等分辨率）
                previewSize = chooseOptimalSize(sizes);
                
                Log.d(TAG, String.format("Camera initialized: %s, preview size: %dx%d", 
                    cameraId, previewSize.getWidth(), previewSize.getHeight()));
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing camera", e);
        }
    }
    
    /**
     * 选择最优的预览尺寸
     */
    private Size chooseOptimalSize(Size[] sizes) {
        if (sizes == null || sizes.length == 0) {
            return new Size(640, 480); // 默认尺寸
        }
        
        // 按分辨率排序，选择中等分辨率
        List<Size> sizeList = Arrays.asList(sizes);
        Collections.sort(sizeList, new Comparator<Size>() {
            @Override
            public int compare(Size lhs, Size rhs) {
                return Long.signum((long) lhs.getWidth() * lhs.getHeight() - 
                                 (long) rhs.getWidth() * rhs.getHeight());
            }
        });
        
        // 选择接近1280x720的尺寸
        for (Size size : sizeList) {
            if (size.getWidth() >= 1280 && size.getHeight() >= 720) {
                return size;
            }
        }
        
        // 如果没有找到合适的，返回中间的尺寸
        return sizeList.get(sizeList.size() / 2);
    }
    
    /**
     * 设置数据回调
     */
    public void setCameraDataCallback(CameraDataCallback callback) {
        this.cameraDataCallback = callback;
        Log.i(TAG, "📷 设置摄像头数据回调");
    }
    
    /**
     * 获取摄像头状态
     */
    public boolean isCameraOpen() {
        return isCameraOpen;
    }
    
    /**
     * 启用WebSocket推送（像屏幕共享一样自动开始后台传输）
     */
    public void enableWebSocketPush() {
        Log.i(TAG, "📷 启用摄像头WebSocket推送 - 开始后台传输");
        
        // 设置推送标记
        enableWebSocketPush = true;
        
        // 注意：不在这里调用startCamera()防止循环调用
        // 调用方应该先调用startCamera()再调用enableWebSocketPush()
        
        // 启动后台传输定时器（每40ms）
        startCaptureTimer();
        
        Log.i(TAG, "📡 摄像头后台传输已启动 - 40ms间隔");
    }
    
    /**
     * 禁用WebSocket推送
     */
    public void disableWebSocketPush() {
        enableWebSocketPush = false;
        stopCaptureTimer();
        Log.i(TAG, "📷 禁用摄像头WebSocket推送");
    }
    
    /**
     * 启动摄像头（WebSocket+RDT协议模式）
     */
    public void startCamera() {
        if (isCameraOpen) {
            Log.w(TAG, "📹 摄像头已开启");
            return;
        }
        
        if (cameraId == null) {
            Log.e(TAG, "❌ 没有可用的摄像头");
            if (cameraDataCallback != null) {
                cameraDataCallback.onError("没有可用的摄像头");
            }
            return;
        }
        
        // 检查摄像头权限
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "❌ 摄像头权限未授予");
            if (cameraDataCallback != null) {
                cameraDataCallback.onError("摄像头权限未授予");
            }
            return;
        }
        
        try {
            Log.i(TAG, "📹 启动摄像头 - WebSocket+RDT协议模式");
            Log.i(TAG, "📐 摄像头参数 - 分辨率: " + previewSize.getWidth() + "x" + previewSize.getHeight() + ", 格式: JPEG");
            
            // WebSocket连接状态检查和初始化（优先处理）
            WebSocketManager webSocketManager = WebSocketManager.instance();
            if (webSocketManager != null) {
                Log.i(TAG, "🌐 WebSocket状态检查 - 连接状态: " + (webSocketManager.isConnected() ? "✅已连接" : "❌断开"));
                
                if (!webSocketManager.isConnected()) {
                    Log.i(TAG, "🔄 尝试重新连接WebSocket...");
                    webSocketManager.connect();
                    
                    // 等待连接建立（最多3秒）
                    new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        boolean connected = webSocketManager.isConnected();
                        Log.i(TAG, "🔍 WebSocket连接结果: " + (connected ? "✅成功" : "❌失败"));
                        if (connected) {
                            enableWebSocketPush();
                            Log.i(TAG, "🚀 摄像头WebSocket推送已启用");
                        } else {
                            Log.w(TAG, "⚠️ WebSocket连接失败，摄像头数据将无法推送");
                        }
                    }, 3000);
                } else {
                    enableWebSocketPush();
                    Log.i(TAG, "🚀 摄像头WebSocket推送已启用（现有连接）");
                }
            } else {
                Log.w(TAG, "⚠️ WebSocket管理器为null，摄像头数据无法推送");
            }
            
            // 创建ImageReader
            imageReader = ImageReader.newInstance(
                previewSize.getWidth(), 
                previewSize.getHeight(), 
                ImageFormat.JPEG, 
                2
            );
            imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler);
            
            // 打开摄像头
            Log.i(TAG, "🔓 正在打开摄像头设备: " + cameraId);
            cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler);
            
            // 重置统计数据
            frameCount.set(0);
            totalDataSize.set(0);
            captureStartTime = System.currentTimeMillis();
            
        } catch (SecurityException e) {
            Log.e(TAG, "❌ 摄像头权限被拒绝", e);
            if (cameraDataCallback != null) {
                cameraDataCallback.onError("摄像头权限被拒绝: " + e.getMessage());
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ 启动摄像头失败", e);
            if (cameraDataCallback != null) {
                cameraDataCallback.onError("启动摄像头失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 停止摄像头
     */
    public void stopCamera() {
        if (!isCameraOpen) {
            Log.w(TAG, "Camera not opened");
            return;
        }
        
        try {
            if (captureSession != null) {
                captureSession.close();
                captureSession = null;
            }
            
            if (cameraDevice != null) {
                cameraDevice.close();
                cameraDevice = null;
            }
            
            if (imageReader != null) {
                imageReader.close();
                imageReader = null;
            }
            
            isCameraOpen = false;
            Log.i(TAG, "Camera stopped successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping camera", e);
        }
    }
    
    /**
     * 摄像头状态回调
     */
    private final CameraDevice.StateCallback cameraStateCallback = 
        new CameraDevice.StateCallback() {
            @Override
            public void onOpened(@NonNull CameraDevice camera) {
                cameraDevice = camera;
                isCameraOpen = true;
                createCaptureSession();
                
                // 启动定时采集（每40ms）
                startCaptureTimer();
                
                Log.i(TAG, "📹 摄像头打开成功 - 开始定时采集（40ms间隔）");
            }
            
            @Override
            public void onDisconnected(@NonNull CameraDevice camera) {
                camera.close();
                cameraDevice = null;
                isCameraOpen = false;
                Log.w(TAG, "Camera disconnected");
            }
            
            @Override
            public void onError(@NonNull CameraDevice camera, int error) {
                camera.close();
                cameraDevice = null;
                isCameraOpen = false;
                Log.e(TAG, "Camera error: " + error);
                if (cameraDataCallback != null) {
                    cameraDataCallback.onError("摄像头错误: " + error);
                }
            }
        };
    
    /**
     * 创建捕获会话
     */
    private void createCaptureSession() {
        try {
            Surface surface = imageReader.getSurface();
            
            cameraDevice.createCaptureSession(
                Arrays.asList(surface),
                new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession session) {
                        captureSession = session;
                        startRepeatingCapture();
                        Log.i(TAG, "Capture session configured");
                    }
                    
                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        Log.e(TAG, "Capture session configure failed");
                        if (cameraDataCallback != null) {
                            cameraDataCallback.onError("摄像头捕获会话配置失败");
                        }
                    }
                },
                backgroundHandler
            );
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to create capture session", e);
            if (cameraDataCallback != null) {
                cameraDataCallback.onError("创建捕获会话失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 开始重复捕获
     */
    private void startRepeatingCapture() {
        try {
            CaptureRequest.Builder captureBuilder = 
                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
            
            // 设置重复捕获（每秒捕获几帧）
            captureSession.setRepeatingRequest(
                captureBuilder.build(),
                captureCallback,
                backgroundHandler
            );
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start repeating capture", e);
            if (cameraDataCallback != null) {
                cameraDataCallback.onError("开始重复捕获失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 捕获回调
     */
    private final CameraCaptureSession.CaptureCallback captureCallback = 
        new CameraCaptureSession.CaptureCallback() {
            @Override
            public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                         @NonNull CaptureRequest request,
                                         @NonNull TotalCaptureResult result) {
                // 捕获完成
            }
        };
    
    /**
     * 图像可用监听器
     */
    private final ImageReader.OnImageAvailableListener imageAvailableListener = 
        new ImageReader.OnImageAvailableListener() {
            @Override
            public void onImageAvailable(ImageReader reader) {
                try {
                    Image image = reader.acquireLatestImage();
                    if (image != null) {
                        processImage(image);
                        image.close();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing camera image", e);
                }
            }
        };
    
    /**
     * 处理捕获的图像 - 转换为WebP格式并记录详细信息
     */
    private void processImage(Image image) {
        if (image == null) {
            return;
        }
        
        try {
            // 将Image转换为Bitmap
            Bitmap bitmap = imageToBitmap(image);
            if (bitmap == null) {
                Log.w(TAG, "⚠️ 图像转换为Bitmap失败");
                return;
            }
            
            // 记录原始图像信息
            long originalSize = bitmap.getByteCount();
            Log.v(TAG, String.format("📸 原始图像 - 尺寸: %dx%d, 原始大小: %.1fKB, 格式: %s", 
                bitmap.getWidth(), bitmap.getHeight(), originalSize / 1024.0f, bitmap.getConfig()));
            
            // 压缩为WebP格式
            byte[] webpData = compressToWebP(bitmap);
            if (webpData == null) {
                Log.w(TAG, "⚠️ WebP压缩失败");
                bitmap.recycle();
                return;
            }
            
            // 计算压缩比
            float compressionRatio = (float) webpData.length / originalSize * 100;
            
            // 📷 每个摄像头帧都输出详细日志（像音频一样）
            long frameNum = frameCount.incrementAndGet();
            totalDataSize.addAndGet(webpData.length);
            
            Log.i(TAG, String.format("📷 摄像头帧 Frame #%d | 尺寸: %dx%d | 原始: %.1fKB | WebP: %.1fKB | 压缩率: %.1f%% | WebSocket: %s | 时间: %dms", 
                frameNum, bitmap.getWidth(), bitmap.getHeight(), originalSize / 1024.0f, webpData.length / 1024.0f, 
                compressionRatio, (WebSocketManager.instance() != null && WebSocketManager.instance().isConnected()) ? "✅连接" : "❌断开", 
                System.currentTimeMillis() % 100000));
            
            // 保存最新图像数据供WebSocket推送使用
            synchronized (imageDataLock) {
                latestImageData = webpData;
            }
            
            // 回调原始WebP数据（保持兼容性）
            if (cameraDataCallback != null) {
                cameraDataCallback.onCameraData(webpData);
            }
            
            // 每50帧输出统计信息（约2秒，因为40ms间隔）
            if (frameNum % 50 == 0) {
                long currentTime = System.currentTimeMillis();
                if (captureStartTime > 0) {
                    float timeDiff = (currentTime - captureStartTime) / 1000.0f;
                    float fps = frameNum / timeDiff;
                    Log.i(TAG, String.format("📊 摄像头统计 | 帧数: %d | FPS: %.1f | 累计: %.1f MB | 平均压缩率: %.1f%%", 
                           frameNum, fps, totalDataSize.get() / 1024.0f / 1024.0f, 
                           (totalDataSize.get() * 100.0f) / (frameNum * originalSize)));
                }
            }
            
            bitmap.recycle();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 处理摄像头图像数据异常", e);
            if (cameraDataCallback != null) {
                cameraDataCallback.onError("摄像头图像处理错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 实时日志输出图像数据信息
     */
    private void logImageData(Image image, byte[] imageData) {
        try {
            frameCount.incrementAndGet();
            
            // 获取图像基本信息
            int width = image.getWidth();
            int height = image.getHeight();
            int format = image.getFormat();
            long timestamp = image.getTimestamp();
            int dataSize = imageData.length;
            
            // 格式转换
            String formatName;
            switch (format) {
                case ImageFormat.JPEG:
                    formatName = "JPEG";
                    break;
                case ImageFormat.YUV_420_888:
                    formatName = "YUV_420_888";
                    break;
                case ImageFormat.NV21:
                    formatName = "NV21";
                    break;
                default:
                    formatName = "Unknown(" + format + ")";
            }
            
            // 计算数据质量指标
            int quality = calculateImageQuality(imageData);
            
            // 实时日志输出
            Log.i(TAG, String.format("[摄像头实时数据] 帧数: %d, 尺寸: %dx%d, 格式: %s, 数据大小: %d bytes, 质量: %d%%, 时间戳: %d", 
                frameCount, width, height, formatName, dataSize, quality, timestamp));
            
            // 每秒输出一次详细统计信息
            if (System.currentTimeMillis() - lastLogTime > 1000) {
                float fps = frameCount.get() / ((System.currentTimeMillis() - lastLogTime) / 1000.0f);
                Log.d(TAG, String.format("[摄像头统计] 帧率: %.1f fps, 总帧数: %d, 摄像头ID: %s, 预览尺寸: %s", 
                    fps, frameCount, cameraId, previewSize != null ? previewSize.toString() : "未设置"));
                lastLogTime = System.currentTimeMillis();
                frameCount.set(0);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error logging image data", e);
        }
    }
    
    /**
     * 计算图像质量指标 (简单的基于文件大小的估算)
     */
    private int calculateImageQuality(byte[] imageData) {
        if (imageData == null || imageData.length == 0) {
            return 0;
        }
        
        // 基于图像数据大小估算质量
        // 这是一个简化的计算，实际应用中可能需要更复杂的图像分析
        int size = imageData.length;
        
        // 假设高质量图像通常较大
        if (size > 500000) { // 500KB以上
            return 90 + (int)(Math.random() * 10);
        } else if (size > 200000) { // 200KB-500KB
            return 70 + (int)(Math.random() * 20);
        } else if (size > 100000) { // 100KB-200KB
            return 50 + (int)(Math.random() * 20);
        } else {
            return 30 + (int)(Math.random() * 20);
        }
    }
    
    /**
     * 获取摄像头状态
        Log.d(TAG, "🌐 摄像头WebSocket管理器初始化完成");
    }
    
    /**
     * 初始化定时采集器（每40ms采集一帧）
     */
    private void initCaptureTimer() {
        if (captureHandler == null) {
            captureHandler = new Handler(android.os.Looper.getMainLooper());
        }
        
        captureRunnable = new Runnable() {
            @Override
            public void run() {
                // 发送最新的摄像头数据（如果有的话）
                synchronized (imageDataLock) {
                    if (latestImageData != null && enableWebSocketPush) {
                        sendCameraData(latestImageData);
                    }
                }
                
                // 继续下一次发送
                if (enableWebSocketPush && captureHandler != null) {
                    captureHandler.postDelayed(this, CAPTURE_INTERVAL_MS);
                }
            }
        };
        
        Log.d(TAG, "📹 定时采集器初始化完成 (每40ms)");
    }
    
    /**
     * 采集摄像头帧并通过WebSocket发送（完全复用ScreenCaptureManager模式）
     */
    private void captureCameraFrame() {
        if (captureSession == null || !isCameraOpen) {
            return;
        }
        
        try {
            // 创建单次拍照请求
            CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(imageReader.getSurface());
            
            // 设置自动对焦和自动曝光
            captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
            
            // 执行单次拍照
            captureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                                @NonNull CaptureRequest request,
                                                @NonNull TotalCaptureResult result) {
                    // 拍照完成，等待imageAvailableListener处理
                }
                
                @Override
                public void onCaptureFailed(@NonNull CameraCaptureSession session,
                                             @NonNull CaptureRequest request,
                                             @NonNull CaptureFailure failure) {
                    Log.w(TAG, "⚠️ 摄像头拍照失败: " + failure.getReason());
                }
            }, backgroundHandler);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 摄像头拍照失败", e);
        }
    }
    
    /**
     * 处理相机图像数据 - 转换为WebP格式并记录详细信息
     */
    private void processImageData(Image image) {
        if (image == null) {
            return;
        }
        
        try {
            // 将Image转换为Bitmap
            Bitmap bitmap = imageToBitmap(image);
            if (bitmap == null) {
                Log.w(TAG, "⚠️ 图像转换为Bitmap失败");
                return;
            }
            
            // 记录原始图像信息
            long originalSize = bitmap.getByteCount();
            Log.v(TAG, String.format("📸 原始图像 - 尺寸: %dx%d, 原始大小: %.1fKB, 格式: %s", 
                bitmap.getWidth(), bitmap.getHeight(), originalSize / 1024.0f, bitmap.getConfig()));
            
            // 压缩为WebP格式
            byte[] webpData = compressToWebP(bitmap);
            if (webpData == null) {
                Log.w(TAG, "⚠️ WebP压缩失败");
                bitmap.recycle();
                return;
            }
            
            // 计算压缩比
            float compressionRatio = (float) webpData.length / originalSize * 100;
            
            // 📷 每个摄像头帧都输出详细日志（像音频一样）
            long frameNum = frameCount.incrementAndGet();
            totalDataSize.addAndGet(webpData.length);
            
            Log.i(TAG, String.format("📷 摄像头帧 Frame #%d | 尺寸: %dx%d | 原始: %.1fKB | WebP: %.1fKB | 压缩率: %.1f%% | WebSocket: %s | 时间: %dms", 
                frameNum, bitmap.getWidth(), bitmap.getHeight(), originalSize / 1024.0f, webpData.length / 1024.0f, 
                compressionRatio, (WebSocketManager.instance() != null && WebSocketManager.instance().isConnected()) ? "✅连接" : "❌断开", 
                System.currentTimeMillis() % 100000));
            
            // 保存最新图像数据供WebSocket推送使用
            synchronized (imageDataLock) {
                latestImageData = webpData;
            }
            
            // 回调原始WebP数据（保持兼容性）
            if (cameraDataCallback != null) {
                cameraDataCallback.onCameraData(webpData);
            }
            
            // 每50帧输出统计信息（约2秒，因为40ms间隔）
            if (frameNum % 50 == 0) {
                long currentTime = System.currentTimeMillis();
                if (captureStartTime > 0) {
                    float timeDiff = (currentTime - captureStartTime) / 1000.0f;
                    float fps = frameNum / timeDiff;
                    Log.i(TAG, String.format("📊 摄像头统计 | 帧数: %d | FPS: %.1f | 累计: %.1f MB | 平均压缩率: %.1f%%", 
                           frameNum, fps, totalDataSize.get() / 1024.0f / 1024.0f, 
                           (totalDataSize.get() * 100.0f) / (frameNum * originalSize)));
                }
            }
            
            bitmap.recycle();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 处理摄像头图像数据异常", e);
        }
    }
    
    /**
     * 将Image转换为Bitmap
     */
    private Bitmap imageToBitmap(Image image) {
        try {
            Image.Plane[] planes = image.getPlanes();
            ByteBuffer buffer = planes[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Image转Bitmap失败", e);
            return null;
        }
    }
    
    /**
     * 压缩Bitmap为WebP格式（完全复用ScreenCaptureManager逻辑）
     */
    private byte[] compressToWebP(Bitmap bitmap) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            boolean success = bitmap.compress(Bitmap.CompressFormat.WEBP, WEBP_QUALITY, outputStream);
            
            if (!success) {
                Log.w(TAG, "WebP压缩失败");
                return null;
            }
            
            return outputStream.toByteArray();
            
        } catch (Exception e) {
            Log.e(TAG, "WebP压缩异常", e);
            return null;
        }
    }
    
    /**
     * 发送摄像头数据（使用WebSocketManager的RDTProtocol）
     */
    private void sendCameraData(byte[] webpData) {
        WebSocketManager webSocketManager = WebSocketManager.instance();
        if (webSocketManager == null || !webSocketManager.isConnected()) {
            Log.v(TAG, "⚠️ 摄像头数据发送被跳过 - WebSocket未连接");
            return;
        }
        
        try {
            // 使用WebSocketManager的sendCameraData方法，已集成RDTProtocol
            webSocketManager.sendCameraData(webpData);
            
            // 更新统计数据
            long frameNum = frameCount.incrementAndGet();
            totalDataSize.addAndGet(webpData.length);
            
            // 📷 每个摄像头帧都输出日志（每40ms一次）- 像屏幕共享一样
            Log.i(TAG, String.format("📷 摄像头发送 Frame #%d | WebP: %.1fKB | 格式: WebP | WebSocket: ✓ | 时间: %dms", 
                   frameNum, webpData.length / 1024.0f, System.currentTimeMillis() % 100000));
            
            // 每50帧输出一次统计数据（约2秒）
            if (frameNum % 50 == 0) {
                long currentTime = System.currentTimeMillis();
                if (captureStartTime > 0) {
                    float timeDiff = (currentTime - captureStartTime) / 1000.0f;
                    float fps = frameNum / timeDiff;
                    Log.i(TAG, String.format("📡 摄像头发送统计 | 帧数: %d | FPS: %.1f | 累计: %.1f MB", 
                           frameNum, fps, totalDataSize.get() / 1024.0f / 1024.0f));
                }
            }
            
        } catch (Exception e) {
            Log.e(TAG, "发送摄像头数据失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 启动定时采集
     */
    private void startCaptureTimer() {
        if (captureHandler != null && captureRunnable != null) {
            captureStartTime = System.currentTimeMillis();
            frameCount.set(0);
            totalDataSize.set(0);
            
            Log.i(TAG, "🎬 启动摄像头定时采集 (40ms间隔)");
            captureHandler.post(captureRunnable);
        }
    }
    
    /**
     * 停止定时采集
     */
    private void stopCaptureTimer() {
        if (captureHandler != null && captureRunnable != null) {
            captureHandler.removeCallbacks(captureRunnable);
            Log.i(TAG, "🛑 摄像头定时采集已停止");
        }
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stopCamera();
        stopBackgroundThread();
        
        // 不需要断开WebSocket连接，因为它由PermissionManager管理
        
        Log.i(TAG, "🗑️ CameraController 资源释放完成");
    }
}
