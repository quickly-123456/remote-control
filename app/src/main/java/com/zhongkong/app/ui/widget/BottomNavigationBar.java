package com.zhongkong.app.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.zhongkong.app.R;

import java.util.ArrayList;
import java.util.List;

public class BottomNavigationBar extends LinearLayout {
    
    private int selectedItem = 0;
    private int itemCount = 4;
    private OnItemSelectedListener listener;
    private List<BottomNavigationItem> items = new ArrayList<>();
    
    // 自定义属性
    public BottomNavigationBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    
    private void init(Context context, AttributeSet attrs) {
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER);
        
        // 获取自定义属性
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.BottomNavigationBar);
            itemCount = a.getInt(R.styleable.BottomNavigationBar_itemCount, 4);
            selectedItem = a.getInt(R.styleable.BottomNavigationBar_selectedItem, 0);
            a.recycle();
        }
        
        // 添加导航项
        for (int i = 0; i < itemCount; i++) {
            BottomNavigationItem item = new BottomNavigationItem(context);
            LayoutParams params = new LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
            item.setLayoutParams(params);
            final int position = i;
            item.setOnClickListener(v -> {
                setSelectedItem(position);
                if (listener != null) {
                    listener.onItemSelected(position);
                }
            });
            addView(item);
            items.add(item);
        }
        
        // 设置初始选中项
        setSelectedItem(selectedItem);
    }
    
    public void setSelectedItem(int position) {
        if (position < 0 || position >= items.size()) return;
        
        items.get(selectedItem).setSelected(false);
        items.get(position).setSelected(true);
        selectedItem = position;
    }
    
    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        this.listener = listener;
    }
    
    public void setBadge(int position, int count) {
        if (position < 0 || position >= items.size()) return;
        items.get(position).setBadge(count);
    }
    
    public void clearBadge(int position) {
        if (position < 0 || position >= items.size()) return;
        items.get(position).clearBadge();
    }
    
    public interface OnItemSelectedListener {
        void onItemSelected(int position);
    }
}