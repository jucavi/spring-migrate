package com.example.springmigrate.service;

import com.example.springmigrate.dto.FileTypeNodeDto;

import java.io.IOException;
import java.util.Map;

public interface IFileTypeLogicalService {

    public Map<String, String> findAllFileTypes() throws IOException;
}
