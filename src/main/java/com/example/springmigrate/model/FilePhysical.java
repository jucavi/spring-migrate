package com.example.springmigrate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
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
     * @param name      file name without extension
     * @param extension extension
     * @param filePath  absolute path
     */
    public FilePhysical(@NotNull String name, @NotNull String extension, @NotNull Path filePath) {
        this.name = name;
        this.extension = extension.startsWith(".") && !extension.equals(".") ? extension : ".".concat(extension);
        this.parentDirectory = new DirectoryPhysical(filePath.getParent());
    }

    public String getFileName() {
        return extension == null ? name : name.concat(extension);
    }

    public Path getAbsolutePath() {
        return Paths.get(parentDirectory.getFullPath(), getFileName());
    }

    public Path getAbsolutePathWithoutExtension() {
        return Paths.get(parentDirectory.getFullPath(), getName());
    }

    public Boolean isFullNameWithExtension() {
        String extension = FilenameUtils.getExtension(this.getAbsolutePath().toString());

        return !extension.isBlank();
    }

    @NotNull
    private Path getFileNamePath() {
        return Paths.get(this.getFileName());
    }

    public String getParentPath() {
        return this.parentDirectory.getFullPath();
    }

    /**
     * Returns the mime type of file
     */
    public String getMimeType() {

        try {
            return Files.probeContentType(getAbsolutePath());
        } catch (IOException e) {
            return "";
        }

    }
}