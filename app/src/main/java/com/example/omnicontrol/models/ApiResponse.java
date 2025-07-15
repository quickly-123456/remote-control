package com.example.omnicontrol.models;

import com.google.gson.annotations.SerializedName;

/**
 * API响应通用模型
 */
public class ApiResponse<T> {
    @SerializedName("result")
    private int result;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("user")
    private T data;
    
    public ApiResponse() {}
    
    public ApiResponse(int result, String message) {
        this.result = result;
        this.message = message;
    }
    
    public ApiResponse(int result, String message, T data) {
        this.result = result;
        this.message = message;
        this.data = data;
    }
    
    public int getResult() {
        return result;
    }
    
    public void setResult(int result) {
        this.result = result;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public T getData() {
        return data;
    }
    
    public void setData(T data) {
        this.data = data;
    }
    
    public boolean isSuccess() {
        return result == 0;
    }
}
