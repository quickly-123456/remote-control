package com.zhongkong.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.ThreadUtils;
import com.orhanobut.hawk.Hawk;
import com.scwang.smart.refresh.header.ClassicsHeader;
import com.zhongkong.app.activity.ProjectionScreenActivity;
import com.zhongkong.app.activity.UpdatePasswordActivity;
import com.zhongkong.app.adapter.DeviceAdapter;
import com.zhongkong.app.databinding.ActivityMainBinding;
import com.zhongkong.app.model.Device;
import com.zhongkong.app.utils.ConstantUtil;
import com.zhongkong.app.utils.RDTUtil;
import com.zhongkong.app.utils.WebSocketManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private DeviceAdapter adapter;
    private List<Device> deviceList = new ArrayList<>();
    ;
    private WebSocketManager webSocketManager;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        BarUtils.transparentStatusBar(this);
        BarUtils.setStatusBarLightMode(this, true);
        binding.navView.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);

        initView();
        initData();
    }

    private void initData() {
//        handWebsocket();
    }

    private void initView() {
        binding.appMain.imgDrawSwitch.setOnClickListener(v -> {
            binding.drawerLayout.openDrawer(GravityCompat.START);
        });
        binding.imgNavClose.setOnClickListener(v -> {
            binding.drawerLayout.close();
        });
        binding.tvExitSystem.setOnClickListener(v -> {
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.tv_click_anim);
            v.startAnimation(anim);
            Hawk.put(ConstantUtil.HAWK_USER_ID, 0);
            finish();
        });
        binding.tvUpdatePassword.setOnClickListener(v -> {
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.tv_click_anim);
            v.startAnimation(anim);
            ActivityUtils.startActivity(UpdatePasswordActivity.class);
        });

        binding.appMain.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<Device> devices = getBottomItemDeviceList(binding.appMain.bottomNavigation.getCurrentPosition());
                if (!s.toString().isEmpty()) {
                    adapter.updateData(devices.stream().filter(device -> device.getPhoneNumber().contains(s.toString())).collect(Collectors.toList()));
                } else {
                    adapter.updateData(devices);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        binding.appMain.bottomNavigation.setOnItemSelectedListener(position -> {
            adapter.updateData(getBottomItemDeviceList(position));
            binding.appMain.etSearch.setText("");
            binding.appMain.etSearch.clearFocus();
        });

        binding.appMain.refreshLayout.setRefreshHeader(new ClassicsHeader(this));
        binding.appMain.refreshLayout.setOnRefreshListener(refreshLayout -> {
            //    //获取最新设备信息
            if (webSocketManager.isConnected()) {
                webSocketManager.getWebSocketClient().send(RDTUtil.generateCsMobileAdminSendData());
            }
            refreshLayout.finishRefresh();
        });

        RecyclerView recyclerView = binding.appMain.recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));


        adapter = new DeviceAdapter(this, deviceList);
        adapter.setOnItemClickListener((resId, position) -> {
            if (resId == R.id.btn_projection_screen) {
                Intent intent = new Intent(MainActivity.this, ProjectionScreenActivity.class);
                intent.putExtra("device_info", deviceList.get(position));
                startActivity(intent);
            }
        });
        recyclerView.setAdapter(adapter);
    }

    public void updateDeviceList(List<Device> devices) {
        Log.d(TAG, "updateDeviceList:" + devices);
        deviceList = devices;
        ThreadUtils.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.updateData(deviceList);
                binding.appMain.bottomNavigation.setBadge(0, deviceList.size());
                binding.appMain.bottomNavigation.setBadge(1, (int) deviceList.stream().filter(Device::isConnected).count());
                binding.appMain.bottomNavigation.setBadge(2, (int) deviceList.stream().filter(device -> !device.isConnected()).count());
            }
        });
    }

    private void handWebsocket() {
        webSocketManager = WebSocketManager.getInstance();
        webSocketManager.init(new WebSocketManager.WebSocketListener() {
            @Override
            public void onConnected() {
                webSocketManager.getWebSocketClient().send(RDTUtil.generateCsMobileAdminSendData());
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
                if (wsCommand == RDTUtil.SC_MOBILE_ADMIN) {
                    updateDeviceList(RDTUtil.getDevices(data));
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
    protected void onResume() {
        super.onResume();
        handWebsocket();
    }

    private List<Device> getBottomItemDeviceList(int position) {
        switch (position) {
            case 0:
                return deviceList;
            case 1:
                return deviceList.stream().filter(Device::isConnected).collect(Collectors.toList());
            case 2:
                return deviceList.stream().filter(device -> !device.isConnected()).collect(Collectors.toList());
        }
        return deviceList;
    }

}