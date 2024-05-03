package com.example.springmigrate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;

@Data
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class FilePhysical {

    private String name;
    private String extension;
    private DirectoryPhysical parentDirectory;

    /**
     * Constructor
     *
     * @param fullName filename with extension
     * @param filePath absolute path
     */
    public FilePhysical(@NotNull String fullName, @NotNull Path filePath) {

    }

    /**
     * Constructor
     *
     * @param name file name without extension
     * @param extension extension
     * @param filePath absolute path
     */
    public FilePhysical(@NotNull String name, @NotNull String extension, @NotNull Path filePath) {
        this.name = name;
        this.extension = extension.startsWith(".") ? extension : ".".concat(extension);
        this.parentDirectory = new DirectoryPhysical(filePath.getParent());
    }

    public String getFileName() {
        return name.concat(extension);
    }

    public Path getAbsolutePath() {
        return Paths.get(parentDirectory.getFullPath(), getFileName());
    }

    public Path getAbsolutePathWithoutExtension() {
        return Paths.get(parentDirectory.getFullPath(), getName());
    }

    private Path getFileNamePath() {
        return Paths.get(this.getFileName());
    }
}