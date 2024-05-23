package com.example.springmigrate.service.implementation;

import com.example.springmigrate.dto.ContentFileNodeDto;
import com.example.springmigrate.dto.FileFilterDto;
import com.example.springmigrate.dto.FileNodeDto;
import com.example.springmigrate.model.FilePhysical;
import com.example.springmigrate.repository.IFileRepository;
import com.example.springmigrate.service.IFileLogicalService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
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

    @Override
    public List<FileNodeDto> findAll() throws IOException {
        return repository.findAll();
    }

    @Override
    public void deleteFile(String id) throws IOException {
        repository.deleteFile(id);
    }

    /**
     * Find all logical files whose names includes the name of the physical file
     * without extension
     *
     * @param filePhysical physical file object
     * @return the list of logical files that match the filter requirement
     * @throws IOException if I/O exception occurred
     * @see FilePhysical
     */
    @Override
    public List<FileNodeDto> findCandidateFilesByName(
            @NotNull String name) throws IOException {

        // Setting content for search files by name (include)
        ContentFileNodeDto content = new ContentFileNodeDto();
        content.setName(name);

        // Setting filter
        FileFilterDto filter = new FileFilterDto();
        filter.setSize(1000);
        filter.setContent(content);

        // Find candidates with filename(invalid UUID)
        return this.findFilesByFilter(filter);
    }
}
