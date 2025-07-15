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
    
    private Context context;
    private AudioRecord audioRecord;
    private int minBufferSize;
    
    // 后台线程
    private HandlerThread recordingThread;
    private Handler recordingHandler;
    private volatile boolean isRecording = false;
    
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
                Log.e(TAG, "Invalid buffer size");
                return;
            }
            
            // 创建AudioRecord
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
                == PackageManager.PERMISSION_GRANTED) {
                
                audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    minBufferSize * 2 // 使用2倍缓冲区
                );
                
                if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                    Log.d(TAG, String.format("AudioRecord initialized: sample rate=%d, buffer size=%d", 
                        SAMPLE_RATE, minBufferSize));
                } else {
                    Log.e(TAG, "AudioRecord initialization failed");
                    audioRecord = null;
                }
            } else {
                Log.e(TAG, "Audio record permission not granted");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing AudioRecord", e);
        }
    }
    
    /**
     * 设置音频数据回调
     */
    public void setAudioDataCallback(AudioDataCallback callback) {
        this.audioDataCallback = callback;
    }
    
    /**
     * 开始录音
     */
    public void startRecording() {
        if (isRecording) {
            Log.w(TAG, "Audio recording already started");
            return;
        }
        
        if (audioRecord == null) {
            Log.e(TAG, "AudioRecord not initialized");
            if (audioDataCallback != null) {
                audioDataCallback.onError("音频录制器未初始化");
            }
            return;
        }
        
        // 检查权限
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Audio record permission not granted");
            if (audioDataCallback != null) {
                audioDataCallback.onError("麦克风权限未授予");
            }
            return;
        }
        
        try {
            // 启动录音线程
            startRecordingThread();
            
            // 开始录音
            audioRecord.startRecording();
            isRecording = true;
            
            Log.i(TAG, "Audio recording started successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to start audio recording", e);
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
            // 可以在这里进行音频处理，如降噪、压缩等
            
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
     * 释放资源
     */
    public void release() {
        stopRecording();
        
        if (audioRecord != null) {
            try {
                audioRecord.release();
                audioRecord = null;
            } catch (Exception e) {
                Log.e(TAG, "Error releasing AudioRecord", e);
            }
        }
    }
}
