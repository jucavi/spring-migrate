package com.example.springmigrate.service.implementation;

import com.example.springmigrate.service.IDirectoryLogicalService;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class Migrate {

    private final IDirectoryLogicalService directoryLogicalService;

    public Migrate(IDirectoryLogicalService directoryLogicalService) {
        this.directoryLogicalService = directoryLogicalService;
    }

    public void migrate(Path directoryPath) throws IOException {

        // reduce complex names
        directoryLogicalService.normalizeDirectoriesNames();
        traverseAndMigrate(directoryPath);
    }

    private static void traverseAndMigrate(Path directoryPath) {
        // TODO: LOGIC HERE
    }


    /**
     * Validate UUID
     *
     * @param candidate string
     * @return {@code true} if candidate is a valid UUID, {@code false} otherwise
     */
    private static Boolean isValidUUID(String candidate) {
        try {
            UUID.fromString(candidate);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return true;
    }

}
