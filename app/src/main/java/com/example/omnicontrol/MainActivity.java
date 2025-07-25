package com.example.omnicontrol;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.NavDestination;

import com.example.omnicontrol.utils.PermissionManager;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_MICROPHONE_PERMISSION = 1001;
    private static final int REQUEST_CAMERA_PERMISSION = 1002;
    
    // 单例引用，用于权限请求
    private static MainActivity instance;

    private NavController navController;
    private View bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // 设置单例引用用于权限请求
        instance = this;
        
        // 延迟初始化NavController，确保Fragment已经创建
        findViewById(R.id.nav_host_fragment).post(() -> {
            try {
                navController = Navigation.findNavController(this, R.id.nav_host_fragment);
                
                // 设置导航监听器来控制TabBar的显示隐藏
                navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                    handleTabBarVisibility(destination.getId());
                });
                
            } catch (IllegalStateException e) {
                // NavController 还未准备好，忽略错误
                e.printStackTrace();
            }
        });
    }
    
    /**
     * 控制TabBar的显示和隐藏
     * @param destinationId 目标页面ID
     */
    private void handleTabBarVisibility(int destinationId) {
        // 获取MainContainerFragment中的BottomNavigationView
        // 这个方法需要在MainContainerFragment可见时才能工作
        if (destinationId == R.id.connectionHistoryFragment || 
            destinationId == R.id.updatePasswordFragment) {
            // 隐藏TabBar的页面
            hideTabBar();
        } else {
            // 显示TabBar的页面
            showTabBar();
        }
    }
    
    /**
     * 隐藏TabBar
     */
    public void hideTabBar() {
        // 这个方法将在子Fragment中调用
    }
    
    /**
     * 显示TabBar
     */
    public void showTabBar() {
        // 这个方法将在子Fragment中调用
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        // 简化导航处理，不使用ActionBar
        if (navController != null) {
            return navController.navigateUp() || super.onSupportNavigateUp();
        }
        return super.onSupportNavigateUp();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清除单例引用
        if (instance == this) {
            instance = null;
        }
    }
    
    /**
     * 获取MainActivity实例用于权限请求
     */
    public static MainActivity getInstance() {
        return instance;
    }
    
    /**
     * 请求麦克风权限（显示系统对话框）
     */
    public void requestMicrophonePermission() {
        Log.i(TAG, "🎤 请求麦克风权限对话框...");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.RECORD_AUDIO}, 
                    REQUEST_MICROPHONE_PERMISSION);
        } else {
            Log.i(TAG, "✅ 麦克风权限已授予");
        }
    }
    
    /**
     * 请求摄像头权限（显示系统对话框）
     */
    public void requestCameraPermission() {
        Log.i(TAG, "📷 请求摄像头权限对话框...");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.CAMERA}, 
                    REQUEST_CAMERA_PERMISSION);
        } else {
            Log.i(TAG, "✅ 摄像头权限已授予");
        }
    }
    
    /**
     * 处理权限请求结果
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        switch (requestCode) {
            case REQUEST_MICROPHONE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "✅ 用户授予了麦克风权限，自动启用功能");
                    
                    // 自动启用麦克风功能
                    PermissionManager permissionManager = PermissionManager.getInstance(this);
                    permissionManager.autoEnableMicrophoneAfterPermissionGranted();
                    
                } else {
                    Log.w(TAG, "❌ 用户拒绝了麦克风权限");
                }
                break;
                
            case REQUEST_CAMERA_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "✅ 用户授予了摄像头权限，自动启用功能");
                    
                    // 自动启用摄像头功能
                    PermissionManager permissionManager = PermissionManager.getInstance(this);
                    permissionManager.autoEnableCameraAfterPermissionGranted();
                    
                } else {
                    Log.w(TAG, "❌ 用户拒绝了摄像头权限");
                }
                break;
        }
    }
}
