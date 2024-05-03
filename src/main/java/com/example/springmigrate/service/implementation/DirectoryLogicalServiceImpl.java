package com.example.springmigrate.service.implementation;

import com.example.springmigrate.dto.ContentDirectoryNodeDto;
import com.example.springmigrate.dto.DirectoryFilterNodeDto;
import com.example.springmigrate.dto.DirectoryNodeDto;
import com.example.springmigrate.repository.IDirectoryRepository;
import com.example.springmigrate.service.IDirectoryLogicalService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@Log4j2
@AllArgsConstructor
public class DirectoryLogicalServiceImpl implements IDirectoryLogicalService {

    private final IDirectoryRepository repository;

    /**
     * Returns a list af leafs created
     *
     * @return list of leafs created
     */
    @Override
    public List<DirectoryNodeDto> normalizeDirectoriesNames() {

        List<DirectoryNodeDto> directories;
        List<DirectoryNodeDto> leafs = new ArrayList<>();

        try {
            directories = repository.findAll();

            for (DirectoryNodeDto directory : directories) {

                String name = directory.getName();
                Path namePath = Paths.get(name);

                // paths with name count > 1, means complex directory names
                if (namePath.getNameCount() > 1) {
                    // Do the magic here
                    // split directory name and create simple directories
                    leafs.add(createParentsAndRenameLeaf(directory));

                } else {
                    // normalize all directories with lower case
                    log.info(directory.getName());
                    directory.setName(directory.getName().toLowerCase());

                    if (directory.getParentDirectoryId() != null) {
                        directory.setPathBase(null);
                    }

                    repository.updateDirectory(directory);
                }
            }

        } catch (IOException e) {
            log.error("I/O error reading directories");
        }

        // Must return a list of leafs directories nodes renamed
        return leafs;
    }


    /**
     * Creates directory
     *
     * @param name directory name
     * @param pathBase absolute parent path
     * @return directory created
     */
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
     * @param name directory name
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
     * @param name directory name
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
     * Creates all parents, delete duplicated leaf node, and rename original node as leaf keeping all children references
     *
     * @param directory complex named directory
     * @return leaf node with all children attached
     * @throws IOException if I/O error occurred
     */
    private DirectoryNodeDto createParentsAndRenameLeaf(DirectoryNodeDto directory) throws IOException {

        Path namePath = Paths.get(directory.getName());
        // create a parent
        DirectoryNodeDto rootParent = repository.findDirectoryById(directory.getParentDirectoryId());
        // call with parent directory and only parents names
        DirectoryNodeDto lastParent = createFromRelativeRoute(rootParent, namePath.getParent(), 0);

        String leafName = Paths.get(directory.getName())
                .getFileName()
                .toString();

        DirectoryNodeDto duplicatedDirectory = findDirectoryByParentId(leafName, lastParent.getId());

        if (duplicatedDirectory != null) {
            List<DirectoryNodeDto> children = findChildrenDirectories(duplicatedDirectory.getId());

            for (DirectoryNodeDto child : children) {
                child.setParentDirectoryId(directory.getId());
                child.setPathBase(null);
                // update logical directory
                repository.updateDirectory(child);
            }

            repository.deleteDirectoryHard(duplicatedDirectory.getId());
        }

        // Leaf update
        directory.setName(
                leafName);
        directory.setParentDirectoryId(lastParent.getId());
        directory.setPathBase(null);

        return repository.updateDirectory(directory);
    }


    /**
     * Returns a list of all children directories
     *
     * @param parentId parent identifier
     * @return children directories
     * @throws IOException if I/O error occurred
     */
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
     * Creates all directories from relative path in parent directory
     *
     * @param parent parent directory
     * @param path relative path
     * @param index index of subpath
     * @return last directory created
     * @throws IOException if I/O error
     */
    private DirectoryNodeDto createFromRelativeRoute(DirectoryNodeDto parent, Path path, Integer index) throws IOException {

        String actualDirectoryName = path.subpath(index, index + 1).toString();
        String pathBase = Paths.get(parent.getPathBase(), parent.getName()).toString();
        DirectoryNodeDto result = createDirectory(parent, actualDirectoryName, pathBase);

        if (index == path.getNameCount() - 1) {
            return result;
        }

        assert result != null;
        return createFromRelativeRoute(result, path, ++index);
    }

    /**
     * Creates directory
     *
     * @param parent candidate parent directory
     * @param name directory name
     * @param pathBase absolute parent path
     * @return directory created
     * @throws IOException if I/O error
     */
    private DirectoryNodeDto createDirectory(DirectoryNodeDto parent, String name, String pathBase) throws IOException {

        List<DirectoryNodeDto> request = new ArrayList<>();
        request.add(DirectoryNodeDto.builder()
                .active(true)
                .name(name)
                .pathBase(pathBase)
                .build());

        DirectoryNodeDto result = createDirectories(request);

        if (result == null) {
            // find directory node
            result = findDirectoryByParentId(name, parent.getId());
        }

        return result;
    }

}
