package com.example.administrator.coolweather;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.administrator.coolweather.gson.Forecast;
import com.example.administrator.coolweather.gson.Weather;
import com.example.administrator.coolweather.util.HttpUtil;
import com.example.administrator.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView titleUpdateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    // 预报子项的布局，在forecast.xml中引入的LinearLayout
    private LinearLayout forecastLayout;

    private TextView aqiText;
    private TextView pm25Text;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;

    private ImageView bingPicView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();

            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE // 防止系统栏隐藏时内容区域大小发生变化
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

            getWindow().setStatusBarColor(Color.TRANSPARENT);
            // 下面的导航栏设置透明
//            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }
        setContentView(R.layout.activity_weather);


        // 初始化各个控件
        bingPicView = (ImageView) findViewById(R.id.bing_pic_img_view);
        weatherLayout = (ScrollView) findViewById(R.id.weather_scroll_view);
        titleCity = (TextView) findViewById(R.id.city_title);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.carwash_text);
        sportText = (TextView) findViewById(R.id.sport_text);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        // 没有该键，就返回null
        String weatherString = preferences.getString("weather", null);
        if (weatherString != null) {
            // 有缓存时直接解析天气数据，注意这里只存了一个响应，所以只要一选中某个城市，再怎么选城市也只是读取第一次保存的城市
            // TODO 以后再选择城市读取的还是第一个城市的信息，需要增加切换城市的功能，待完善
            Weather weather = Utility.handleWeatherResponse(weatherString);
            showWeatherInfo(weather);
            // 无法缓存时去服务器查询
        } else {
            String weatherId = getIntent().getStringExtra("weather_id");
            // 请求数据过程中，讲ScrollView设置为不可见。因为还没数据，显示无用
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        }

        // 先试着从本地加载图片

        String bingPic = preferences.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicView);
        } else {
            loadBingPic();
        }
    }

    /**
     * 加载Bing每日一图
     */
    private void loadBingPic() {
        String requestBingPicUrl = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPicUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic", bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(bingPicView);
                    }
                });
            }
        });
    }

    /**
     * 根据天气id来请求天气数据
     *
     * @param weatherId 天气id
     */
    private void requestWeather(String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=b4a2ae648cc34289a145471385286b95";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                // responseText是从服务器返回的，上面那个weatherString是从preference里读取的
                final Weather weather = Utility.handleWeatherResponse(responseText);
                // 显示天气，牵涉到UI操作，切换到主线程
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.status)) {
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    /**
     * 显示天气信息到
     *
     * @param weather Weather实例
     */
    private void showWeatherInfo(Weather weather) {
        // title.xml now.xml中的内容
        String cityName = weather.basic.cityName;
        // 只取时间不取日期，2017.3.23 15:03,以空格分割，取第二个
        String updateTime = "更新于"+weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "℃";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText(updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);

        // forecast.xml中的内容
        forecastLayout.removeAllViews(); // 更新预报时先清空所有view
        for (Forecast forecast : weather.forecastList) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView) view.findViewById(R.id.item_data_text);
            TextView infoText = (TextView) view.findViewById(R.id.item_info_text);
            TextView maxAndMinText = (TextView) view.findViewById(R.id.item_min_max_text);
            String date = forecast.date.split("-")[1] +"月"+ forecast.date.split("-")[2] + "日";
            dateText.setText(date);
            infoText.setText(forecast.more.info);
            String maxAndMinDegree = forecast.temperature.max + "℃ / " + forecast.temperature.min + "℃";
            maxAndMinText.setText(maxAndMinDegree);
            forecastLayout.addView(view);
        }

        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }
        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车建议：" + weather.suggestion.carWash.info;
        String sport = "运动建议：" + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        // 天气信息加载好后显示
        weatherLayout.setVisibility(View.VISIBLE);

    }
}