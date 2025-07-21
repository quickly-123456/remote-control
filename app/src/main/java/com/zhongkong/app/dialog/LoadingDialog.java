package com.zhongkong.app.dialog;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.zhongkong.app.R;

public class LoadingDialog {
    private Dialog mLoadingDialog;

    public LoadingDialog(Context context) {
        this(context, null);
    }

    /**
     * @param context
     * @param message dialog中文字
     */
    public LoadingDialog(Context context, String message) {
        this(context, message, true);
    }

    /**
     * @param context
     * @param message dialog中文字
     */
    public LoadingDialog(Context context, String message, boolean cancleLoadingEnable) {
        // 首先得到整个View
        View view = LayoutInflater.from(context).inflate(
                R.layout.dialog_loading, null);
        // 获取整个布局
        LinearLayout layout = view.findViewById(R.id.dialog_loading_view);
        // 页面中显示文本
        TextView loadingText = view.findViewById(R.id.tipTextView);
        // 显示文本
        if (!TextUtils.isEmpty(message)) {
            loadingText.setText(message);
        }
        // 创建自定义样式的Dialog
        mLoadingDialog = new Dialog(context);
        mLoadingDialog.setCancelable(cancleLoadingEnable);
        mLoadingDialog.setCanceledOnTouchOutside(cancleLoadingEnable);

        mLoadingDialog.setContentView(layout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
    }


    public void showDialog() {
        try {
            if (mLoadingDialog != null && !mLoadingDialog.isShowing()) {
                mLoadingDialog.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void cancelDialog() {
        try {
            if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
                mLoadingDialog.dismiss();
            }
            mLoadingDialog = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}