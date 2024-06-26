package com.example.springmigrate.repository.implemantation;

import com.example.springmigrate.dto.RootNodeDto;
import com.example.springmigrate.network.implementation.ApiRootDirectoryHttpClient;
import com.example.springmigrate.repository.IRootDirectoryRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;


@Repository
@AllArgsConstructor
public class RootDirectoryRepositoryImpl implements IRootDirectoryRepository {

    private final ApiRootDirectoryHttpClient rootDirectoryHttpClient;

    @Override
    public List<RootNodeDto> findAll() throws IOException {
        return rootDirectoryHttpClient.apiFindRootDirectories();
    }

    @Override
    public List<RootNodeDto> findByDirectoryId(String directoryId) throws IOException {
        return rootDirectoryHttpClient.apiFindByDirectoryId(directoryId);
    }

    @Override
    public void deleteByDirectoryId(String directoryId) throws IOException {
        rootDirectoryHttpClient.apiDeleteByDirectoryId(directoryId);
    }

    @Override
    public void truncate() throws IOException {
        rootDirectoryHttpClient.truncate();
    }

    @Override
    public RootNodeDto createRoot(RootNodeDto rootNode) throws IOException {
        return rootDirectoryHttpClient.createRoot(rootNode);
    }
}
