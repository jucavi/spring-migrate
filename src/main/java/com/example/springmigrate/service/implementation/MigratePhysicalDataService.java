package com.example.springmigrate.service.implementation;

import com.example.springmigrate.dto.DirectoryNodeDto;
import com.example.springmigrate.service.IDirectoryLogicalService;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
@Log4j2
public class MigratePhysicalDataService {

    private final IDirectoryLogicalService directoryLogicalService;
    private final FileTypeMappingService typeMappingService;

    public MigratePhysicalDataService(
            IDirectoryLogicalService directoryLogicalService,
            FileTypeMappingService typeMappingService) {

        this.directoryLogicalService = directoryLogicalService;
        this.typeMappingService = typeMappingService;
    }

    public void migrate(Path directoryPath) throws IOException {

        // reduce complex names
        directoryLogicalService.normalizeDirectoriesNames();
        traverseAndMigrate(directoryPath);
    }

    private void traverseAndMigrate(Path directoryPath) {

        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directoryPath)) {

            for (Path path : dirStream) {

                if (Files.isRegularFile(path)) {
                    // TODO: LOGIC HERE
                    // TODO: Check UUID if valid (RENAME, UPDATE), otherwise(CREATE)

                } else {
                    // TODO: Check UUID and rename if valid

                    if (isValidUUID(path)) {
                        path = renamePhysicalDirectoryNamedWithUUID(path);
                    }

                    traverseAndMigrate(path);
                }

            }

        } catch (IOException ex) {
            log.error("I/O error during API request: {}", ex.getMessage());
        }
    }

    /**
     * Rename folder with UUID
     * @param path directory path
     * @return directory name extracted from database
     */
    private Path renamePhysicalDirectoryNamedWithUUID(Path path) {
        try {
            DirectoryNodeDto dto = directoryLogicalService.findDirectoryById(path.getFileName().toString());

            if ( dto != null) {
                Path newName =  path.resolveSibling(dto.getName());
                Files.move(path, newName);
                return newName;
            }
        } catch (IOException e) {
            // can't rename a directory
        }

        return path;
    }


    /**
     * Validate UUID
     *
     * @param path element path
     * @return {@code true} if element name is a valid UUID, {@code false} otherwise
     */
    private static Boolean isValidUUID(Path path) {
        try {
            // for validation only
            UUID.fromString(path.getFileName().toString());
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return true;
    }

}
