package com.example.omnicontrol.managers;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.os.Build;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import com.example.omnicontrol.services.RemoteAccessibilityService;
// Removed MessageType import - no longer using Binary Protocol

/**
 * 触摸控制处理器
 * 负责处理SC_TOUCHED信号，执行远程触摸操作
 */
public class TouchControlHandler {
    private static final String TAG = "TouchControlHandler";
    
    private Context context;
    private RemoteAccessibilityService accessibilityService;
    private TouchControlCallback callback;
    

    
    public interface TouchControlCallback {
        void onTouchExecuted(int x, int y, boolean success);
        void onTouchError(String error);
    }
    
    public TouchControlHandler(Context context) {
        this.context = context;
    }
    
    public void setCallback(TouchControlCallback callback) {
        this.callback = callback;
    }
    
    /**
     * 设置无障碍服务实例
     */
    public void setAccessibilityService(RemoteAccessibilityService service) {
        this.accessibilityService = service;
        Log.d(TAG, "AccessibilityService已设置: " + (service != null ? "已连接" : "未连接"));
    }
    
    /**
     * 处理触摸事件（只包含坐标，执行点击操作）
     * @param x X坐标
     * @param y Y坐标
     */
    public void handleTouchEvent(float x, float y) {
        // 添加详细的触摸事件日志
        Log.d(TAG, String.format("👆 接收到触摸事件: 坐标=(%.1f, %.1f)", x, y));
        
        if (accessibilityService == null) {
            Log.w(TAG, "⚠️ 无障碍服务未可用，无法执行触摸操作");
            if (callback != null) {
                callback.onTouchError("无障碍服务未启用，无法执行触摸操作");
            }
            return;
        }
        
        // 执行点击操作
        executeTouchAction((int)x, (int)y);
    }
    
    /**
     * 处理SC_TOUCHED信号 - 执行触摸操作（保留原有方法兼容性）
     */
    // handleTouchMessage方法已移除 - 不再使用Binary Protocol
    // 现在直接使用handleTouchEvent(float x, float y)方法
    

    
    /**
     * 执行触摸操作（重载方法支持action和extraData）
     */
    private void executeTouchAction(int x, int y, String action, String extraData) {
        try {
            Log.v(TAG, String.format("执行触摸动作: %s at (%d, %d)", action, x, y));
            
            if (action == null) action = "click";
            
            switch (action.toLowerCase()) {
                case "click":
                case "tap":
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        executeGestureTouch(x, y);
                    } else {
                        executeLegacyTouch(x, y);
                    }
                    break;
                case "long_press":
                case "longpress":
                    executeLongPress(x, y);
                    break;
                case "swipe":
                    performSwipe(x, y, extraData);
                    break;
                case "double_click":
                case "doubleclick":
                    performDoubleClick(x, y);
                    break;
                default:
                    Log.w(TAG, "未知的触摸动作: " + action + "，默认执行点击");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        executeGestureTouch(x, y);
                    } else {
                        executeLegacyTouch(x, y);
                    }
                    break;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error executing touch action at (" + x + ", " + y + ")", e);
            if (callback != null) {
                callback.onTouchError("触摸操作执行失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 执行触摸操作（保留原有方法兼容性）
     */
    private void executeTouchAction(int x, int y) {
        executeTouchAction(x, y, "click", null);
    }
    
    /**
     * 执行滑动操作
     */
    private void performSwipe(int startX, int startY, String extraData) {
        try {
            // 解析终点坐标，格式：endX,endY[,duration]
            String[] parts = extraData != null ? extraData.split(",") : new String[0];
            if (parts.length < 2) {
                Log.w(TAG, "滑动操作缺少终点坐标: " + extraData);
                return;
            }
            
            int endX = Integer.parseInt(parts[0]);
            int endY = Integer.parseInt(parts[1]);
            int duration = parts.length > 2 ? Integer.parseInt(parts[2]) : 300; // 默认300ms
            
            executeSwipeGesture(startX, startY, endX, endY, duration);
            
        } catch (Exception e) {
            Log.e(TAG, "滑动操作解析失败: " + extraData, e);
        }
    }
    
    /**
     * 执行双击操作
     */
    private void performDoubleClick(int x, int y) {
        // 先执行第一次点击
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            executeGestureTouch(x, y);
        } else {
            executeLegacyTouch(x, y);
        }
        
        // 延迟后执行第二次点击
        new android.os.Handler().postDelayed(() -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                executeGestureTouch(x, y);
            } else {
                executeLegacyTouch(x, y);
            }
            Log.d(TAG, String.format("双击操作完成: (%d, %d)", x, y));
        }, 150); // 150ms间隔
    }
    
