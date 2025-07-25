package com.zhongkong.app.ui.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

// 创建自定义 SurfaceView
public class StreamingSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
    
    private Bitmap currentBitmap;
    private final Object bitmapLock = new Object();
    
    public StreamingSurfaceView(Context context) {
        super(context);
        getHolder().addCallback(this);
    }

    public StreamingSurfaceView(Context context, AttributeSet attrs){
        super(context, attrs);
        getHolder().addCallback(this);
    }
    
    public void updateBitmap(Bitmap bitmap) {
        synchronized (bitmapLock) {
            if (currentBitmap != null && !currentBitmap.isRecycled()) {
                currentBitmap.recycle();
            }
            currentBitmap = bitmap;
        }
        redraw();
    }
    
    private void redraw() {
        Canvas canvas = getHolder().lockCanvas();
        if (canvas != null) {
            synchronized (bitmapLock) {
                if (currentBitmap != null && !currentBitmap.isRecycled()) {
                    // 计算缩放比例
                    float scale = Math.min(
                        (float) canvas.getWidth() / currentBitmap.getWidth(),
                        (float) canvas.getHeight() / currentBitmap.getHeight()
                    );
                    
                    // 居中绘制
                    float dx = (canvas.getWidth() - currentBitmap.getWidth() * scale) / 2;
                    float dy = (canvas.getHeight() - currentBitmap.getHeight() * scale) / 2;
                    
                    canvas.save();
                    canvas.translate(dx, dy);
                    canvas.scale(scale, scale);
                    canvas.drawBitmap(currentBitmap, 0, 0, null);
                    canvas.restore();
                }
            }
            getHolder().unlockCanvasAndPost(canvas);
        }
    }
    
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        redraw();
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        redraw();
    }
    
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}
}