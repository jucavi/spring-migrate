package com.example.springmigrate.service;

import com.example.springmigrate.dto.FileNodeDto;

import java.io.IOException;

public interface IFileLogicalService {

    FileNodeDto findFileById(String id) throws IOException;
    FileNodeDto updateFile(FileNodeDto dto) throws IOException;
    FileNodeDto createFile(FileNodeDto dto) throws IOException;
}
