package com.example.springmigrate.network.implementation;

import com.example.springmigrate.dto.DirectoryNodeDto;
import com.example.springmigrate.dto.FileNodeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApiFileHttpClientImplTest {

    private String uuid;

    @Autowired
    private ApiFileHttpClientImpl apiFileHttpClient;

    @BeforeEach
    void setUp() throws IOException {
        this.uuid = "003398f7-5d94-46b5-bd28-21e30f04fd61";
    }

    @Test
    void apiFindFiles() {
    }

    @Test
    void apiCreateFile() {
    }

    @Test
    void apiFindFileById() throws IOException {
        FileNodeDto dto = apiFileHttpClient.apiFindFileById(this.uuid);

        assertNotNull(dto);
        assertEquals(dto.getName(), "21 07 22 Ignacio Lavín.jpg");
    }

    @Test
    void apiUpdateFile() {
    }

    @Test
    void apiDeleteFileById() {
    }
}