package com.example.springmigrate.network.implementation;

import com.example.springmigrate.dto.ContentDirectoryNodeDto;
import com.example.springmigrate.dto.DirectoryFilterNodeDto;
import com.example.springmigrate.dto.DirectoryNodeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ApiDirectoryHttpClientImplTest {

    private String uuid;
    private DirectoryNodeDto dto;

    @Autowired
    private ApiDirectoryHttpClientImpl apiDirectoryHttpClient;

    @BeforeEach
    void setUp() throws IOException {

        this.uuid = "662880dd-a731-4177-842d-28984f1ec030";
        dto = apiDirectoryHttpClient.apiFindDirectoryById(uuid);
    }

    @Test
    void apiFindDirectories() throws IOException {

        List<DirectoryNodeDto> result = apiDirectoryHttpClient.apiFindDirectories();

        assertNotNull(result);
        assertTrue(result.size() > 1);
    }

    @Test
    void apiCreateDirectoryHierarchicallyLogical() throws IOException {

        String name = "testname";
        String pathBase = Paths.get(dto.getPathBase(), dto.getName()).toString();

        DirectoryNodeDto newDto = new DirectoryNodeDto();
        newDto.setName(name);
        newDto.setPathBase(pathBase);

        List<DirectoryNodeDto> list = new ArrayList<>();
        list.add(newDto);

        List<DirectoryNodeDto> result = apiDirectoryHttpClient.apiCreateDirectoryHierarchicallyLogical(list);

        assertFalse(result.isEmpty());
        assertEquals(name, result.get(result.size() - 1).getName());
        assertEquals(pathBase, result.get(result.size() - 1).getPathBase());
        assertEquals(dto.getId(), result.get(result.size() - 1).getParentDirectoryId());
    }

    @Test
    void apiFindDirectoryById() throws IOException {

        String expected = "DP";
        DirectoryNodeDto dto = apiDirectoryHttpClient.apiFindDirectoryById(uuid);

        assertNotNull(dto);
        assertEquals(expected, dto.getName());
    }

    @Test
    void apiUpdateDirectory() throws IOException {

        String name = "SuperDp";

        dto.setName(name);
        dto.setParentDirectoryId(null);

        DirectoryNodeDto updatedDirectory = apiDirectoryHttpClient.apiUpdateDirectory(dto);

        assertNotNull(updatedDirectory);
        assertEquals(updatedDirectory.getName(), name.toLowerCase());
    }

    @Test
    void apiSearchAllDirectoriesByFilter() throws IOException {

        String name = "testName";
        String pathBase = Paths.get(dto.getPathBase(), dto.getName()).toString();

        ContentDirectoryNodeDto content = new ContentDirectoryNodeDto();
        content.setActive(true);
        content.setExactName(name);
        content.setParentDirectoryId(dto.getId());

        DirectoryFilterNodeDto filter = new DirectoryFilterNodeDto();
        filter.setContent(content);
        filter.setPage(0);
        filter.setSize(20);

        List<DirectoryNodeDto> result = apiDirectoryHttpClient.apiSearchAllDirectoriesByFilter(filter);

        assertFalse(result.isEmpty());
        assertEquals(result.get(0).getPathBase(), pathBase);
    }

    @Test
    void apiSearchDirectoryByFilter() throws IOException {

        String name = "testName";
        String pathBase = Paths.get(dto.getPathBase(), dto.getName()).toString();

        ContentDirectoryNodeDto content = new ContentDirectoryNodeDto();
        content.setActive(true);
        content.setExactName(name);
        content.setParentDirectoryId(dto.getId());

        DirectoryFilterNodeDto filter = new DirectoryFilterNodeDto();
        filter.setContent(content);
        filter.setPage(0);
        filter.setSize(20);

        DirectoryNodeDto result = apiDirectoryHttpClient.apiSearchDirectoryByFilter(filter);

        assertNotNull(result);
        assertEquals(result.getPathBase(), pathBase);
    }

    @Test
    void apiDeleteDirectoryById() throws IOException {

        apiDirectoryHttpClient.apiDeleteDirectoryById(uuid);

        DirectoryNodeDto result = apiDirectoryHttpClient.apiFindDirectoryById(uuid);

        assertFalse(result.getActive());
    }

    @Test
    void apiDeleteDirectoryHardById() throws IOException {

        apiDirectoryHttpClient.apiDeleteDirectoryHardById(uuid);

        DirectoryNodeDto result = apiDirectoryHttpClient.apiFindDirectoryById(uuid);

        assertNull(result);
    }
}