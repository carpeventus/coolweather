package com.example.administrator.coolweather;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.administrator.coolweather.gson.Forecast;
import com.example.administrator.coolweather.gson.Weather;
import com.example.administrator.coolweather.service.AutoUpdateService;
import com.example.administrator.coolweather.util.HttpUtil;
import com.example.administrator.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

// TODO 返回键可以返回选择城市列表，而不是直接退出程序。drawer的标题栏太高，不美观
public class WeatherActivity extends AppCompatActivity {

    public SwipeRefreshLayout swipeRefreshLayout;
    private String mWeatherId;

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
    public DrawerLayout drawerLayout;
    private Button navButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();

            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE // 防止系统栏隐藏时内容区域大小发生变化
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_weather);

        // 初始化各个控件
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navButton = (Button) findViewById(R.id.nav_button);
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

        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefreshLayout.setColorSchemeResources(R.color.colorPrimary);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        // 没有该键，就返回null
        String weatherString = preferences.getString("weather", null);
        if (weatherString != null) {
            // 有缓存时直接解析天气数据,每次打开程序就自动更新
            Weather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.basic.weatherId;
            // 这里会开启服务，这时开始更新数据
            showWeatherInfo(weather);
            // 无法缓存时去服务器查询
        } else {
            // 只有第一次选择城市的时候回进入这个条件，之后都会从SharedPreferences读取最新选择的城市
            mWeatherId = getIntent().getStringExtra("weather_id");
            // 请求数据过程中，讲ScrollView设置为不可见。因为还没数据，显示无用
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(mWeatherId);
        }

        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                // 上次请求已经保存了最新的weatherId.若没有切换城市每次刷新都是刷新当前
                requestWeather(mWeatherId);
            }
        });

        // 先试着从本地加载图片
        String bingPic = preferences.getString("bing_pic", null);
        if (bingPic != null) {
            Glide.with(this).load(bingPic).into(bingPicView);
        } else {
            loadBingPic();
        }

        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {

            @Override
            public void onDrawerOpened(View drawerView) {
                if (Build.VERSION.SDK_INT >= 21) {
                    View decorView = getWindow().getDecorView();

                    decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);

                    getWindow().setStatusBarColor(Color.TRANSPARENT);
                }
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                if (Build.VERSION.SDK_INT >= 21) {
                    View decorView = getWindow().getDecorView();

                    decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE // 防止系统栏隐藏时内容区域大小发生变化
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);

                    getWindow().setStatusBarColor(Color.TRANSPARENT);
                }
            }
        });
    }

    /**
     * 重写返回键监听
     * 当drawer打开时候，功能和上面的返回键一样。不过当前等级为省份时候，关闭drawer回到天气界面
     * 当drawer已经关闭的状态，再次按下返回键，退出程序
     */
    @Override
    public void onBackPressed() {
        ChooseAreaFragment drawerFragment = (ChooseAreaFragment) getSupportFragmentManager().findFragmentById(R.id.drawer_choose_area_fragment);
        if (drawerLayout.isDrawerOpen(R.id.drawer_choose_area_fragment)) {
            if (ChooseAreaFragment.currentLevel == ChooseAreaFragment.LEVEL_COUNTRY) {
                drawerFragment.queryCities();
            } else if (ChooseAreaFragment.currentLevel == ChooseAreaFragment.LEVEL_CITY) {
                drawerFragment.queryProvinces();
            } else if (ChooseAreaFragment.currentLevel == ChooseAreaFragment.LEVEL_PROVINCE) {
                drawerLayout.closeDrawers();
            }
        } else {
            super.onBackPressed();
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
    public void requestWeather(String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=b4a2ae648cc34289a145471385286b95";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
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
                            // 每请求一次就保存当前城市的ID
                            mWeatherId = weather.basic.weatherId;
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefreshLayout.setRefreshing(false);
                    }
                });
            }
        });
        // 每次请求天气数据的时候，也请求图片
        loadBingPic();
    }

    /**
     * 显示天气信息到
     *
     * @param weather Weather实例
     */
    private void showWeatherInfo(Weather weather) {
        if (weather != null && weather.status.equals("ok")) {
            // title.xml now.xml中的内容
            String cityName = weather.basic.cityName;
            // 只取时间不取日期，2017.3.23 15:03,以空格分割，取第二个
            String updateTime = "更新于" + weather.basic.update.updateTime.split(" ")[1];
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
                String date = forecast.date.split("-")[1] + "月" + forecast.date.split("-")[2] + "日";
                dateText.setText(date);
                infoText.setText(forecast.more.info);
                String maxAndMinDegree = forecast.temperature.max + "℃ / " + forecast.temperature.min + "℃";
                maxAndMinText.setText(maxAndMinDegree);
                forecastLayout.addView(view);
            }

            if (weather.aqi != null) {
                aqiText.setTextSize(40);
                pm25Text.setTextSize(40);
                aqiText.setText(weather.aqi.city.aqi);
                pm25Text.setText(weather.aqi.city.pm25);
            } else {
                aqiText.setTextSize(24);
                pm25Text.setTextSize(24);
                aqiText.setText("暂无");
                pm25Text.setText("暂无");
            }
            String comfort = "舒适度：" + weather.suggestion.comfort.info;
            String carWash = "洗车指数：" + weather.suggestion.carWash.info;
            String sport = "运动指数：" + weather.suggestion.sport.info;
            comfortText.setText(comfort);
            carWashText.setText(carWash);
            sportText.setText(sport);
            // 天气信息加载好后显示
            weatherLayout.setVisibility(View.VISIBLE);

            // 每次应用启动会调用showWeatherInfo方法，在这里启动服务，保证每次进来都是更新过的数据
            Intent intent = new Intent(this, AutoUpdateService.class);
            startService(intent);

        } else {
            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
        }
    }
}
