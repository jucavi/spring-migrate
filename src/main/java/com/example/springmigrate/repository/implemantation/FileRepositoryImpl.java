package com.example.springmigrate.repository.implemantation;

import com.example.springmigrate.dto.FileFilterDto;
import com.example.springmigrate.dto.FileNodeDto;
import com.example.springmigrate.network.implementation.ApiFileHttpClientImpl;
import com.example.springmigrate.repository.IFileRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;

@Repository
@AllArgsConstructor
public class FileRepositoryImpl implements IFileRepository {

    private final ApiFileHttpClientImpl fileHttpClient;

    /**
     * Find all files
     *
     * @return list of files nodes
     */
    @Override
    public List<FileNodeDto> findAll() throws IOException {
        return fileHttpClient.apiFindFiles();
    }

    @Override
    public List<FileNodeDto> findFilesByFilter(FileFilterDto filter) throws IOException {
        return fileHttpClient.apiFindFilesByFilter(filter);
    }

    /**
     * Create file, 409 if already exists
     *
     * @param file file node with id null
     * @return file created, otherwise {@code null}
     */
    @Override
    public FileNodeDto createFile(FileNodeDto file) throws IOException {
        return fileHttpClient.apiCreateFile(file);
    }

    /**
     * Find file by uuid identifier
     *
     * @param uuid identifier
     * @return file if exists, otherwise {@code null}
     */
    @Override
    public FileNodeDto findFileById(String uuid) throws IOException {
        return fileHttpClient.apiFindFileById(uuid);
    }

    public FileNodeDto updateFile(String id, FileNodeDto dto) throws IOException {
        return fileHttpClient.apiUpdateFile(dto, id);
    }

    /**
     * Delete file by identifier
     *
     * @param uuid identifier
     */
    @Override
    public void deleteFile(String uuid) throws IOException {
        fileHttpClient.apiDeleteFileById(uuid);
    }
}
