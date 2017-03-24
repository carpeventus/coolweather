package com.example.administrator.coolweather.gson;


import com.google.gson.annotations.SerializedName;

public class Basic {
    // @SerializedName注解在Json映射时候使用注解里面的内容，而非驼峰法命名的Java字段
    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    public class Update {
        @SerializedName("loc")
        public String updateTime;
    }
}
