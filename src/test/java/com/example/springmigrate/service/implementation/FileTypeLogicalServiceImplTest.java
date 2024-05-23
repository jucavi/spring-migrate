package com.example.springmigrate.service.implementation;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class FileTypeLogicalServiceImplTest {


    @Autowired
    private FileTypeLogicalServiceImpl service;

    @Test
    void findAllFileTypes() throws IOException {

        Map<String, String> actual = service.findAllFileTypes();

        assertEquals(actual.get("image/png"), ".png");
    }
}