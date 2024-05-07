package com.example.springmigrate.service;

import com.example.springmigrate.dto.RootNodeDto;

import java.io.IOException;
import java.util.List;

public interface IRootDirectoryService {

    List<RootNodeDto> findAll() throws IOException;
    List<RootNodeDto> findByDirectoryId(String directoryId) throws IOException;
    void deleteAll();
    void deleteByDirectoryId(String directoryId) throws IOException;
}
