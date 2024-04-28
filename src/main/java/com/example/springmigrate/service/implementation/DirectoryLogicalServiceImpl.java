package com.example.springmigrate.service.implementation;

import com.example.springmigrate.dto.ContentNodeDto;
import com.example.springmigrate.dto.DirectoryFilterNodeDto;
import com.example.springmigrate.dto.DirectoryNodeDto;
import com.example.springmigrate.repository.IDirectoryRepository;
import com.example.springmigrate.service.IDirectoryLogicalService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@AllArgsConstructor
public class DirectoryLogicalServiceImpl implements IDirectoryLogicalService {

    private final IDirectoryRepository repository;

    /**
     * Returns a list af leafs created
     *
     * @return list of leafs created
     * @throws IOException
     */
    @Override
    public List<DirectoryNodeDto> normalizeDirectoryName() throws IOException {

        List<DirectoryNodeDto> directories = repository.findAll();
        List<DirectoryNodeDto> leafs = new ArrayList<>();


        for (DirectoryNodeDto directory : directories) {

            String name = directory.getName();
            Path namePath = Paths.get(name);

            // paths with name count > 1, means complex directory names
            if (namePath.getNameCount() > 1) {
                // Do the magic here
                // split directory name and create simple directories
                leafs.add(createParentsAndRenameLeaf(directory));
            }
        }

        // Must return a list of leafs directories nodes renamed
        return leafs;
    }

    private DirectoryNodeDto createFromRelativeRoute(DirectoryNodeDto parent, Path path, Integer index) throws IOException {

        String actualDirectoryName = path.subpath(index, index + 1).toString();
        String pathBase = parent.getPathBase();

        DirectoryNodeDto result = createDirectory(
                DirectoryNodeDto.builder()
                        .name(actualDirectoryName)
                        .pathBase(pathBase)
                        .build());

        if (result == null) {
            // find directoy node and node
            result = findDirectory(actualDirectoryName, parent.getId());
        }


        if (index == path.getNameCount()) {
            return result;
        }

        assert result != null;
        return createFromRelativeRoute(result, path, ++index);
    }

    private void createParentsAndRenameLeaf(DirectoryNodeDto directory, Path namePath) throws IOException {

        DirectoryNodeDto parent = new DirectoryNodeDto();
        parent.setParentDirectoryId(directory.getParentDirectoryId());

        createFromRelativeRoute(parent, namePath, 0);

//        // complex name has at least one parent
//        Path parents = namePath.getParent();
//        Path leaf = namePath.getFileName();
//
//        // base path + parents path + leaf
//        String basePath = directory.getPathBase();
//
//        DirectoryNodeDto result = createDirectory(
//                DirectoryNodeDto.builder()
//                        .name(parents.getParent()
//                                .getFileName().toString())
//                        .pathBase(
//                                Paths.get(
//                                        basePath,
//                                        parents.toString())
//                                        .toString())
//                        .build());
//
//        DirectoryNodeDto parent;
//        if (result != null) {
//            parent = result;
//
//        } else {
//
//            DirectoryFilterNodeDto filter = DirectoryFilterNodeDto.builder()
//                    .content(
//                            ContentNodeDto.builder()
//                                    .active(true)
//                                    .name()
//                                    .build())
//                    .build();
//        }
    }

    /**
     * Create directory
     *
     * @param directory directory node with id null
     * @return list of directories created, otherwise {@code null}
     */
    @Override
    public DirectoryNodeDto createDirectory(DirectoryNodeDto directory) throws IOException {
        List<DirectoryNodeDto> directories = repository.createDirectory(directory);

        if (directories.isEmpty()) {
            return null;
        }

        return directories.get(directories.size() - 1);
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

    @Override
    public DirectoryNodeDto findDirectory(String name, String parentId) throws IOException {

        ContentNodeDto content = new ContentNodeDto();
        content.setName(name);
        content.setParentDirectoryId(parentId);

        DirectoryFilterNodeDto filter = new DirectoryFilterNodeDto();
        filter.setContent(content);
        filter.setPage(0);
        filter.setSize(20);

        List<DirectoryFilterNodeDto> filters = new ArrayList<>();
        filters.add(filter);

        List<DirectoryNodeDto> results = repository.findDirectoryByFilter(filters);
        if (results.isEmpty()) {
            return null;
        }

        return results.get(0);
    }
}
