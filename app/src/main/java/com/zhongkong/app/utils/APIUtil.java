package com.zhongkong.app.utils;

import android.content.Context;

import com.afollestad.materialdialogs.MaterialDialog;
import com.blankj.utilcode.util.ToastUtils;
import com.lzy.okgo.OkGo;
import com.lzy.okgo.callback.StringCallback;
import com.lzy.okgo.model.Response;
import com.lzy.okgo.request.base.Request;
import com.zhongkong.app.R;

public class APIUtil {

    public static final String URL_BASE = "http://185.128.227.222:5558/";
    public static final String URL_LOGIN = URL_BASE+"api/login";

    public static void post(){

    }


    public static void postRequest(Context context,
                                   String url,
                                   String requestJson,
                                   RequestCallback callback) {
        // 1. 创建加载对话框
//        MaterialDialog progressDialog = createLoadingDialog(context);

        // 2. 发送POST请求
        OkGo.<String>post(url)
                .tag(context) // 绑定上下文，防止内存泄漏
                .upJson(requestJson) // 发送JSON数据
                .execute(new StringCallback() {

                    // 3. 开始请求时显示加载框
                    @Override
                    public void onStart(Request<String, ? extends Request> request) {
                        super.onStart(request);
//                        if (progressDialog != null && !progressDialog.isShowing()) {
//                            progressDialog.show();
//                        }
                    }
                    // 4. 请求成功回调
                    @Override
                    public void onSuccess(Response<String> response) {
//                        if (progressDialog != null && progressDialog.isShowing()) {
//                            progressDialog.dismiss();
//                        }
                        callback.onSuccess(response.body());
                    }
                    // 5. 请求失败回调
                    @Override
                    public void onError(Response<String> response) {
                        super.onError(response);
//                        if (progressDialog != null && progressDialog.isShowing()) {
//                            progressDialog.dismiss();
//                        }

                        String errorMessage = "请求失败";
                        if (response.getException() != null) {
                            errorMessage += ": " + response.getException().getMessage();
                        }

                        ToastUtils.showLong("error code:"+response.code()+",message:"+ errorMessage);
                    }
                });
    }
    // 创建加载对话框
//    private static MaterialDialog createLoadingDialog(Context context) {
//        return new MaterialDialog(context, MaterialDialog.getDEFAULT_BEHAVIOR())
//                .title(R.string.loading_title)
//                .message(R.string.loading_message)
//                .progress(true, 0)
//                .cancelable(false)
//                .progressIndeterminateStyle(false);
//    }
    // 回调接口
    public interface RequestCallback {
        void onSuccess(String response);
    }

}
