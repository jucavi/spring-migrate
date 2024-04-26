package com.example.springmigrate.repository.implemantation;

import com.example.springmigrate.dto.DirectoryFilterNodeDto;
import com.example.springmigrate.dto.DirectoryNodeDto;
import com.example.springmigrate.network.implementation.ApiDirectoryHttpClientImpl;
import com.example.springmigrate.repository.IDirectoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;

@Repository
@AllArgsConstructor
public class DirectoryRepositoryImpl  implements IDirectoryRepository {

    private final ApiDirectoryHttpClientImpl DirectoryHttpClient;


    /**
     * Find all directories
     *
     * @return list of directories nodes
     */
    @Override
    public List<DirectoryNodeDto> findAll() throws IOException {
        return  DirectoryHttpClient.apiFindDirectories();
    }


    /**
     * Create directory, 409 if already exists
     *
     * @param directory directory node with id null
     * @return list of directories created, otherwise {@code null}
     */
    @Override
    public List<DirectoryNodeDto> createDirectory(DirectoryNodeDto directory) throws IOException {
        return DirectoryHttpClient.apiCreateDirectoryHierarchicallyPhysical(directory);
    }


    /**
     * Find directory by uuid identifier
     *
     * @param uuid identifier
     * @return directory if exists, otherwise {@code null}
     */
    @Override
    public DirectoryNodeDto findDirectoryById(String uuid) throws IOException {
        return DirectoryHttpClient.apiFindDirectoryById(uuid);
    }


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
    @Override
    public DirectoryNodeDto updateDirectory(DirectoryNodeDto directory) throws IOException {
        return DirectoryHttpClient.apiUpdateDirectory(directory.getId(), directory);
    }


    /**
     * Find all directories by filter
     *
     * @param directories directory filter
     * @return list of directories created
     */
    @Override
    public List<DirectoryNodeDto> findAllDirectoriesByFilter(List<DirectoryFilterNodeDto> directories) throws IOException {
        return DirectoryHttpClient.apiSearchAllDirectoriesByFilter(directories);
    }


    /**
     * Find first directory that mach with filter criteria
     *
     * @param directories directory filter
     * @return list with first directory match
     */
    @Override
    public List<DirectoryNodeDto> findDirectoryByFilter(List<DirectoryFilterNodeDto> directories) throws IOException {
        return DirectoryHttpClient.apiSearchDirectoryByFilter(directories);
    }

    /**
     * Delete directory by identifier
     *
     * @param uuid identifier
     */
    @Override
    public void deleteDirectory(String uuid) throws IOException {
        DirectoryHttpClient.apiDeleteDirectoryById(uuid);
    }
}
