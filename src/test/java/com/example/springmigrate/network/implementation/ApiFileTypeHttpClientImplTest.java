package com.example.springmigrate.network.implementation;

import com.example.springmigrate.dto.FileTypeNodeDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ApiFileTypeHttpClientImplTest {

    @Autowired
    private ApiFileTypeHttpClientImpl apiFileTypeHttpClient;

    @Test
    void apiFindFiles() throws IOException {

        List<FileTypeNodeDto> result = apiFileTypeHttpClient.apiFindTypes();

        assertNotNull(result);
        assertTrue(result.size() > 1);
    }
}