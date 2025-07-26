package com.zhongkong.app.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

public class StreamingSurfaceView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "StreamingSurfaceView";

    // 核心状态变量
    private Bitmap currentBitmap;
    private OnSurfaceViewClickListener clickListener;
    private final Object bitmapLock = new Object();

    // 尺寸信息和点击坐标计算
    private float aspectRatio = 1.0f;   // 图片宽高比
    private RectF imageDrawRegion = new RectF(); // 图片绘制区域（相对于整个视图）

    // 接口定义：归一化坐标监听器
    public interface OnSurfaceViewClickListener {
        void onClick(float normalizedX, float normalizedY);
    }

    public StreamingSurfaceView(Context context) {
        super(context);
        init();
    }

    public StreamingSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        // 初始化Surface和事件处理
        getHolder().addCallback(this);
        setZOrderOnTop(true); // 确保Surface在其它视图上方
        getHolder().setFormat(android.graphics.PixelFormat.TRANSLUCENT);
        setClickable(true);
    }

    // 设置点击事件监听器
    public void setOnSurfaceViewClickListener(OnSurfaceViewClickListener listener) {
        this.clickListener = listener;
    }

    // 更新图片流的主方法
    public void updateBitmap(Bitmap bitmap) {
        if (bitmap == null) return;

        synchronized (bitmapLock) {
            // 1. 提取图片宽高比
            aspectRatio = (float) bitmap.getHeight() / (float) bitmap.getWidth();

            // 2. 更新当前图片
            if (currentBitmap != null && !currentBitmap.isRecycled()) {
                currentBitmap.recycle();
            }
            currentBitmap = bitmap;

            // 3. 计算新尺寸（高度固定，宽度自适应）
            post(() -> {
                // 检查是否需要调整视图尺寸
                adjustViewSizeToFit();
                // 请求重绘
                redraw();
            });
        }
    }

    // 调整视图尺寸以适应图片比例
    private void adjustViewSizeToFit() {
        // 检查高度是否有效
        int currentHeight = getHeight();
        if (currentHeight <= 0) {
            // 如果高度还未测量，设置默认值
            ViewGroup.LayoutParams params = getLayoutParams();
            params.height = 300; // 默认高度
            setLayoutParams(params);
            return;
        }

        // 计算新宽度（根据当前高度和图片宽高比）
        int newWidth = (int) (currentHeight / aspectRatio);

        // 更新视图尺寸
        ViewGroup.LayoutParams params = getLayoutParams();
        if (params.width != newWidth) {
            params.width = newWidth; // 设置新的宽度
            setLayoutParams(params);
            requestLayout();
        }
    }

    // 核心绘制方法
    private void redraw() {
        synchronized (bitmapLock) {
            if (currentBitmap == null || currentBitmap.isRecycled())
                return;

            // 获取并锁定画布
            Canvas canvas = getHolder().lockCanvas();
            if (canvas == null) return;

            try {
                // 清除画布（透明背景）
                canvas.drawColor(0x00000000);

                // 计算绘制区域（全视图填充）
                imageDrawRegion.set(0, 0, canvas.getWidth(), canvas.getHeight());

                // 绘制整个图片（根据当前视图大小自适应）
                canvas.drawBitmap(currentBitmap,
                        null, // 完整图片
                        imageDrawRegion, // 整个视图区域
                        null);

            } finally {
                // 解锁并提交画布
                getHolder().unlockCanvasAndPost(canvas);
            }
        }
    }

    // 点击事件处理
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {

            // 如果没有图片或者图片已经被回收，则忽略点击事件
            synchronized (bitmapLock) {
                if (currentBitmap == null || currentBitmap.isRecycled()) {
                    // 可以选择不处理，返回false，让事件传递给其它视图
                    return false;
                }
            }

            // 获取视图内的点击坐标
            float x = event.getX();
            float y = event.getY();

            // 计算归一化坐标 (0-1范围)
            float relativeX = x / getWidth();
            float relativeY = y / getHeight();

            // 记录调试信息
            Log.d(TAG, String.format(
                    "Clicked at (%.1f, %.1f) -> Normalized: (%.2f, %.2f)",
                    x, y, relativeX, relativeY));

            // 回调点击监听器
            if (clickListener != null) {
                clickListener.onClick(relativeX, relativeY);
            }

            return true;
        }
        return super.onTouchEvent(event);
    }

    // Surface生命周期回调
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        redraw();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // 调整视图大小后重绘
        post(() -> {
            adjustViewSizeToFit();
            redraw();
        });
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}

    // 资源清理方法
    public void release() {
        synchronized (bitmapLock) {
            if (currentBitmap != null && !currentBitmap.isRecycled()) {
                currentBitmap.recycle();
                currentBitmap = null;
            }
        }
    }

    // 获取当前图片宽高比
    public float getAspectRatio() {
        return aspectRatio;
    }

    // 获取图片绘制区域
    public RectF getDrawRegion() {
        return new RectF(imageDrawRegion);
    }
}
