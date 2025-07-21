package com.zhongkong.app.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.InputType;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import com.zhongkong.app.R;

public class PasswordInputView extends RelativeLayout {
    
    private boolean isPassword = true;
    private EditText etPassword;
    private ImageView ivToggleVisibility;
    private int cursorPosition;
    
    public PasswordInputView(Context context) {
        super(context);
        init(context, null);
    }
    
    public PasswordInputView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    
    public PasswordInputView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }
    
    private void init(Context context, AttributeSet attrs) {
        LayoutInflater.from(context).inflate(R.layout.view_password_input, this, true);

        etPassword = findViewById(R.id.et_password);
        ivToggleVisibility = findViewById(R.id.iv_toggle_visibility);
        
        // 处理自定义属性
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PasswordInputView);
            String hint = a.getString(R.styleable.PasswordInputView_hint);
            isPassword = a.getBoolean(R.styleable.PasswordInputView_isPassword, true);

            setHint(hint);
            setPasswordMode(isPassword);

            a.recycle();
        }
        
        // 切换密码可见性
        ivToggleVisibility.setOnClickListener(v->{
            isPassword = !isPassword;
            setPasswordMode(isPassword);
        });
    }
    
    public void setHint(String hint) {
        etPassword.setHint(hint);
    }
    
    public void setPasswordMode(boolean isPassword) {
        if(isPassword){
            etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
        }else{
            etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        }

        ivToggleVisibility.setImageResource(isPassword ?
                R.drawable.ic_visibility_off:R.drawable.ic_visibility);

        // 恢复光标位置
        etPassword.setSelection(etPassword.getText().length());
    }

    public String getContent(){
        return etPassword.getText().toString();
    }

}