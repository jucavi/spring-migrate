package com.example.springmigrate.config.utils;

import lombok.RequiredArgsConstructor;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;


@RequiredArgsConstructor
public class RetrofitClient {

    private final ApiUrl apiUrl;
    private static Retrofit instance;

    public Retrofit getInstance() {
        if (instance == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .readTimeout(60, TimeUnit.SECONDS)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS);


            instance = new Retrofit.Builder()
                    .baseUrl(apiUrl.getBaseUrl())
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build();
        }

        return instance;
    }
}
