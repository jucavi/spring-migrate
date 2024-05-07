package com.example.springmigrate.service.implementation;

import com.example.springmigrate.dto.RootNodeDto;
import com.example.springmigrate.repository.IRootDirectoryRepository;
import com.example.springmigrate.service.IRootDirectoryService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;


@Service
@Log4j2
@AllArgsConstructor
public class RootDirectoryServiceImpl implements IRootDirectoryService {

    private final IRootDirectoryRepository repository;

    @Override
    public List<RootNodeDto> findAll() throws IOException {
        return repository.findAll();
    }

    @Override
    public List<RootNodeDto> findByDirectoryId(String directoryId) throws IOException {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public void deleteAll() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public void deleteByDirectoryId(String directoryId) throws IOException {
        repository.deleteByDirectoryId(directoryId);
    }
}
