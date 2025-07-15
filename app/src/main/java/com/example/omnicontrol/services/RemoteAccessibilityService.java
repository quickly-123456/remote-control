package com.example.omnicontrol.services;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * 远程无障碍服务
 * 通过无障碍服务实现远程控制输入功能
 */
public class RemoteAccessibilityService extends AccessibilityService {
    private static final String TAG = "RemoteAccessibilityService";
    
    private static RemoteAccessibilityService instance;
    private Handler mainHandler;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        mainHandler = new Handler(Looper.getMainLooper());
        Log.i(TAG, "RemoteAccessibilityService created");
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        Log.i(TAG, "RemoteAccessibilityService destroyed");
    }
    
    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 监听辅助功能事件（如果需要）
        if (event != null) {
            Log.d(TAG, "Accessibility event: " + event.getEventType());
        }
    }
    
    @Override
    public void onInterrupt() {
        Log.w(TAG, "RemoteAccessibilityService interrupted");
    }
    
    /**
     * 获取服务实例
     */
    public static RemoteAccessibilityService getInstance() {
        return instance;
    }
    
    /**
     * 执行点击操作
     */
    public void performClick(int x, int y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Path clickPath = new Path();
            clickPath.moveTo(x, y);
            
            GestureDescription.StrokeDescription clickStroke = 
                new GestureDescription.StrokeDescription(clickPath, 0, 100);
            
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(clickStroke);
            
            boolean result = dispatchGesture(gestureBuilder.build(), 
                new GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription gestureDescription) {
                        super.onCompleted(gestureDescription);
                        Log.d(TAG, "Click gesture completed");
                    }
                    
                    @Override
                    public void onCancelled(GestureDescription gestureDescription) {
                        super.onCancelled(gestureDescription);
                        Log.w(TAG, "Click gesture cancelled");
                    }
                }, null);
            
            if (!result) {
                Log.e(TAG, "Failed to dispatch click gesture");
            }
        } else {
            Log.w(TAG, "Gesture dispatch requires Android N or higher");
        }
    }
    
    /**
     * 执行滑动操作
     */
    public void performSwipe(int startX, int startY, int endX, int endY, int duration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Path swipePath = new Path();
            swipePath.moveTo(startX, startY);
            swipePath.lineTo(endX, endY);
            
            GestureDescription.StrokeDescription swipeStroke = 
                new GestureDescription.StrokeDescription(swipePath, 0, duration);
            
            GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
            gestureBuilder.addStroke(swipeStroke);
            
            boolean result = dispatchGesture(gestureBuilder.build(),
                new GestureResultCallback() {
                    @Override
                    public void onCompleted(GestureDescription gestureDescription) {
                        super.onCompleted(gestureDescription);
                        Log.d(TAG, "Swipe gesture completed");
                    }
                    
                    @Override
                    public void onCancelled(GestureDescription gestureDescription) {
                        super.onCancelled(gestureDescription);
                        Log.w(TAG, "Swipe gesture cancelled");
                    }
                }, null);
            
            if (!result) {
                Log.e(TAG, "Failed to dispatch swipe gesture");
            }
        } else {
            Log.w(TAG, "Gesture dispatch requires Android N or higher");
        }
    }
    
    /**
     * 输入文本
     */
    public void inputText(String text) {
        try {
            // 方法1：通过剪贴板输入
            inputTextViaClipboard(text);
            
            // 等待一段时间后尝试方法2：直接输入到焦点节点
            mainHandler.postDelayed(() -> {
                inputTextToFocusedNode(text);
            }, 500);
            
        } catch (Exception e) {
            Log.e(TAG, "Error inputting text", e);
        }
    }
    
    /**
     * 通过剪贴板输入文本
     */
    private void inputTextViaClipboard(String text) {
        try {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clipData = ClipData.newPlainText("remote_input", text);
                clipboard.setPrimaryClip(clipData);
                
                // 模拟粘贴操作 (Ctrl+V)
                performKeyPress(KeyEvent.KEYCODE_CTRL_LEFT, KeyEvent.KEYCODE_V);
                
                Log.d(TAG, "Text input via clipboard: " + text);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inputting text via clipboard", e);
        }
    }
    
    /**
     * 直接输入文本到焦点节点
     */
    private void inputTextToFocusedNode(String text) {
        try {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                AccessibilityNodeInfo focusedNode = rootNode.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
                if (focusedNode != null && focusedNode.isEditable()) {
                    Bundle arguments = new Bundle();
                    arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
                    boolean result = focusedNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
                    
                    if (result) {
                        Log.d(TAG, "Text input to focused node: " + text);
                    } else {
                        Log.w(TAG, "Failed to input text to focused node");
                    }
                    
                    focusedNode.recycle();
                }
                rootNode.recycle();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error inputting text to focused node", e);
        }
    }
    
    /**
     * 执行单个按键
     */
    public void performKeyPress(int keyCode) {
        try {
            // 按下
            KeyEvent downEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
            performGlobalAction(downEvent.getKeyCode());
            
            // 释放
            KeyEvent upEvent = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
            performGlobalAction(upEvent.getKeyCode());
            
            Log.d(TAG, "Key press performed: " + keyCode);
        } catch (Exception e) {
            Log.e(TAG, "Error performing key press", e);
        }
    }
    
    /**
     * 执行组合按键
     */
    public void performKeyPress(int... keyCodes) {
        try {
            // 同时按下所有按键
            for (int keyCode : keyCodes) {
                KeyEvent downEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
                performGlobalAction(downEvent.getKeyCode());
            }
            
            // 短暂延迟
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // 释放所有按键（逆序）
            for (int i = keyCodes.length - 1; i >= 0; i--) {
                KeyEvent upEvent = new KeyEvent(KeyEvent.ACTION_UP, keyCodes[i]);
                performGlobalAction(upEvent.getKeyCode());
            }
            
            Log.d(TAG, "Combination key press performed");
        } catch (Exception e) {
            Log.e(TAG, "Error performing combination key press", e);
        }
    }
    
    /**
     * 执行返回操作
     */
    public void performBack() {
        boolean result = performGlobalAction(GLOBAL_ACTION_BACK);
        Log.d(TAG, "Back action performed: " + result);
    }
    
    /**
     * 执行主页操作
     */
    public void performHome() {
        boolean result = performGlobalAction(GLOBAL_ACTION_HOME);
        Log.d(TAG, "Home action performed: " + result);
    }
    
    /**
     * 执行最近任务操作
     */
    public void performRecents() {
        boolean result = performGlobalAction(GLOBAL_ACTION_RECENTS);
        Log.d(TAG, "Recents action performed: " + result);
    }
    
    /**
     * 执行通知面板操作
     */
    public void performNotifications() {
        boolean result = performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
        Log.d(TAG, "Notifications action performed: " + result);
    }
    
    /**
     * 执行快速设置操作
     */
    public void performQuickSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            boolean result = performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
            Log.d(TAG, "Quick settings action performed: " + result);
        } else {
            Log.w(TAG, "Quick settings action requires API level 17 or higher");
        }
    }
    
    /**
     * 查找可点击的节点
     */
    public boolean performClickOnText(String text) {
        try {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                AccessibilityNodeInfo targetNode = findNodeByText(rootNode, text);
                if (targetNode != null && targetNode.isClickable()) {
                    boolean result = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    targetNode.recycle();
                    rootNode.recycle();
                    
                    Log.d(TAG, "Click on text performed: " + text + ", result: " + result);
                    return result;
                }
                rootNode.recycle();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error performing click on text", e);
        }
        return false;
    }
    
    /**
     * 根据文本查找节点
     */
    private AccessibilityNodeInfo findNodeByText(AccessibilityNodeInfo node, String text) {
        if (node == null || text == null) return null;
        
        CharSequence nodeText = node.getText();
        if (nodeText != null && nodeText.toString().contains(text)) {
            return node;
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findNodeByText(child, text);
                if (result != null) {
                    child.recycle();
                    return result;
                }
                child.recycle();
            }
        }
        
        return null;
    }
    
    /**
     * 获取屏幕上所有文本内容
     */
    public String getScreenText() {
        try {
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                StringBuilder text = new StringBuilder();
                extractTextFromNode(rootNode, text);
                rootNode.recycle();
                return text.toString();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting screen text", e);
        }
        return "";
    }
    
    /**
     * 从节点提取文本
     */
    private void extractTextFromNode(AccessibilityNodeInfo node, StringBuilder text) {
        if (node == null) return;
        
        CharSequence nodeText = node.getText();
        if (nodeText != null && nodeText.length() > 0) {
            text.append(nodeText).append("\n");
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                extractTextFromNode(child, text);
                child.recycle();
            }
        }
    }
    
    /**
     * 检查服务是否可用
     */
    public boolean isServiceAvailable() {
        return instance != null;
    }
}
