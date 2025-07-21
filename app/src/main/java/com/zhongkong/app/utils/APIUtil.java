package com.zhongkong.app.utils;

import android.content.Context;
import android.os.SystemClock;

import com.afollestad.materialdialogs.MaterialDialog;
import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.base.Request;
import com.zhongkong.app.R;
import com.zhongkong.app.dialog.LoadingDialog;

public class APIUtil {

    public static final String URL_BASE = "http://185.128.227.222:5558/";
    public static final String URL_LOGIN = URL_BASE+"api/login";
    public static final String URL_UPDATE_PASSWORD = URL_BASE+"api/updatepassword";

    public static void postRequest(Context context,
                                   String url,
                                   String requestJson,
                                   RequestCallback callback) {
        OkGo.<String>post(url)
                .tag(context) // 绑定上下文，防止内存泄漏
                .upJson(requestJson) // 发送JSON数据
                .execute(new StringCallback() {

                    // 3. 开始请求时显示加载框
                    @Override
                    public void onStart(Request<String, ? extends Request> request) {
                        super.onStart(request);

                    }
                    // 4. 请求成功回调
                    @Override
                    public void onSuccess(Response<String> response) {
                        callback.onSuccess(response.body());
                    }
                    // 5. 请求失败回调
                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        String errorMessage = "请求失败";
                        if (response.getException() != null) {
                            errorMessage += ": " + response.getException().getMessage();
                        }

                        ToastUtils.showLong("error code:"+response.code()+",message:"+ errorMessage);
                    }
                });
    }

    public interface RequestCallback {
        void onSuccess(String response);
    }

    public static void postRequestWithLoadDialog(Context context,
                                   String url,
                                   String requestJson,
                                   RequestCallback callback) {
        LoadingDialog dialog = new LoadingDialog(context);

        OkGo.<String>post(url)
                .tag(context) // 绑定上下文，防止内存泄漏
                .upJson(requestJson) // 发送JSON数据
                .execute(new StringCallback() {

                    // 3. 开始请求时显示加载框
                    @Override
                    public void onStart(Request<String, ? extends Request> request) {
                        super.onStart(request);
                        dialog.showDialog();

                    }
                    // 4. 请求成功回调
                    @Override
                    public void onSuccess(Response<String> response) {
                        dialog.cancelDialog();
                        callback.onSuccess(response.body());
                    }
                    // 5. 请求失败回调
                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
                        dialog.cancelDialog();

                        String errorMessage = "请求失败";
                        if (response.getException() != null) {
                            errorMessage += ": " + response.getException().getMessage();
                        }

                        ToastUtils.showLong("error code:"+response.code()+",message:"+ errorMessage);
                    }
                });

    }

}
