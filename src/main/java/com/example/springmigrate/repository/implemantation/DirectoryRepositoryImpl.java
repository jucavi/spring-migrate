package com.example.springmigrate.repository.implemantation;

import com.example.springmigrate.dto.DirectoryFilterNodeDto;
import com.example.springmigrate.dto.DirectoryNodeDto;
import com.example.springmigrate.network.implementation.ApiDirectoryHttpClientImpl;
import com.example.springmigrate.repository.IDirectoryRepository;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;

@Repository
public class DirectoryRepositoryImpl extends ApiDirectoryHttpClientImpl implements IDirectoryRepository {

    @Override
    public List<DirectoryNodeDto> findAll() throws IOException {
        return  apiFindDirectories();
    }

    @Override
    public List<DirectoryNodeDto> createDirectory(DirectoryNodeDto directory) throws IOException {
        return apiCreateDirectoryHierarchicallyPhysical(directory);
    }

    @Override
    public DirectoryNodeDto findDirectoryById(String uuid) throws IOException {
        return apiFindDirectoryById(uuid);
    }

    @Override
    public DirectoryNodeDto updateDirectory(DirectoryNodeDto directory) throws IOException {
        return apiUpdateDirectory(directory.getId(), directory);
    }

    @Override
    public List<DirectoryNodeDto> findAllDirectoriesByFilter(List<DirectoryFilterNodeDto> directories) throws IOException {
        return apiSearchAllDirectoriesByFilter(directories);
    }

    @Override
    public List<DirectoryNodeDto> findDirectoryByFilter(List<DirectoryFilterNodeDto> directories) throws IOException {
        return apiSearchDirectoryByFilter(directories);
    }

    @Override
    public void deleteDirectory(String uuid) throws IOException {
        apiDeleteDirectoryById(uuid);
    }
}
