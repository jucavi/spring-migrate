package com.example.springmigrate.config.utils;

import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;


@Setter
@Getter
@RequiredArgsConstructor
public class ApiUrl {

    private final String baseUrl;
}
