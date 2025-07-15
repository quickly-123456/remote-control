package com.example.omnicontrol.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import java.util.concurrent.TimeUnit;

/**
 * 网络服务管理器
 * 提供Retrofit实例和API服务
 */
public class NetworkService {
    
    private static final String BASE_URL = "http://185.128.227.222:5555/";
    private static NetworkService instance;
    private ApiService apiService;
    
    private NetworkService() {
        initializeRetrofit();
    }
    
    public static synchronized NetworkService getInstance() {
        if (instance == null) {
            instance = new NetworkService();
        }
        return instance;
    }
    
    private void initializeRetrofit() {
        // 创建HTTP日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        // 创建OkHttpClient
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(loggingInterceptor)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        // 创建Retrofit实例
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        // 创建API服务实例
        apiService = retrofit.create(ApiService.class);
    }
    
    public ApiService getApiService() {
        return apiService;
    }
}
