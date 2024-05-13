package com.example.springmigrate.service;

import com.example.springmigrate.dto.FileFilterDto;
import com.example.springmigrate.dto.FileNodeDto;

import java.io.IOException;
import java.util.List;

public interface IFileLogicalService {


    FileNodeDto findFileById(String id) throws IOException;
    FileNodeDto updateFile(FileNodeDto dto) throws IOException;
    FileNodeDto createFile(FileNodeDto dto) throws IOException;
    List<FileNodeDto> findFilesByFilter(FileFilterDto filter) throws IOException;
    List<FileNodeDto> findAll() throws IOException;
    void deleteFile(String id) throws IOException;
}
