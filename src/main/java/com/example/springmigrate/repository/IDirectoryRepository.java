package com.example.springmigrate.repository;


import com.example.springmigrate.dto.DirectoryFilterNodeDto;
import com.example.springmigrate.dto.DirectoryNodeDto;

import java.io.IOException;
import java.util.List;

public interface IDirectoryRepository {

    /**
     * Find all directories
     *
     * @return list of directories nodes
     */
    List<DirectoryNodeDto> findAll() throws IOException;


    /**
     * Create directory, 409 if already exists
     *
     * @param directory directory node with id null
     * @return list of directories created, otherwise {@code null}
     */
    List<DirectoryNodeDto> createDirectory(List<DirectoryNodeDto> directory) throws IOException;


    /**
     * Find directory by uuid identifier
     *
     * @param uuid identifier
     * @return directory if exists, otherwise {@code null}
     */
    DirectoryNodeDto findDirectoryById(String uuid) throws IOException;


    /**
     * Update a directory
     *
     * <ul>
     *     <li>If directory is leaf, pathBase must be {@code null}</li>
     *     <li>If directory is node, parentDirectoryId must be {@code null}</li>
     * </ul>
     *
     * @param directory directory
     * @return directory if updated (), otherwise {@code null}
     */
    DirectoryNodeDto updateDirectory(DirectoryNodeDto directory) throws IOException;


    /**
     * Find all directories by filter
     *
     * @param filter directory filter
     * @return list of directories created
     */
    List<DirectoryNodeDto> findAllDirectoriesByFilter(DirectoryFilterNodeDto filter) throws IOException;


    /**
     * Find first directory that mach with filter criteria
     *
     * @param filter directory filter
     * @return list with first directory match
     */
    DirectoryNodeDto findDirectoryByFilter(DirectoryFilterNodeDto filter) throws IOException;


    /**
     * Delete(soft) directory by identifier
     *
     * @param uuid identifier
     */
    void deleteDirectory(String uuid) throws IOException;


    /**
     * Delete directory by identifier
     *
     * @param uuid identifier
     */
    void deleteDirectoryHard(String uuid) throws IOException;
}
