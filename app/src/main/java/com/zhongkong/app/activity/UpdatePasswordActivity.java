package com.zhongkong.app.activity;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.alibaba.fastjson2.JSONObject;
import com.blankj.utilcode.util.ToastUtils;
import com.orhanobut.hawk.Hawk;
import com.zhongkong.app.databinding.ActivityUpdatePasswordBinding;
import com.zhongkong.app.utils.APIUtil;
import com.zhongkong.app.utils.ConstantUtil;

public class UpdatePasswordActivity extends AppCompatActivity {
    private ActivityUpdatePasswordBinding binding;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUpdatePasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initView();
    }

    private void initView() {
        binding.imgBack.setOnClickListener(v -> {
            finish();
        });
        binding.loginButton.setOnClickListener(v -> {
            String oldPassword = binding.oldPassword.getContent();
            String newPassword = binding.newPassword.getContent();
            String confirmPassword = binding.confirmPassword.getContent();
            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                ToastUtils.showShort("请填写完整");
                return;
            }
            if (!newPassword.equals(confirmPassword)) {
                ToastUtils.showShort("两次密码不一致");
                return;
            }

            JSONObject postData = new JSONObject();
            postData.put("phone", Hawk.get(ConstantUtil.HAWK_USER_PHONE));
            postData.put("old_password", oldPassword);
            postData.put("new_password", newPassword);
            APIUtil.postRequestWithLoadDialog(this, APIUtil.URL_UPDATE_PASSWORD, postData.toJSONString(), response -> {
                try {
                    JSONObject body = JSONObject.parseObject(response);
                    ToastUtils.showShort(body.getString("message"));
                } catch (Exception e) {
                    ToastUtils.showShort("修改失败:" + e.getMessage());
                }
            });
        });

    }


}
