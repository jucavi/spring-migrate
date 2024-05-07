package com.example.springmigrate.repository;

import com.example.springmigrate.dto.RootNodeDto;

import java.io.IOException;
import java.util.List;

public interface IRootDirectoryRepository {

    /**
     * Find all root directories
     *
     * @return list of root directories nodes
     */
    List<RootNodeDto> findAll() throws IOException;

    /**
     * Find all root directories by child directory id
     *
     * @return list of root directories nodes with child directory id
     */
    List<RootNodeDto> findByDirectoryId(String directoryId) throws IOException;


    /**
     * Deletes root directory by directory child Id
     */
    void deleteByDirectoryId(String directoryId) throws IOException;
}
