package com.example.springmigrate.service.implementation;

import com.example.springmigrate.config.utils.error.NoRequirementsMeted;
import com.example.springmigrate.dto.ContentDirectoryNodeDto;
import com.example.springmigrate.dto.DirectoryFilterNodeDto;
import com.example.springmigrate.dto.DirectoryNodeDto;
import com.example.springmigrate.repository.IDirectoryRepository;
import com.example.springmigrate.service.IDirectoryLogicalService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Log4j2
@AllArgsConstructor
public class DirectoryLogicalServiceImpl implements IDirectoryLogicalService {

    private final IDirectoryRepository repository;


    /**
     * Returns a list af all roots directories
     *
     * @return list af all roots directories
     */
    @Override
    public List<DirectoryNodeDto> findALl() throws IOException {
        return repository.findAll();
    }

    /**
     * Creates directory
     *
     * @param name     directory name
     * @param pathBase absolute parent path
     * @return directory created
     */
    @Override
    public DirectoryNodeDto createDirectory(String name, String pathBase) throws IOException {

        List<DirectoryNodeDto> request = new ArrayList<>();
        request.add(DirectoryNodeDto.builder()
                .active(true)
                .name(name)
                .pathBase(pathBase)
                .build());

        // Creates directories, null if an error occurred or directory already exists
        DirectoryNodeDto result = createDirectories(request);

        if (result == null) {
            result = findDirectoryByBasePath(name, pathBase);
        }

        return result;
    }

    /**
     * Returns a list of directories whose match filter
     *
     * @param filter filter
     * @return a list of directories whose match filter
     * @throws IOException if I/O exception occurred
     */
    @Override
    public List<DirectoryNodeDto> findAllDirectoriesByFilter(DirectoryFilterNodeDto filter) throws IOException {
        return repository.findAllDirectoriesByFilter(filter);
    }

    /**
     * Deletes directory from database
     *
     * @param id identifier
     * @throws IOException if I/O exception occurred
     */
    @Override
    public void deleteDirectoryHard(String id) throws IOException {
        repository.deleteDirectoryHard(id);
    }

    /**
     * Deletes directory(Soft)
     *
     * @param id identifier
     * @throws IOException if I/O exception occurred
     */
    @Override
    public void deleteDirectoryById(String id) throws IOException {
        repository.deleteDirectory(id);
    }

    /**
     * Find directory by identifier
     *
     * @param id identifier
     * @return directory if exists, otherwise {@code null}
     * @throws IOException if I/O error occurred
     */
    @Override
    public DirectoryNodeDto findDirectoryById(String id) throws IOException {
        return repository.findDirectoryById(id);
    }

    /**
     * Find directory by name and parent ID
     *
     * @param name     directory name
     * @param parentId parent identifier
     * @return directory if exists, otherwise {@code null}
     * @throws IOException if I/O error occurred
     */
    @Override
    public DirectoryNodeDto findDirectoryByParentId(String name, String parentId) throws IOException {

        ContentDirectoryNodeDto content = new ContentDirectoryNodeDto();
        content.setActive(true);
        content.setExactName(name);
        content.setParentDirectoryId(parentId);

        DirectoryFilterNodeDto filter = new DirectoryFilterNodeDto();
        filter.setContent(content);
        filter.setPage(0);
        filter.setSize(20);

        return repository.findDirectoryByFilter(filter);
    }

    /**
     * Find directory by name and base path
     *
     * @param name     directory name
     * @param basePath base path
     * @return directory if exists, otherwise {@code null}
     * @throws IOException if I/O error occurred
     */
    @Override
    public DirectoryNodeDto findDirectoryByBasePath(String name, String basePath) throws IOException {

        DirectoryNodeDto result = null;

        // create a filter
        ContentDirectoryNodeDto content = new ContentDirectoryNodeDto();
        content.setActive(true);
        content.setExactName(name);

        DirectoryFilterNodeDto filter = new DirectoryFilterNodeDto();
        filter.setContent(content);
        filter.setPage(0);
        filter.setSize(20);

        // find directory node
        List<DirectoryNodeDto> results = repository.findAllDirectoriesByFilter(filter);

        for (DirectoryNodeDto dto : results) {
            if (dto.getPathBase().equalsIgnoreCase(basePath)) {
                result = dto;
                break;
            }
        }

        return result;
    }

    /**
     * Create directory
     *
     * @param directories directory node with id null
     * @return list of directories created, otherwise {@code null}
     */
    @Override
    public DirectoryNodeDto createDirectories(List<DirectoryNodeDto> directories) throws IOException {
        List<DirectoryNodeDto> result = repository.createDirectory(directories);

        if (result == null || result.isEmpty()) {
            return null;
        }

        return result.get(result.size() - 1);
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
     * @return directory if updated, otherwise {@code null}
     */
    @Override
    public DirectoryNodeDto updateDirectory(DirectoryNodeDto directory) throws IOException {
        return repository.updateDirectory(directory);
    }

    /**
     * Returns a list of all children directories
     *
     * @param parentId parent identifier
     * @return children directories
     * @throws IOException if I/O error occurred
     */
    @Override
    public List<DirectoryNodeDto> findChildrenDirectories(String parentId) throws IOException {

        ContentDirectoryNodeDto content = new ContentDirectoryNodeDto();
        content.setActive(true);
        content.setParentDirectoryId(parentId);

        DirectoryFilterNodeDto filter = new DirectoryFilterNodeDto();
        filter.setContent(content);
        filter.setPage(0);
        filter.setSize(100000);

        return repository.findAllDirectoriesByFilter(filter);
    }

    /**
     * Creates if not exists logical directory
     *
     * @param name     directory name
     * @param basePath directory path base
     * @return logical directory node
     * @throws IOException         if IOException occurred
     * @throws NoRequirementsMeted if cant create a directory node
     */
    @Override
    public DirectoryNodeDto createLogicalNode(String name, String basePath) throws IOException, NoRequirementsMeted {
        DirectoryNodeDto directoryNode = this.createDirectory(name, basePath);

        // Could be already created
        if (directoryNode == null) {
            directoryNode = this.findDirectoryByBasePath(name, basePath);
        }

        // could be integrity problems
        if (directoryNode == null) {
            throw new NoRequirementsMeted("Unable to create necessary node");
        }

        return directoryNode;
    }
}