    /**
     * 使用GestureDescription执行触摸（Android 7.0+）
     */
    private void executeGestureTouch(int x, int y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 创建触摸手势路径
            Path touchPath = new Path();
            touchPath.moveTo(x, y);
            
            // 创建手势描述
            GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(
                touchPath, 0, 100); // 持续100ms的点击
            
            GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();
            
            // 执行手势
            boolean result = accessibilityService.dispatchGesture(gesture, new AccessibilityService.GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    Log.d(TAG, "Touch gesture completed at (" + x + ", " + y + ")");
                    if (callback != null) {
                        callback.onTouchExecuted(x, y, true);
                    }
                }
                
                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                    Log.w(TAG, "Touch gesture cancelled at (" + x + ", " + y + ")");
                    if (callback != null) {
                        callback.onTouchExecuted(x, y, false);
                    }
                }
            }, null);
            
            if (!result) {
                Log.e(TAG, "Failed to dispatch touch gesture");
                if (callback != null) {
                    callback.onTouchError("手势分发失败");
                }
            } else {
                Log.i(TAG, "Touch gesture dispatched successfully at (" + x + ", " + y + ")");
            }
        }
    }
    
    /**
     * 低版本Android的触摸实现
     */
    private void executeLegacyTouch(int x, int y) {
        // 对于较低版本的Android，可以使用AccessibilityNodeInfo进行点击
        try {
            AccessibilityNodeInfo rootNode = accessibilityService.getRootInActiveWindow();
            if (rootNode != null) {
                // 查找指定坐标的节点并执行点击
                AccessibilityNodeInfo targetNode = findNodeAtCoordinate(rootNode, x, y);
                if (targetNode != null && targetNode.isClickable()) {
                    boolean result = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    Log.i(TAG, "Legacy touch executed at (" + x + ", " + y + "), result: " + result);
                    
                    if (callback != null) {
                        callback.onTouchExecuted(x, y, result);
                    }
                } else {
                    Log.w(TAG, "No clickable node found at (" + x + ", " + y + ")");
                    if (callback != null) {
                        callback.onTouchError("指定坐标无可点击元素");
                    }
                }
            } else {
                Log.e(TAG, "Cannot get root node for legacy touch");
                if (callback != null) {
                    callback.onTouchError("无法获取屏幕根节点");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in legacy touch execution", e);
            if (callback != null) {
                callback.onTouchError("低版本触摸执行失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 查找指定坐标的节点
     */
    private AccessibilityNodeInfo findNodeAtCoordinate(AccessibilityNodeInfo node, int x, int y) {
        if (node == null) return null;
        
        android.graphics.Rect bounds = new android.graphics.Rect();
        node.getBoundsInScreen(bounds);
        
        // 检查当前节点是否包含目标坐标
        if (bounds.contains(x, y)) {
            // 先检查子节点
            for (int i = 0; i < node.getChildCount(); i++) {
                AccessibilityNodeInfo child = node.getChild(i);
                AccessibilityNodeInfo result = findNodeAtCoordinate(child, x, y);
                if (result != null) {
                    return result;
                }
            }
            
            // 如果没有找到合适的子节点，返回当前节点
            if (node.isClickable()) {
                return node;
            }
        }
        
        return null;
    }
    
    /**
     * 执行滑动手势
     */
    public void executeSwipeGesture(int startX, int startY, int endX, int endY, int duration) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && accessibilityService != null) {
            Path swipePath = new Path();
            swipePath.moveTo(startX, startY);
            swipePath.lineTo(endX, endY);
            
            GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(
                swipePath, 0, duration);
            
            GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();
            
            boolean result = accessibilityService.dispatchGesture(gesture, new AccessibilityService.GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    Log.d(TAG, "Swipe gesture completed from (" + startX + ", " + startY + ") to (" + endX + ", " + endY + ")");
                }
                
                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    Log.w(TAG, "Swipe gesture cancelled");
                }
            }, null);
            
            Log.i(TAG, "Swipe gesture dispatched: " + result);
        }
    }
    
    /**
     * 执行长按操作
     */
    public void executeLongPress(int x, int y) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && accessibilityService != null) {
            Path touchPath = new Path();
            touchPath.moveTo(x, y);
            
            // 长按持续1000ms
            GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(
                touchPath, 0, 1000);
            
            GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();
            
            accessibilityService.dispatchGesture(gesture, null, null);
            Log.i(TAG, "Long press executed at (" + x + ", " + y + ")");
        }
    }
    
    /**
     * 检查触摸控制是否可用
     */
    public boolean isTouchControlAvailable() {
        return accessibilityService != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }
}
