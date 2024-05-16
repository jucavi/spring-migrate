package com.example.springmigrate.service.implementation;

import com.example.springmigrate.dto.ContentDirectoryNodeDto;
import com.example.springmigrate.dto.DirectoryFilterNodeDto;
import com.example.springmigrate.dto.DirectoryNodeDto;
import com.example.springmigrate.dto.FileNodeDto;
import com.example.springmigrate.model.DirectoryPhysical;
import com.example.springmigrate.model.FilePhysical;
import com.example.springmigrate.service.IDirectoryLogicalService;
import com.example.springmigrate.service.IFileLogicalService;
import com.example.springmigrate.service.IFileTypeLogicalService;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
public class MigratePhysicalDataService {

    private final IDirectoryLogicalService directoryLogicalService;
    private final IFileLogicalService fileLogicalService;

    private final Map<String, String> mimeTypes;

    /**
     * Constructor
     *
     * @param directoryLogicalService directory service
     * @param fileLogicalService      file service
     * @param fileTypeService         file type service
     */
    public MigratePhysicalDataService(
            IDirectoryLogicalService directoryLogicalService,
            IFileLogicalService fileLogicalService,
            IFileTypeLogicalService fileTypeService) throws IOException {

        this.directoryLogicalService = directoryLogicalService;
        this.fileLogicalService = fileLogicalService;
        this.mimeTypes = fileTypeService.findAllFileTypes();
    }

    /**
     * Create and keep database integrity from physical data
     *
     * @param directories list of paths to physical data
     */
    public void migrate(List<Path> directories) {

        // reduce complex names
        //Directories with uuid and complex names
        for (Path directoryPath : directories) {
            log.info("Working on {}", directoryPath);
            log.info("Normalizing folder names...");
            List<DirectoryNodeDto> leafs = normalizeDirectoriesNames();
            log.info("Migrating...");
            traverseAndMigrate(directoryPath);
        }
    }


    /**
     * Traverse directories and make migration
     *
     * @param directoryPath directory path
     */
    //TODO: RENAME EXISTING FOLDERS WITHOUT UUID NAME TO LOWER
    private void traverseAndMigrate(Path directoryPath) {

        for (Path path : getPathList(directoryPath)) {

            FilePhysical filePhysicalUUID = FilePhysical
                    .builder()
                    .name(path.getFileName().toString())
                    .parentDirectory(
                            new DirectoryPhysical(path.getParent()))
                    .build();

            // FILES LOGIC
            // Always absolute path
            if (Files.isRegularFile(path)) {

                try {
                    fileProcessLogic(filePhysicalUUID);

                } catch (IllegalArgumentException ex) {
                    log.error("Interrupted Exception: {}", ex.getMessage());
                } catch (IOException ex) {
                    log.error("I/O error during API request: {}", ex.getMessage());
                } catch (Exception ex) {
                    log.error("Unexpected exception occurred: {}", ex.getMessage());
                }


            } else { // FOLDERS LOGIC

                // Valid UUID as name?, rename it from database.
                if (isValidUUID(filePhysicalUUID.getName())) {
                    path = renamePhysicalDirectoryNamedWithUUID(path);
                }

                traverseAndMigrate(path);
            }

        }
    }

    /**
     * File process logic
     *
     * @param filePhysicalUUID File with uuid name
     * @throws IOException              if IOException occurred
     * @throws IllegalArgumentException if IllegalArgument occurred
     */
    private void fileProcessLogic(@NotNull FilePhysical filePhysicalUUID) throws IOException, IllegalArgumentException {

        // Find logical directory with physical info
        DirectoryNodeDto parentLogical = directoryLogicalService.createDirectory(
                filePhysicalUUID
                        .getParentDirectory()
                        .getName(),
                filePhysicalUUID
                        .getParentDirectory()
                        .getBasePath());

        if (parentLogical != null && parentLogical.getId() != null) {

            // Update physicalName
            if (isValidUUID(filePhysicalUUID.getName())) {
                migrateData(filePhysicalUUID, parentLogical);
            } else {
                log.info("Invalid UUID: {}", filePhysicalUUID.getName());
            }

            // TODO: What if we have no UUID file name???

        } else { // directory with physicalName: check and create
            log.error("Unable to find logical parent directory: {}", filePhysicalUUID.getParentDirectory().getPath());
        }
    }

