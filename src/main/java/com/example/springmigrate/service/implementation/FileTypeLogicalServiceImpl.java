package com.example.springmigrate.service.implementation;

import com.example.springmigrate.dto.FileTypeNodeDto;
import com.example.springmigrate.repository.IFileTypeRepository;
import com.example.springmigrate.service.IFileTypeLogicalService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class FileTypeLogicalServiceImpl implements IFileTypeLogicalService {

    private final IFileTypeRepository repository;

    @Override
    public Map<String, String> findAllFileTypes() throws IOException {

        Map<String, String> result = new HashMap<>();
        List<FileTypeNodeDto> response =  repository.findAll();

        if (response != null) {
            response.forEach(c -> result.put(c.getMimeType(), c.getExtension()));
        }

        return result;
    }
}
