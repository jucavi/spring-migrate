package com.example.springmigrate.service;

import com.example.springmigrate.dto.RootNodeDto;

import java.io.IOException;
import java.util.List;

public interface IRootDirectoryService {
    List<RootNodeDto> findAll() throws IOException;
    List<RootNodeDto> findByDirectoryId(String directoryId) throws IOException;
    void truncate() throws IOException;
    RootNodeDto createRoot(RootNodeDto rootNode)throws IOException;
    void deleteByDirectoryId(String directoryId) throws IOException;
}
