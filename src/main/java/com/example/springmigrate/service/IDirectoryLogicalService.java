package com.example.springmigrate.service;

import com.example.springmigrate.dto.DirectoryFilterNodeDto;
import com.example.springmigrate.dto.DirectoryNodeDto;

import java.io.IOException;
import java.util.List;

public interface IDirectoryLogicalService {

    List<DirectoryNodeDto> normalizeDirectoriesNames() throws IOException;
    DirectoryNodeDto createDirectories(List<DirectoryNodeDto> directories) throws IOException;
    DirectoryNodeDto updateDirectory(DirectoryNodeDto directory) throws IOException;
    DirectoryNodeDto findDirectoryByParentId(String name, String parentId) throws IOException;
    DirectoryNodeDto findDirectoryByBasePath(String name, String pathBase) throws IOException;
    DirectoryNodeDto findDirectoryById(String id) throws IOException;
    DirectoryNodeDto createDirectory(String name, String pathBase) throws IOException;

}
