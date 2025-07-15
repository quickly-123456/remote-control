package com.example.omnicontrol.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.example.omnicontrol.R;
import com.example.omnicontrol.databinding.FragmentMainBinding;
import com.example.omnicontrol.utils.UserManager;

public class MainFragment extends Fragment {
    
    private FragmentMainBinding binding;
    private UserManager userManager;
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, 
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentMainBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        userManager = new UserManager(getContext());
        
        // 显示用户信息
        initializeUserInfo();
        
        // 设置各个功能按钮的点击事件
        binding.cardStatistics.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_main_to_statistics)
        );
        
        binding.cardConfiguration.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_main_to_configuration)
        );
        
        binding.cardHistory.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_main_to_connection_history)
        );
        
        binding.cardPassword.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_main_to_update_password)
        );
        
        binding.cardAbout.setOnClickListener(v -> 
            Navigation.findNavController(v).navigate(R.id.action_main_to_about)
        );
    }
    
    private void initializeUserInfo() {
        String username = userManager.getCurrentUsername();
        if (!username.isEmpty()) {
            // 如果主页布局中有用户名显示控件，可以在这里设置
            // binding.tvWelcome.setText("欢迎，" + username);
            Toast.makeText(getContext(), "欢迎回来，" + username, Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
