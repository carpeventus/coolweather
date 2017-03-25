package com.example.administrator.coolweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.example.administrator.coolweather.gson.Weather;
import com.example.administrator.coolweather.util.HttpUtil;
import com.example.administrator.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {

    // 后台更新天气数据，无需和活动交互
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
//        updatePic();
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour = 60 * 60 * 1000; // 每隔一小时更新一次
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
        // 之所以是自己传给自己是因为这个服务会唤起一个新的服务（自己）
        Intent i = new Intent(this, AutoUpdateService.class);
        // 获取一个PendingIntent对象，而且该对象日后激发时所做的事情是启动一个新service
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        // 每次调用时先取消，再重新设置
        // cancel()方法是为了解除PendingIntent和被包装的Intent之间的关联
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 更新天气数据，只在有缓存的时候后台更新。这里存入最新的数据即可，不用传入给mWeather
     * 因为每次打开程序都会优先读取缓存
     */


    private void updateWeather() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = preferences.getString("weather", null);
        if (weatherString != null) {
            // 有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            String weatherId = weather.basic.weatherId;
            String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=b4a2ae648cc34289a145471385286b95";
            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    Weather weather = Utility.handleWeatherResponse(responseText);

                    if (weather != null && "ok".equals(weather.status)) {
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString("weather", responseText);
                        editor.apply();
                    }
                }
            });
        }
    }

//    /**
//     * 更新图片
//     */
//    private void updatePic() {
//        String requestBingPicUrl = "http://guolin.tech/api/bing_pic";
//        HttpUtil.sendOkHttpRequest(requestBingPicUrl, new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//                e.printStackTrace();
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                final String bingPic = response.body().string();
//                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
//                editor.putString("bing_pic", bingPic);
//                editor.apply();
//            }
//        });
//    }
}