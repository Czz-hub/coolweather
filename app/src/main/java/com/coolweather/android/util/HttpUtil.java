package com.coolweather.android.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class HttpUtil {
    // 静态方法：发送 HTTP 请求并异步处理响应
    public static void sendOkHttpRequest(String address, okhttp3.Callback callback) {
        OkHttpClient client = new OkHttpClient();          // 1. 创建 OkHttpClient 实例
        Request request = new Request.Builder()            // 2. 构建请求对象
                .url(address)
                .build();
        client.newCall(request).enqueue(callback);         // 3. 异步执行请求
    }
}
