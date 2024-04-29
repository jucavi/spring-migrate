package com.example.springmigrate.config.utils;

import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;


@Component
public class RetrofitClient {

    //@Value(value = "${app.app.api-url}:http//localhost:9004")
    private static String baseUrl;
    private static Retrofit instance;

    public RetrofitClient( @Value("${app.api-url}") String baseUrl) {
        RetrofitClient.baseUrl = baseUrl;
    }

    public Retrofit getInstance() {
        if (instance == null) {
            OkHttpClient.Builder httpClient = new OkHttpClient.Builder()
                    .readTimeout(5, TimeUnit.SECONDS);


            instance = new Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient.build())
                    .build();
        }

        return instance;
    }
}
