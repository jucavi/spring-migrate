package com.example.springmigrate.service;

import com.example.springmigrate.dto.DirectoryNodeDto;

import java.io.IOException;
import java.util.List;

public interface IDirectoryLogicalService {

    List<DirectoryNodeDto> normalizeDirectoriesNames() throws IOException;
    DirectoryNodeDto createDirectory(List<DirectoryNodeDto> directories) throws IOException;
    DirectoryNodeDto updateDirectory(DirectoryNodeDto directory) throws IOException;
    DirectoryNodeDto findDirectory(String name, String parentId) throws IOException;
    DirectoryNodeDto findDirectoryById(String id) throws IOException;

}