    /**
     * Rename physical files with uuid names based on database information.
     * Always after update move physical files to database base url to keep integrity
     *
     * @param filePhysicalUUID physical file with uuid as filename
     * @param parentLogical    logical parent directory extracted from physical route
     * @throws IOException I/O exception during service call
     */
    private void migrateData(@NotNull FilePhysical filePhysicalUUID, @NotNull DirectoryNodeDto parentLogical) throws IOException, IllegalArgumentException {
        // find logical file by uuid
        FileNodeDto dto = fileLogicalService.findFileById(filePhysicalUUID.getName());

        assert dto != null;
        // complete filename with extension from database information
        FilePhysical filePhysical = getFilenameWithExtension(dto, filePhysicalUUID);

        // Full path to physical file renamed
        // if already exist(with prefix (001-, 002, ...) and extension)
        filePhysical = getPathNameIfDuplicatedFile(filePhysical); // extension added

        // update file(logical) with actual physicalName
        dto.setName(filePhysical.getFileName());
        dto.setParentDirectoryId(parentLogical.getId());
        FileNodeDto updated = fileLogicalService.updateFile(dto);

        // Recheck DB and physical
        if (updated != null) {

            Path originPath = filePhysicalUUID.getAbsolutePathWithoutExtension();
            Path destinyPath = Paths.get(updated.getPathBase(), updated.getName());

            // Renames physical uuid filename with updated data logical
            Files.move(originPath, destinyPath);
        } else {
            log.error("Unable to rename(check rename) '{}' due unable update database record.", filePhysical.getFileName());
        }
    }

    /**
     * Returns a list of paths denoting the files in the directory
     *
     * @param directoryPath directory path
     * @return list of paths denoting the files in the directory
     */
    @NotNull
    private static List<Path> getPathList(Path directoryPath) {

        if (!Files.isDirectory(directoryPath)) {
            return new ArrayList<>();
        }

        return Arrays.stream(
                        Objects.requireNonNull(
                                new File(directoryPath.toString()).listFiles()))
                .map(file -> Paths.get(file.getAbsolutePath()))
                .collect(Collectors.toList());
    }


    /**
     * Create physical directory with correct extensión from database record
     *
     * @param fileDto file dto
     * @param file    file with dto uuid as name
     * @return physical file representation
     */
    @NotNull
    private FilePhysical getFilenameWithExtension(@NotNull FileNodeDto fileDto, FilePhysical file) {

        String filename = fileDto.getName();
        String mimeType = fileDto.getMimeType();
        // Could raise NullPointerException, ClassCastException but dtos have valid mimetypes
        String extension = mimeTypes.get(mimeType);

        if (filename.endsWith(extension)) {
            int index = filename.lastIndexOf(".");
            filename = filename.substring(0, index);
        }

        return new FilePhysical(
                filename, // without extension
                extension,
                file.getParentDirectory());
    }

    /**
     /**
     * Rename folder with UUID
     *
     * @param path directory path
     * @return directory name extracted from database
     */
    private Path renamePhysicalDirectoryNamedWithUUID(@NotNull Path path) {
        try {
            DirectoryNodeDto dto = directoryLogicalService.findDirectoryById(path.getFileName().toString());

            if (dto != null) {
                Path newName = path.resolveSibling(dto.getName());

                // Update physical folder name
                Files.move(path, newName);

                return newName;
            }
        } catch (IOException e) {
            // can't rename a directory
        }

        return path;
    }


    /**
     * Check for duplicated files in physical storage and return a file with unique filename
     *
     * @param filePhysical physical file
     * @return new file with unique name in storage path
     */
    public FilePhysical getPathNameIfDuplicatedFile(@NotNull FilePhysical filePhysical) {
        String name = filePhysical.getName();
        File finalFileName = new File(filePhysical.getAbsolutePath().toString());
        int prefix = 1;

        while (finalFileName.exists()) {
            name = String.format("%03d-%s", prefix, filePhysical.getName());
            finalFileName = new File(
                    Paths.get(
                                    filePhysical
                                            .getParentDirectory()
                                            .getPath()
                                            .toString(),
                                    name.concat(filePhysical.getExtension()))
                            .toString());
            prefix++;
        }

        return FilePhysical.builder()
                .name(name)
                .extension(filePhysical.getExtension())
                .parentDirectory(filePhysical.getParentDirectory())
                .build();
    }


    /**
     * Validate UUID
     *
     * @param name name
     * @return {@code true} if element name is a valid UUID, {@code false} otherwise
     */
    @NotNull
    private static Boolean isValidUUID(String name) {
        try {
            // for validation only
            UUID.fromString(name);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return true;
    }

    /**
     * Returns a list af leafs created
     *
     * @return list of leafs created
     */
    public List<DirectoryNodeDto> normalizeDirectoriesNames() {

        List<DirectoryNodeDto> directories;
        List<DirectoryNodeDto> leafs = new ArrayList<>();

        try {
            directories = directoryLogicalService.findALl();

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
                    directory.setName(directory.getName().toLowerCase());

                    if (directory.getParentDirectoryId() != null) {
                        directory.setPathBase(null);
                    }

                    directoryLogicalService.updateDirectory(directory);
                }
            }

        } catch (IOException e) {
            log.error("I/O error reading directories");
        }

