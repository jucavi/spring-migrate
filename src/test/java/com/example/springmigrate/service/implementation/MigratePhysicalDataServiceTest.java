package com.example.springmigrate.service.implementation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MigratePhysicalDataServiceTest {

    @Autowired
    private MigratePhysicalDataService service;

    private String filename;
    private Path path;

    @BeforeEach
    void setUp() {
        path = Paths.get("C:\\Users\\jcvilarrubia\\Desktop\\text.txt");
        try {
            Files.createFile(path);
        } catch (IOException e) {
            //
        }
    }

    @Test
    void renameDuplicatedFile() {
    }
}