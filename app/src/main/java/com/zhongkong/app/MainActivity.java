package com.zhongkong.app;

import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.ActivityUtils;
import com.blankj.utilcode.util.BarUtils;
import com.orhanobut.hawk.Hawk;
import com.zhongkong.app.activity.UpdatePasswordActivity;
import com.zhongkong.app.adapter.DeviceAdapter;
import com.zhongkong.app.databinding.ActivityMainBinding;
import com.zhongkong.app.model.Device;
import com.zhongkong.app.utils.ConstantUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    private DeviceAdapter adapter;
    private List<Device> deviceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());


        initView();
        initData();

    }

    private void initData() {
        binding.appBarMain.bottomNavigation.setBadge(0,  deviceList.size());
        binding.appBarMain.bottomNavigation.setBadge(1, (int) deviceList.stream().filter(Device::isConnected).count());
        binding.appBarMain.bottomNavigation.setBadge(2, (int) deviceList.stream().filter(device -> !device.isConnected()).count());
    }

    private void initView() {
        BarUtils.transparentStatusBar(this);
        BarUtils.setStatusBarLightMode(this, true);
        binding.navView.setPadding(0, BarUtils.getStatusBarHeight(), 0, 0);
        binding.appBarMain.imgDrawSwitch.setOnClickListener(v -> {
            binding.drawerLayout.openDrawer(GravityCompat.START);
        });
        binding.imgNavClose.setOnClickListener(v->{
            binding.drawerLayout.close();
        });
        binding.tvExitSystem.setOnClickListener(v->{
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.tv_click_anim);
            v.startAnimation(anim);
            Hawk.put(ConstantUtil.HAWK_USER_ID,0);
            finish();
        });
        binding.tvUpdatePassword.setOnClickListener(v->{
            Animation anim = AnimationUtils.loadAnimation(this, R.anim.tv_click_anim);
            v.startAnimation(anim);
            ActivityUtils.startActivity(UpdatePasswordActivity.class);
        });

        binding.appBarMain.bottomNavigation.setOnItemSelectedListener(position -> {
            switch (position){
                case 0:
                    adapter.updateData(deviceList);
                    break;
                case 1:
                    adapter.updateData(deviceList.stream().filter(Device::isConnected).collect(Collectors.toList()));
                    break;
                case 2:
                    adapter.updateData(deviceList.stream().filter(device -> !device.isConnected()).collect(Collectors.toList()));
                    break;
            }
        });

        RecyclerView recyclerView = binding.appBarMain.recyclerView;
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        deviceList = new ArrayList<>();
        deviceList.add(new Device("Redmi-K40", "15825859652", false));
        deviceList.add(new Device("Redmi-K40", "15825859652", true));
        deviceList.add(new Device("Redmi-K40", "15825859652", false));
        deviceList.add(new Device("Redmi-K40", "15825859652", true));
        deviceList.add(new Device("Redmi-K40", "15825859652", true));
        deviceList.add(new Device("Redmi-K40", "15825859652", true));
        deviceList.add(new Device("Redmi-K40", "15825859652", true));
        deviceList.add(new Device("Redmi-K40", "15825859652", true));

        adapter = new DeviceAdapter(this, deviceList);
        recyclerView.setAdapter(adapter);
    }
}