package com.zhongkong.app.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.ThreadUtils;
import com.zhongkong.app.databinding.ActivityProjectionScreenBinding;
import com.zhongkong.app.utils.RDTUtil;
import com.zhongkong.app.utils.WebSocketManager;

public class ProjectionScreenActivity extends AppCompatActivity {
    private static final String TAG = "ProjectionScreenActivity";
    private WebSocketManager webSocketManager;
    private ActivityProjectionScreenBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProjectionScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.ivClose.setOnClickListener(v -> finish());

        webSocketManager = WebSocketManager.getInstance();
        webSocketManager.init(new WebSocketManager.WebSocketListener() {
            @Override
            public void onConnected() {
                webSocketManager.getWebSocketClient().send(RDTUtil.getCsMobileAdminSendData());
            }

            @Override
            public void onDisconnected(int code, String reason, boolean remote) {
            }

            @Override
            public void onMessage(String message) {
                Log.d(TAG, "onMessage:" + message);
            }

            @Override
            public void onBinaryMessage(byte[] data) {
                int wsCommand = RDTUtil.getWsCommand(data);
                Log.d(TAG, "wsCommand:" + wsCommand);
                switch (wsCommand) {
                    case RDTUtil.SC_SCREEN:
                        ThreadUtils.executeByCached(new ThreadUtils.SimpleTask<Bitmap>() {
                            @Override
                            public Bitmap doInBackground() throws Throwable {
                                byte[] webpData = RDTUtil.getScreenWebpData(data);
                                return BitmapFactory.decodeByteArray(webpData, 0, webpData.length);
                            }

                            @Override
                            public void onSuccess(Bitmap result) {
                                binding.imgScreen.updateBitmap(result);
                            }
                        });
                        break;
                }
            }

            @Override
            public void onError(Exception ex) {
            }
        });
        webSocketManager.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webSocketManager.disconnect();
    }
}
