package com.example.springmigrate.service.implementation;

import com.example.springmigrate.dto.DirectoryNodeDto;
import com.example.springmigrate.dto.FileNodeDto;
import com.example.springmigrate.model.DirectoryPhysical;
import com.example.springmigrate.model.FilePhysical;
import com.example.springmigrate.service.IDirectoryLogicalService;
import com.example.springmigrate.service.IFileLogicalService;
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

    private void fileProcessLogic(FilePhysical filePhysicalUUID) throws IOException, IllegalArgumentException {

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
     * @param parentLogical logical parent directory extracted from physical route
     * @throws IOException I/O exception during service call
     */
    private void migrateData(FilePhysical filePhysicalUUID, DirectoryNodeDto parentLogical) throws IOException, IllegalArgumentException {
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
     * @param  file file with dto uuid as name
     * @return physical file representation
     */
    @NotNull
    private FilePhysical getFilenameWithExtension(@NotNull FileNodeDto fileDto, FilePhysical file) {

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
                file.getParentDirectory());
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
     * Check for duplicated files in physical storage and return a file with unique filename
     *
     * @param parentPath absolute parent directory path
     * @param filename   filename
     */
    /**
     * Check for duplicated files in physical storage and return a file with unique filename
     *
     * @param filePhysical physical file
     * @return new file with unique name in storage path
     */
    public FilePhysical getPathNameIfDuplicatedFile(FilePhysical filePhysical) {
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