        // Must return a list of leafs directories nodes renamed
        return leafs;
    }

    /**
     * Creates all parents, delete duplicated leaf node, and rename original node as leaf keeping all children references
     *
     * @param directory complex named directory
     * @return leaf node with all children attached
     * @throws IOException if I/O error occurred
     */
    private DirectoryNodeDto createParentsAndRenameLeaf(@NotNull DirectoryNodeDto directory) throws IOException {

        Path namePath = Paths.get(directory.getName());

        // find parent directory
        DirectoryNodeDto rootParent = directoryLogicalService.findDirectoryById(directory.getParentDirectoryId());

        // call with parent directory of complex node
        // and all parents nodes of a leaf in relative path form
        // from directory named -> /d1/d2/d3/leaf with parent -> /parent
        // complex directories always have a parent (minimum complex directory name /d1/d2 -> parent(/d1))
        //      rootParent -> /parent
        //      parent nodes -> /d1/d2/d3
        DirectoryNodeDto lastParent = createFromRelativeRoute(rootParent, namePath.getParent(), directory.getId(), 0);

        String leafName = Paths.get(directory.getName())
                .getFileName()
                .toString();

        DirectoryNodeDto duplicatedDirectory = directoryLogicalService.findDirectoryByParentId(leafName, lastParent.getId());

        if (duplicatedDirectory != null) {
            List<DirectoryNodeDto> children = findChildrenDirectories(duplicatedDirectory.getId());

            for (DirectoryNodeDto child : children) {
                child.setParentDirectoryId(directory.getId());
                child.setPathBase(null);
                // update logical directory
                directoryLogicalService.updateDirectory(child);
            }

            directoryLogicalService.deleteDirectoryHard(duplicatedDirectory.getId());
        }

        // Leaf update
        directory.setName(
                leafName);
        directory.setParentDirectoryId(lastParent.getId());
        directory.setPathBase(null);

        return directoryLogicalService.updateDirectory(directory);
    }

    /**
     * Creates all directories from relative path in parent directory
     *
     * <ul>
     *     <li>Example:
     *     </li>
     *     <li>directory named -> /d1/d2/d3/leaf with parent -> /parent
     *     </li>
     *     <li>rootParent -> /parent
     *     </li>
     *     <li>path -> /d1/d2/d3
     *     </li>
     *
     *     <li>Result:
     *     </li>
     *     <li>/d1 -> /parent
     *     </li>
     *     <li>/d2 -> /d1
     *     </li>
     *     <li>/d3 -> /d2
     *     </li>
     *     <li>returns /d3
     *     </li>
     * <p>
     *     if /parent == null; then update root's parent pointer to first node created
     * </ul>
     *
     * @param parent    parent directory
     * @param path      relative path
     * @param complexId identifier of complex directory name
     * @param index     index of sub-path
     * @return last directory created
     * @throws IOException if I/O error
     */
    private DirectoryNodeDto createFromRelativeRoute(DirectoryNodeDto parent, Path path, String complexId, Integer index) throws IOException {

        String actualDirectoryName = path.subpath(index, index + 1).toString();
        String pathBase = Paths.get(parent.getPathBase(), parent.getName()).toString();
        DirectoryNodeDto result = createDirectory(parent, actualDirectoryName, pathBase);

        // TODO
        // root's parent directory is a root directory (in root directories table)
        // root directory points to complex named node
        // then we need to update root directory with directory node created
        // this can only happen at first call
        if (parent.getParentDirectoryId() == null && index == 0) {
            // Update rootDirectory -> parent.id to rootDirectory -> result.id
            // 1. find all root's directories whose points to complex node identifier

            // 2, iterate over and update DIRECTORY_ID with result identifier
        }

        if (index == path.getNameCount() - 1) {
            return result;
        }

        assert result != null;
        return createFromRelativeRoute(result, path, complexId, ++index);
    }

    /**
     * Creates directory
     *
     * @param parent   candidate parent directory
     * @param name     directory name
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

        DirectoryNodeDto result = directoryLogicalService.createDirectories(request);

        if (result == null) {
            // find directory node
            result = directoryLogicalService.findDirectoryByParentId(name, parent.getId());
        }

        return result;
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

        return directoryLogicalService.findAllDirectoriesByFilter(filter);
    }

}
