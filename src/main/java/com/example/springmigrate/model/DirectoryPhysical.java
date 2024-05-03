package com.example.springmigrate.model;

import lombok.*;

import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@Builder
@RequiredArgsConstructor
public class DirectoryPhysical {
    private final Path path;

    /**
     * Returns parent path
     */
    public DirectoryPhysical getParent() {
        Path parent = path.getParent();

        if (parent == null) {
            parent = Paths.get(System.getenv("SystemDrive"));
        }

        return new DirectoryPhysical(parent);
    }


    /**
     * Returns directory name
     */
    public String getName() {
        return path.getFileName().toString();
    }


    /**
     * Returns base path of a directory
     */
    public String getBasePath() {
        return getParent().toString();
    }
}
