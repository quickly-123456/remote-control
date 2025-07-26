package com.zhongkong.app.activity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blankj.utilcode.util.ThreadUtils;
import com.blankj.utilcode.util.ToastUtils;
import com.zhongkong.app.databinding.ActivityProjectionScreenBinding;
import com.zhongkong.app.model.Device;
import com.zhongkong.app.ui.widget.StreamingSurfaceView;
import com.zhongkong.app.utils.RDTUtil;
import com.zhongkong.app.utils.WebSocketManager;

public class ProjectionScreenActivity extends AppCompatActivity {
    private static final String TAG = "ProjectionScreenActivity";
    private WebSocketManager webSocketManager;
    private ActivityProjectionScreenBinding binding;
    private Device device;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProjectionScreenBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        device = getIntent().getParcelableExtra("device_info");

        binding.tvName.setText(device.getPhoneNumber());

        binding.ivClose.setOnClickListener(v -> finish());
        binding.llRefresh.setOnClickListener(v -> {
//            webSocketManager.disconnect();
//            webSocketManager.connect();
//            ToastUtils.showShort("刷新完成");
        });

        binding.imgScreen.setOnSurfaceViewClickListener((normalizedX, normalizedY) -> {
            ToastUtils.showLong("点击了：" + normalizedX + ":" + normalizedY);
            int x = (int) (normalizedX * 10000);
            int y = (int) (normalizedY * 10000);
            Log.d(TAG, "x:" + normalizedX + ",y:" + normalizedY+"\n"+
                    "x:" + x + ",y:" + y );
            webSocketManager.getWebSocketClient().send(RDTUtil.generateCsTouchedSendData(x, y));
        });

        handWebsocket();
    }

    private void handWebsocket(){
        webSocketManager = WebSocketManager.getInstance();
        webSocketManager.init(new WebSocketManager.WebSocketListener() {
            @Override
            public void onConnected() {}

            @Override
            public void onDisconnected(int code, String reason, boolean remote) {}

            @Override
            public void onMessage(String message) {
                Log.d(TAG, "onMessage:" + message);
            }

            @Override
            public void onBinaryMessage(byte[] data) {
                int wsCommand = RDTUtil.getWsCommand(data);
                Log.d(TAG, "wsCommand:" + wsCommand);
                if (wsCommand == RDTUtil.SC_SCREEN) {
                    ThreadUtils.executeByCached(new ThreadUtils.SimpleTask<Bitmap>() {
                        @Override
                        public Bitmap doInBackground() throws Throwable {
                            byte[] webpData = RDTUtil.getScreenWebpData(device.getPhoneNumber(),data);
                            if(webpData!=null){
                                return BitmapFactory.decodeByteArray(webpData, 0, webpData.length);
                            }else{
                                return null;
                            }
                        }

                        @Override
                        public void onSuccess(Bitmap result) {
                            if(result != null) binding.imgScreen.updateBitmap(result);
                        }
                    });
                }
            }

            @Override
            public void onError(Exception ex) {
            }
        });
        if(!webSocketManager.isConnected()){
            webSocketManager.connect();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        webSocketManager.disconnect();
    }
}
