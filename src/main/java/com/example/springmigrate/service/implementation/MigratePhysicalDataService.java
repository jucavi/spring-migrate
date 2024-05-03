package com.example.springmigrate.service.implementation;

import com.example.springmigrate.dto.DirectoryNodeDto;
import com.example.springmigrate.dto.FileNodeDto;
import com.example.springmigrate.service.IDirectoryLogicalService;
import com.example.springmigrate.service.IFileLogicalService;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
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
    private final FileTypeMappingService typeMappingService;

    /**
     * Constructor
     *
     * @param directoryLogicalService directory service
     * @param fileLogicalService      file service
     * @param typeMappingService      file type service
     */
    public MigratePhysicalDataService(
            IDirectoryLogicalService directoryLogicalService,
            IFileLogicalService fileLogicalService,
            FileTypeMappingService typeMappingService) {

        this.directoryLogicalService = directoryLogicalService;
        this.fileLogicalService = fileLogicalService;
        this.typeMappingService = typeMappingService;
    }

    /**
     * Create and keep database integrity from physical data
     *
     * @param directoryPath path to physical data
     * @throws IOException if an I/O error occurred
     */
    public void migrate(Path directoryPath) throws IOException {

        // reduce complex names
        // Directories with uuid and complex names
        directoryLogicalService.normalizeDirectoriesNames();
        traverseAndMigrate(directoryPath);
    }


    /**
     * Traverse directories and make migration
     *
     * @param directoryPath directory path
     */
    private void traverseAndMigrate(Path directoryPath) {

        for (Path path : getPathList(directoryPath)) {

            String physicalName = path.getFileName().toString();

            // FILES LOGIC
            // Always absolute path
            if (Files.isRegularFile(path)) {
                try {

                    // Files always have a parent (could be an UUID name)
                    Path directoryPathName = path.getParent().getFileName();

                    Path directoryPathBase;
                    try {
                        directoryPathBase = path.getParent().getParent();

                    } catch (NullPointerException ex) {
                        directoryPathBase = Paths.get(System.getenv("SystemDrive"));
                    }

                    // Create logical directory from physical directory
                    String dName = directoryPathName.getFileName().toString();
                    String dPathBase = directoryPathBase.toString();
                    DirectoryNodeDto parent = directoryLogicalService.createDirectory(dName, dPathBase);

                    if (parent != null && parent.getId() != null) {

                        // Update physicalName
                        if (isValidUUID(physicalName)) {

                            FileNodeDto dto = fileLogicalService.findFileById(physicalName);

                            assert dto != null;

                            // Full path to physical file renamed
                            // if already exist(with prefix (001-, 002, ...) and extension)
                            Path filePathWithPrefix = getPathNameIfDuplicatedFile(path.getParent(), physicalName, getFilenameWithExtension(dto)); // extension added

                            String filename = filePathWithPrefix.getFileName().toString();

                            // Update properties
                            dto.setName(filename);
                            dto.setParentDirectoryId(parent.getId());

                            // update file(logical) with actual physicalName
                            FileNodeDto updated = fileLogicalService.updateFile(dto);

                            if (updated != null) {
                                // update file(physical) with actual logic data updated
                                Files.move(path, Paths.get(updated.getPathBase(), updated.getName()));
                            } else {
                                log.error("Unable to rename '{}' due unable update database record.", filePathWithPrefix);
                            }

                        } else { // valid filename (not UUID) and parent created and not null
//                            FileNodeDto file = new FileNodeDto();
//                            file.setActive(true);
//                            file.setName(physicalName);
//                            file.setParentDirectoryId(parent.getId());
//                            file.setMimeType(Files.probeContentType(path));
//
//                            FileNodeDto created = fileLogicalService.createFile(file);
//
//                            if (created == null) {
//                                log.error("Unable to create '{}' database record.", file);
//                            }
                            log.error("Invalid file name, expected UUID: '{}'.", path);
                        }


                    } else { // directory with physicalName: check and create
                        log.error("***Unable to create file with parent null {}{}{}", directoryPathBase, File.separator, directoryPathName);
                    }

                } catch (FileAlreadyExistsException ex) {
                    // Repeated filenames handled in TraverseDirectory#getFileNameIfDuplicated(Path path, String normFileName)
                    log.warn("File already exists: " + ex.getMessage());
                } catch (IllegalArgumentException ex) {
                    log.error("Interrupted Exception: {}", ex.getMessage());
                } catch (IOException ex) {
                    log.error("I/O error during API request: {}", ex.getMessage());
                }

                // FOLDERS LOGIC
            } else {
                // Valid UUID as name?, rename it.
                if (isValidUUID(physicalName)) {
                    path = renamePhysicalDirectoryNamedWithUUID(path);
                }

                traverseAndMigrate(path);
            }

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
     * Create complete file name with correct extension
     *
     * @param fileDto logical file
     * @return filename with correct extension
     */
    @NotNull
    private String getFilenameWithExtension(@NotNull FileNodeDto fileDto) {

        String filename = fileDto.getName();
        String mimeType = fileDto.getMimeType();
        // Could raise NullPointerException
        String extension = typeMappingService.getFileExtension(mimeType);

        if (!filename.endsWith(extension)) {
            return filename.concat(extension);
        }

        return filename;
    }

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
     * Check for duplicated files in physical storage and return unique filename
     *
     * @param parentPath absolute parent directory path
     * @param filename   filename
     */
    public Path getPathNameIfDuplicatedFile(@NotNull Path parentPath, String filename, String normFileName) {
        File finalFileName = new File(
                Paths.get(parentPath.toString(), normFileName).toString());
        int prefix = 1;

        while (finalFileName.exists()) {
            finalFileName = new File(
                    Paths.get(parentPath.toString(), String.format("%03d-%s", prefix, normFileName)).toString());
            prefix++;
        }

        return finalFileName.toPath();
    }


    /**
     * Validate UUID
     *
     * @param name name
     * @return {@code true} if element name is a valid UUID, {@code false} otherwise
     */
    private static Boolean isValidUUID(String name) {
        try {
            // for validation only
            UUID.fromString(name);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return true;
    }

}
