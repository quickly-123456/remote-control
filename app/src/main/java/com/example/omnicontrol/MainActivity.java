package com.example.omnicontrol;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.NavDestination;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private View bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
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
}
