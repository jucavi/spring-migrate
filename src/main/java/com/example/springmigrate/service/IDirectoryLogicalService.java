package com.example.springmigrate.service;

import com.example.springmigrate.config.utils.error.NoRequirementsMeted;
import com.example.springmigrate.dto.DirectoryFilterNodeDto;
import com.example.springmigrate.dto.DirectoryNodeDto;

import java.io.IOException;
import java.util.List;

public interface IDirectoryLogicalService {

    List<DirectoryNodeDto> findALl() throws IOException;

    DirectoryNodeDto createDirectories(List<DirectoryNodeDto> directories) throws IOException;

    DirectoryNodeDto updateDirectory(DirectoryNodeDto directory) throws IOException;

    DirectoryNodeDto findDirectoryByParentId(String name, String parentId) throws IOException;

    DirectoryNodeDto findDirectoryByBasePath(String name, String pathBase) throws IOException;

    DirectoryNodeDto findDirectoryById(String id) throws IOException;

    DirectoryNodeDto createDirectory(String name, String pathBase) throws IOException;

    List<DirectoryNodeDto> findAllDirectoriesByFilter(DirectoryFilterNodeDto filter) throws IOException;

    void deleteDirectoryHard(String id) throws IOException;

    void deleteDirectoryById(String id) throws IOException;

    List<DirectoryNodeDto> findChildrenDirectories(String parentId) throws IOException;

    DirectoryNodeDto createLogicalNode(String name, String basePath) throws IOException, NoRequirementsMeted;
}
