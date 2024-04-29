package com.example.springmigrate.repository.implemantation;

import com.example.springmigrate.dto.FileTypeNodeDto;
import com.example.springmigrate.network.implementation.ApiFileTypeHttpClientImpl;
import com.example.springmigrate.repository.IFileTypeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.List;

@Repository
@AllArgsConstructor
public class FileTypeImpl implements IFileTypeRepository {

    private final ApiFileTypeHttpClientImpl fileTypeHttpClient;

    /**
     * Find all file type
     *
     * @return list of file type nodes
     */
    @Override
    public List<FileTypeNodeDto> findAll() throws IOException {
        return  fileTypeHttpClient.apiFindTypes();
    }
}
