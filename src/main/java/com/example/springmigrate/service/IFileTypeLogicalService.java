package com.example.springmigrate.service;

import com.example.springmigrate.dto.FileTypeNodeDto;

import java.io.IOException;
import java.util.List;

public interface IFileTypeLogicalService {

    public List<FileTypeNodeDto> findAllFileTypes() throws IOException;
}
