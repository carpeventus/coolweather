package com.example.administrator.coolweather.util;


import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpUtil {
    public static void sendOkHttpRequest(String address, Callback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(address).build();
        // enqueue默认内部已经开启好了子线程进行请求
        client.newCall(request).enqueue(callback);
    }
}
