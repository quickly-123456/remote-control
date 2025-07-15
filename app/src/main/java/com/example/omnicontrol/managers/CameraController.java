package com.example.omnicontrol.managers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
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
    
    // 数据回调接口
    public interface CameraDataCallback {
        void onCameraData(byte[] data);
        void onError(String error);
    }
    
    private CameraDataCallback cameraDataCallback;
    
    public CameraController(Context context) {
        this.context = context;
        this.cameraManager = (android.hardware.camera2.CameraManager) 
            context.getSystemService(Context.CAMERA_SERVICE);
        
        startBackgroundThread();
        initCamera();
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
    }
    
    /**
     * 启动摄像头
     */
    public void startCamera() {
        if (isCameraOpen) {
            Log.w(TAG, "Camera already opened");
            return;
        }
        
        if (cameraId == null) {
            Log.e(TAG, "No camera available");
            if (cameraDataCallback != null) {
                cameraDataCallback.onError("没有可用的摄像头");
            }
            return;
        }
        
        // 检查权限
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Camera permission not granted");
            if (cameraDataCallback != null) {
                cameraDataCallback.onError("摄像头权限未授予");
            }
            return;
        }
        
        try {
            // 创建ImageReader
            imageReader = ImageReader.newInstance(
                previewSize.getWidth(), 
                previewSize.getHeight(), 
                ImageFormat.JPEG, 
                2
            );
            imageReader.setOnImageAvailableListener(imageAvailableListener, backgroundHandler);
            
            // 打开摄像头
            cameraManager.openCamera(cameraId, cameraStateCallback, backgroundHandler);
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start camera", e);
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
                Log.i(TAG, "Camera opened successfully");
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
     * 处理捕获的图像
     */
    private void processImage(Image image) {
        try {
            // 获取JPEG数据
            ByteBuffer buffer = image.getPlanes()[0].getBuffer();
            byte[] imageData = new byte[buffer.remaining()];
            buffer.get(imageData);
            
            // 回调数据
            if (cameraDataCallback != null) {
                cameraDataCallback.onCameraData(imageData);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing camera image", e);
            if (cameraDataCallback != null) {
                cameraDataCallback.onError("摄像头图像处理错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 获取摄像头状态
     */
    public boolean isCameraOpen() {
        return isCameraOpen;
    }
    
    /**
     * 释放资源
     */
    public void release() {
        stopCamera();
        stopBackgroundThread();
    }
}
