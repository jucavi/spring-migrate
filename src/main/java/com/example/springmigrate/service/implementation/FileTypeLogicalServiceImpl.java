package com.example.springmigrate.service.implementation;

import com.example.springmigrate.dto.FileTypeNodeDto;
import com.example.springmigrate.repository.IFileTypeRepository;
import com.example.springmigrate.service.IFileTypeLogicalService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@AllArgsConstructor
public class FileTypeLogicalServiceImpl implements IFileTypeLogicalService {

    private final IFileTypeRepository repository;

    @Override
    public List<FileTypeNodeDto> findAllFileTypes() throws IOException {
        return repository.findAll();
    }
}
