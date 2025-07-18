package com.zhongkong.app;

import android.os.Bundle;
import android.view.View;
import android.view.Menu;

import com.blankj.utilcode.util.BarUtils;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.navigation.NavigationView;

import androidx.core.view.GravityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.appcompat.app.AppCompatActivity;

import com.zhongkong.app.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        BarUtils.transparentStatusBar(this);
        BarUtils.setStatusBarLightMode(this,true);


        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;

        binding.appBarMain.imgDrawSwitch.setOnClickListener(v -> {
            drawer.openDrawer(GravityCompat.START);
        });

    }


}