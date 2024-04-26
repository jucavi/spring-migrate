package com.example.springmigrate.service.implementation;

import com.example.springmigrate.dto.DirectoryNodeDto;
import com.example.springmigrate.repository.IDirectoryRepository;
import com.example.springmigrate.service.IDirectoryLogicalService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
@AllArgsConstructor
public class DirectoryLogicalServiceImpl implements IDirectoryLogicalService {

    private final IDirectoryRepository repository;

    @Override
    public void normalizeDirectoryName() throws IOException {
        List<DirectoryNodeDto> directories = repository.findAll();

        for (DirectoryNodeDto directory : directories) {

            String name = directory.getName();
            Path namePath = Paths.get(name);

            // paths with name count > 1, means complex directory names
            if (namePath.getNameCount() > 1) {
                // Do the magic here
                // split directory name and create simple directories
                createParentsAndRenameLeaf(directory, namePath);
            }
        }
    }

    private DirectoryNodeDto createFromRelativeRoute(String pathBase, Path path, Integer index) throws IOException {

        String actualDirectoryName = path.subpath(index, index + 1).toString();

        DirectoryNodeDto result = createDirectory(
                DirectoryNodeDto.builder()
                        .name(actualDirectoryName)
                        .pathBase(pathBase)
                        .build());

        if (result == null) {
            //
        }


        if (index == path.getNameCount()) {
            return result;
        }

        assert result != null;
        return createFromRelativeRoute(result.getPathBase(), path, ++index);
    }

    private void createParentsAndRenameLeaf(DirectoryNodeDto directory, Path namePath) throws IOException {

        String name = namePath.getFileName().toString();

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
}
