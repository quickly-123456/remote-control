package com.zhongkong.app.ui.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.zhongkong.app.R;

public class BottomNavigationItem extends FrameLayout {
    
    private static final int ANIM_DURATION = 300;
    private static final float SELECTED_SCALE = 1.1f;
    private static final float UNSELECTED_SCALE = 1.0f;
    private static final int SELECTED_COLOR = Color.BLACK;
    private static final int UNSELECTED_COLOR = Color.parseColor("#718096");
    
    private ImageView icon;
    private TextView label;
    private TextView badge;
    private View indicator;
    private boolean isSelected = false;
    
    // 图标和标签资源
    private int[] icons = {
            R.drawable.icon_home,
            R.drawable.icon_home,
            R.drawable.icon_home
    };
    
    private int[] labels = {
            R.string.bottom_nav_all,
            R.string.bottom_nav_connected,
            R.string.bottom_nav_unconnected
    };
    
    public BottomNavigationItem(Context context) {
        super(context);
        init(context);
    }
    
    public BottomNavigationItem(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.bottom_navigation_item, this, true);

        icon = findViewById(R.id.icon);
        label = findViewById(R.id.label);
        badge = findViewById(R.id.badge);
        indicator = findViewById(R.id.indicator);
        
        // 初始状态
        setScaleX(UNSELECTED_SCALE);
        setScaleY(UNSELECTED_SCALE);
        label.setTextColor(UNSELECTED_COLOR);
    }
    
    public void setPosition(int position) {
        if (position < icons.length) {
            icon.setImageResource(icons[position]);
            label.setText(labels[position]);
        }
    }
    
    public void setSelected(boolean selected) {
        isSelected = selected;
        updateState();
    }
    
    private void updateState() {
        // 动画效果
        animate().scaleX(isSelected ? SELECTED_SCALE : UNSELECTED_SCALE)
                .scaleY(isSelected ? SELECTED_SCALE : UNSELECTED_SCALE)
                .setDuration(ANIM_DURATION)
                .start();
        
        // 颜色变化
        label.setTextColor(isSelected ? SELECTED_COLOR : UNSELECTED_COLOR);
        icon.setColorFilter(isSelected ? SELECTED_COLOR : UNSELECTED_COLOR, PorterDuff.Mode.SRC_IN);
        
        // 指示器显示
        indicator.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
        indicator.setAlpha(isSelected ? 1f : 0f);
        indicator.animate()
                .scaleX(isSelected ? 1f : 0.5f)
                .scaleY(isSelected ? 1f : 0.5f)
                .setDuration(ANIM_DURATION)
                .start();
    }
    
    public void setBadge(int count) {
        if (count <= 0) {
            clearBadge();
            return;
        }
        
        badge.setVisibility(VISIBLE);
        badge.setText(count > 99 ? "99+" : String.valueOf(count));
        
        // 动画效果
        badge.setScaleX(0);
        badge.setScaleY(0);
        badge.animate()
                .scaleX(1)
                .scaleY(1)
                .setDuration(ANIM_DURATION)
                .start();
    }
    
    public void clearBadge() {
        badge.setVisibility(GONE);
    }
    
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // 设置位置（在添加到父容器后）
        if (getParent() instanceof ViewGroup) {
            int position = ((ViewGroup) getParent()).indexOfChild(this);
            setPosition(position);
        }
    }
}