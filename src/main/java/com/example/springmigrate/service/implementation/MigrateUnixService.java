package com.example.springmigrate.service.implementation;

import com.example.springmigrate.config.utils.error.NoRequirementsMeted;
import com.example.springmigrate.dto.DirectoryNodeDto;
import com.example.springmigrate.dto.FileNodeDto;
import com.example.springmigrate.dto.RootNodeDto;
import com.example.springmigrate.dto.UnixRootDto;
import com.example.springmigrate.model.DirectoryPhysical;
import com.example.springmigrate.model.FilePhysical;
import com.example.springmigrate.service.IDirectoryLogicalService;
import com.example.springmigrate.service.IFileLogicalService;
import com.example.springmigrate.service.IRootDirectoryService;
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
public class MigrateUnixService {

    private final IDirectoryLogicalService directoryLogicalService;
    private final IFileLogicalService fileLogicalService;
    private final FileTypeMappingService typeMappingService;
    private final IRootDirectoryService rootDirectoryService;
    private UnixRootDto unixRoot;

    /**
     * Constructor
     *
     * @param directoryLogicalService directory service
     * @param fileLogicalService      file service
     * @param typeMappingService      file type service
     */
    public MigrateUnixService(
            IDirectoryLogicalService directoryLogicalService,
            IFileLogicalService fileLogicalService,
            FileTypeMappingService typeMappingService,
            IRootDirectoryService rootDirectoryService) {

        this.directoryLogicalService = directoryLogicalService;
        this.fileLogicalService = fileLogicalService;
        this.typeMappingService = typeMappingService;
        this.rootDirectoryService = rootDirectoryService;
    }

    /**
     * Create and keep database integrity from physical data
     *
     * @throws IOException if an I/O error occurred
     */
    public void migrate(String name, String pathBase, List<Path> directories) throws IOException, NoRequirementsMeted {

        // Create logical and physical node
        unixRoot = setupNodes(name, pathBase);
        makeMigration(directories);
        deleteRoots();
        deleteDirectories();
    }

    /**
     * Deletes all directories except {@code unixRoot} created
     *
     * @throws IOException if IOException occurred
     */
    private void deleteDirectories() throws IOException {

        List<DirectoryNodeDto> directories = directoryLogicalService.findALl();

        assert unixRoot != null;
        String directoryId = unixRoot.getRootNode().getId();

        for (DirectoryNodeDto directory : directories) {
            if (!directory.getId().equals(directoryId)) {
                directoryLogicalService.deleteDirectoryHard(directoryId);
            }
        }
    }

    /**
     * Deletes all root directories except the one whose child is {@code unixRoot}
     *
     * @throws IOException if IOException occurred
     */
    private void deleteRoots() throws IOException {

        List<RootNodeDto> roots = rootDirectoryService.findAll();

        assert unixRoot != null;
        String directoryId = unixRoot.getRootNode().getId();

        for (RootNodeDto root : roots) {
            String childDirectoryId = root.getDirectoryNodeDto().getId();

            if (!childDirectoryId.equals(directoryId)) {
                rootDirectoryService.deleteByDirectoryId(directoryId);
            }
        }
    }

    /**
     * Makes migration process
     *
     * @param directories list of all path to migrate
     */
    private void makeMigration(List<Path> directories) {

        for (Path directory : directories) {
            traverse(directory);
        }
    }

    /**
     * Traverse a path, rename all physical files and points them to {@code unixRoot}, then
     * moves physical directories to {@code unixRoot}
     *
     * @param directoryPath path
     */
    private void traverse(Path directoryPath) {

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
                    if (isValidUUID(filePhysicalUUID.getName())) {
                        migrateData(filePhysicalUUID);
                    }

                } catch (IllegalArgumentException ex) {
                    log.error("Interrupted Exception: {}", ex.getMessage());
                } catch (IOException ex) {
                    log.error("I/O error during API request: {}", ex.getMessage());
                } catch (Exception ex) {
                    log.error("Unexpected exception occurred: {}", ex.getMessage());
                }


            } else { // FOLDERS LOGIC
                traverse(path);
            }

        }
    }


    // Changed
    /**
     * Rename physical files with uuid names based on database information.
     * And move to {@code unixRote}
     *
     * @param filePhysicalUUID physical file with uuid as filename
     * @throws IOException I/O exception during service call
     */
    private void migrateData(@NotNull FilePhysical filePhysicalUUID) throws IOException, IllegalArgumentException {
        // find logical file by uuid
        FileNodeDto dto = fileLogicalService.findFileById(filePhysicalUUID.getName());

        assert dto != null;
        // complete filename with extension from database information, and with unix root parent
        FilePhysical filePhysical = getFilenameWithExtension(dto);

        // Full path to physical file renamed
        // if already exist(with prefix (001-, 002, ...) and extension)
        filePhysical = getPathNameIfDuplicatedFile(filePhysical); // extension added

        String oldParentId = dto.getParentDirectoryId();

        log.info("Unattached file with old parent ID: {}", oldParentId);

        // update file(logical) with actual physicalName
        // and unix root parent directory
        dto.setName(filePhysical.getFileName());
        dto.setParentDirectoryId(unixRoot.getRootNode().getId());
        FileNodeDto updated = fileLogicalService.updateFile(dto);

        // delete old logical parent
        if (updated != null) {
            Path originPath = filePhysicalUUID.getAbsolutePathWithoutExtension();
            Path destinyPath = Paths.get(unixRoot.getRootDirectory().getFullPath(), updated.getName());

            // Renames physical uuid filename with updated data logical
            Files.move(originPath, destinyPath);
        } else {
            log.error("Unable to rename(check rename) '{}' due unable update database record.", filePhysical.getFileName());
        }
    }

    /**
     * Creates logical and physical directory
     *
     * @param name directory name
     * @param pathBase directory path base
     *
     * @return unix root dto with logical and physical info
     * @throws IOException if IOException occurred
     * @throws NoRequirementsMeted If the physical or logical path cannot be created
     */
    private UnixRootDto setupNodes(String name, String pathBase)
            throws IOException, NoRequirementsMeted {

        Path path = Paths.get(pathBase, name);

        DirectoryPhysical rootDirectory = new DirectoryPhysical(path);
        DirectoryNodeDto rootNode = directoryLogicalService.createDirectory(name, pathBase);

        File directory = new File(path.toString());
        // Attempt to create the directory
        boolean directoryCreated = directory.mkdir();

        // if root node is null the folder already exist and could be integrity problems
        if (!directoryCreated || rootNode == null) {
            throw new NoRequirementsMeted("Unable to create necessary scaffold");
        }

        return new UnixRootDto(rootDirectory, rootNode);
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


    // Changed
    /**
     * Create physical directory with correct extensión from database record
     *
     * @param fileDto file dto
     * @return physical file representation
     */
    @NotNull
    private FilePhysical getFilenameWithExtension(@NotNull FileNodeDto fileDto) {

        String filename = fileDto.getName();
        String mimeType = fileDto.getMimeType();
        // Could raise NullPointerException
        String extension = typeMappingService.getFileExtension(mimeType);

        if (filename.endsWith(extension)) {
            int index = filename.lastIndexOf(".");
            filename = filename.substring(0, index);
        }

        return new FilePhysical(
                filename, // without extension
                extension,
                unixRoot.getRootDirectory());
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
}
