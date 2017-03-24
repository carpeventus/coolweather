package com.example.administrator.coolweather.gson;


import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Weather {
    // 状态，成功返回ok
    public String status;

    public Basic basic;
    public AQI aqi;
    public Now now;
    public Suggestion suggestion;

    @SerializedName("daily_forecast")
    public List<Forecast> forecastList;
}