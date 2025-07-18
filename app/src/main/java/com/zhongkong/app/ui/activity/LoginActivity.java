package com.zhongkong.app.ui.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSONObject;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.google.gson.JsonObject;
import com.orhanobut.hawk.Hawk;
import com.zhongkong.app.MainActivity;
import com.zhongkong.app.databinding.ActivityLoginBinding;
import com.zhongkong.app.utils.APIUtil;
import com.zhongkong.app.utils.ConstantUtil;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
        BarUtils.transparentStatusBar(this.getWindow());
        BarUtils.setStatusBarLightMode(this.getWindow(),true);

        initView();
        initData();

    }

    private void initData() {
        if(Hawk.get(ConstantUtil.HAWK_USER_ID, 0)>0){
            gotoMainActivity();
        }
    }

    private void initView() {
        binding.ivToggleVisibility.setOnClickListener(v->{
            isPasswordVisible = !isPasswordVisible;
            if(isPasswordVisible){
                binding.etPassword.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            }else{
                binding.etPassword.setTransformationMethod(PasswordTransformationMethod.getInstance());
            }
        });

        binding.loginButton.setOnClickListener(v->{
            String phone = binding.etUsername.getText().toString();
            String password = binding.etPassword.getText().toString();
            if(phone.isEmpty() || password.isEmpty()){
                ToastUtils.showShort("账号和密码不能为空");
                return;
            }
            JSONObject postData = new JSONObject();
            postData.put("phone",phone);
            postData.put("password",password);
            APIUtil.postRequest(this, APIUtil.URL_LOGIN, postData.toJSONString(), response -> {
                try{
                    JSONObject body = JSONObject.parseObject(response);
                    JSONObject user = body.getJSONObject("user");
                    int userId = Integer.parseInt(user.getString("id"));
                    if(userId>0){
                        Hawk.put(ConstantUtil.HAWK_USER_ID,userId);
                        gotoMainActivity();
                    }
                }catch (Exception e){
                    ToastUtils.showShort("登录失败:"+e.getMessage());
                }
            });
        });
    }

    private void gotoMainActivity(){
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
