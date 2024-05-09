package com.example.springmigrate.service.implementation;

import com.example.springmigrate.dto.FileFilterDto;
import com.example.springmigrate.dto.FileNodeDto;
import com.example.springmigrate.repository.IFileRepository;
import com.example.springmigrate.service.IFileLogicalService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
@Log4j2
@AllArgsConstructor
public class FileLogicalServiceImpl implements IFileLogicalService {

    private final IFileRepository repository;

    @Override
    public FileNodeDto findFileById(String id) throws IOException {
        return repository.findFileById(id);
    }

    @Override
    public FileNodeDto updateFile(FileNodeDto dto) throws IOException {
        return repository.updateFile(dto.getId(), dto);
    }

    @Override
    public FileNodeDto createFile(FileNodeDto dto) throws IOException {
        return repository.createFile(dto);
    }

    @Override
    public List<FileNodeDto> findFilesByFilter(FileFilterDto filter) throws IOException {
        return repository.findFilesByFilter(filter);
    }
}
