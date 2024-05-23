package com.example.springmigrate.repository;

import com.example.springmigrate.dto.FileFilterDto;
import com.example.springmigrate.dto.FileNodeDto;

import java.io.IOException;
import java.util.List;

public interface IFileRepository {

    /**
     * Find all files
     *
     * @return list of file nodes
     */
    List<FileNodeDto> findAll() throws IOException;


    /**
     * Find a list of files tah meet filter requirements
     *
     * @param filter filter
     * @return list of files tah meet filter requirements
     */
    List<FileNodeDto> findFilesByFilter(FileFilterDto filter) throws IOException;


    /**
     * Find file by uuid identifier
     *
     * @param uuid identifier
     * @return file if exists, otherwise {@code null}
     */
    FileNodeDto findFileById(String uuid) throws IOException;


    /**
     * Create file, 409 if already exists
     *
     * @param file file node with id null
     * @return file ndode created, otherwise {@code null}
     */
    FileNodeDto createFile(FileNodeDto file) throws IOException;


    /**
     * Update a file
     *
     * @param file file
     * @return file if updated, otherwise {@code null}
     */
    FileNodeDto updateFile(String id, FileNodeDto file) throws IOException;


    /**
     * Delete file by identifier(Soft)
     *
     * @param uuid identifier
     */
    void deleteFile(String uuid) throws IOException;
}
