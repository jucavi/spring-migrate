package com.example.springmigrate.config;

import com.example.springmigrate.config.utils.ApiUrl;
import com.example.springmigrate.config.utils.RetrofitClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration

public class CustomAppConfiguration {

    @Bean
    public ApiUrl getApiURL(ApplicationArguments arguments) {

        ApiUrl apiUrl = new ApiUrl("http://localhost:9004");

        if (arguments.getSourceArgs().length == 2) {
            apiUrl = new ApiUrl(arguments.getSourceArgs()[1]);
        }

        return apiUrl;
    }


    @Bean
    public RetrofitClient retrofitClient(ApiUrl apiUrl) {
        return new RetrofitClient(apiUrl);
    }
}
