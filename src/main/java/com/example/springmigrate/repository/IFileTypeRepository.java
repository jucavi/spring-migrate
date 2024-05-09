package com.example.springmigrate.repository;

import com.example.springmigrate.dto.FileTypeNodeDto;

import java.io.IOException;
import java.util.List;

public interface IFileTypeRepository {

    /**
     * Find all file types
     *
     * @return list of file type nodes
     */
    List<FileTypeNodeDto> findAll() throws IOException;
}
