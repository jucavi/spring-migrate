package com.example.springmigrate.service;

import com.example.springmigrate.dto.DirectoryNodeDto;

import java.io.IOException;
import java.util.List;

public interface IDirectoryLogicalService {

    public List<DirectoryNodeDto> normalizeDirectoryName() throws IOException;
    public DirectoryNodeDto createDirectory(DirectoryNodeDto directory) throws IOException;
    public DirectoryNodeDto updateDirectory(DirectoryNodeDto directory) throws IOException;
    public DirectoryNodeDto findDirectory(String name, String parentId) throws IOException;

}
