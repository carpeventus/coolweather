package com.example.administrator.coolweather.gson;
// 这里java字段和json键一样，就不用@SerializedName了
public class AQI {
    public AQICity city;

    public class AQICity {
        public String aqi;
        public String pm25;
    }
}
