package com.example.springmigrate.config.utils;

import com.example.springmigrate.config.utils.error.NoRequirementsMeted;
import com.example.springmigrate.dto.FileNodeDto;
import com.example.springmigrate.model.DirectoryPhysical;
import com.example.springmigrate.model.FilePhysical;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Log4j2
public class MigrationUtils {

    /**
     * Move all directory content to destiny directory
     *
     * @param srcPath source directory
     * @param dstPath destination directory
     * @throws IOException if IOException occurred
     */
    public static void copyDirectoryContentTo(@NotNull Path srcPath, Path dstPath) throws IOException {
        // (Is folder) for base recursive implementation
        if (!Files.exists(dstPath)) {
            // different folders: RENAME
            Files.move(srcPath, dstPath);

        } else {
            // same folders, one of them already renamed
            for (Path child : MigrationUtils.getPathList(srcPath)) {

                if (Files.isRegularFile(child)) {
                    try {
                        Path p = Paths.get(dstPath.toString(), child.getFileName().toString());
                        // copy if not exists
                        if (!Files.exists(p)) {
                            Files.copy(child, p);
                        }

                        // delete file after copy from source only if folder was renamed
                        if (!p.equals(child)) {
                            Files.delete(child);
                        }

                    } catch (IOException ex) {
                        log.error("Something fishy: {}", ex.getMessage());
                    }
                }
            }

            // Delete empty source directory
            Files.delete(srcPath);
        }
    }

    /**
     * Returns the number of files inside a directory
     *
     * @param directoryPath directory path
     * @return number of files
     */
    public static long fileCount(Path directoryPath) {
        try (Stream<Path> files = Files.walk(directoryPath)
                .parallel()
                .filter(p -> p.toFile().isFile())) {

            return files.count();

        } catch (IOException ex) {
            return -1;
        }
    }


    /**
     * Returns a list of paths denoting the files in the directory
     *
     * @param directoryPath directory path
     * @return list of paths denoting the files in the directory
     */
    @NotNull
    public static List<Path> getPathList(Path directoryPath) {

        if (!Files.isDirectory(directoryPath)) {
            return new ArrayList<>();
        }

        return Arrays.stream(
                        Objects.requireNonNull(
                                new File(directoryPath.toString())
                                        .listFiles()))
                .map(file -> Paths.get(file.getAbsolutePath()))
                .collect(Collectors.toList());
    }

    /**
     * Check if physical and logical representations have same name and same parent directory
     *
     * @param dto          logical file node
     * @param filePhysical physical file
     * @param mimeTypes    map of mime types
     * @return {@code true} if the names and parents match, otherwise {@code false}
     */
    public static boolean isLogicalRepresentationOfDirectory(
            @NotNull FileNodeDto dto,
            FilePhysical filePhysical,
            Map<String, String> mimeTypes)  {

        // add extension from metadata
        String physicalName = setFileNameWithExtension(filePhysical, mimeTypes);
        String nodeName = dto.getName();

        /* Use cases:
              nodeName without extension (name)
              nodeName with extension (name.pdf)
              fullNodeName name with extension (name.pdf)
              fullNodeName name with duplicate extension (name.pdf.pdf)
         */
        String fullNodeName = nodeName.concat(mimeTypes.get(dto.getMimeType())).toLowerCase();
        //physicalName.matches("^\\d{3}-");
        boolean isEqualName = (physicalName.equals(nodeName) || physicalName.equals(fullNodeName));

        // normalize path bases (api response -> pathBase='opttoolstomcatlatest/Documents/1/VT')
        boolean isEqualPathBase = filePhysical.getParentPath()
                .replace(File.separator, "")
                .equals(dto.getPathBase().replace(File.separator, ""));

        return isEqualName && isEqualPathBase;
    }

    /**
     * Move file to found directory in physical storage
     *
     * @param file file
     * @throws IOException if IOException occurred
     */
    @NotNull
    public static FilePhysical movePhysicalFile(
            @NotNull FilePhysical file,
            @NotNull String newName,
            @NotNull DirectoryPhysical parent) throws IOException {

        Path sourcePath = file.getAbsolutePath();

        file.setName(newName);
        file.setParentDirectory(parent);
        FilePhysical renamed = getPathNameIfDuplicatedFile(file);

        Files.move(sourcePath, renamed.getAbsolutePath());
        return renamed;
    }


    /**
     * Add extension to filename
     *
     * @param filePhysical physical file object
     * @param mimeTypes    available mapping of mime types(mimetype, extension)
     * @return filename with extension
     */
    public static String setFileNameWithExtension(
            @NotNull FilePhysical filePhysical,
            @NotNull Map<String, String> mimeTypes) {

        String physicalName = filePhysical.getName();
        // get mimetype from metadata
        String mimeType = filePhysical.getMimeType();

        // Try set extension
        if (!filePhysical.isFullNameWithExtension()) {
            try {
                physicalName = filePhysical.getName().toLowerCase().concat(".").concat(mimeTypes.get(mimeType));
            } catch (NullPointerException | ClassCastException ex) {
                // mime type not present
            }
        }
        return physicalName;
    }

    /**
     * Check for duplicated files in physical storage and return a file with unique filename
     *
     * @param filePhysical physical file
     * @return new file with unique name in storage path
     */
    public static FilePhysical getPathNameIfDuplicatedFile(@NotNull FilePhysical filePhysical) {
        String name = filePhysical.getName().toLowerCase();
        File finalFileName = new File(filePhysical.getAbsolutePath().toString().toLowerCase());
        int prefix = 1;


        while (finalFileName.exists()) {
            name = String.format("%03d-%s", prefix, filePhysical.getName().toLowerCase());
            finalFileName = new File(Paths.get(filePhysical.getParentDirectory().getPath().toString(), name.concat(filePhysical.getExtension() == null ? "" : filePhysical.getExtension().toLowerCase())).toString());
            prefix++;
        }

        return FilePhysical.builder()
                .name(name)
                .extension(filePhysical.getExtension() == null ? "" : filePhysical.getExtension())
                .parentDirectory(filePhysical.getParentDirectory())
                .build();
    }

    /**
     * Create physical directory with correct extensión from database record
     * in found physical directory as parent
     *
     * @param fileDto        file dto
     * @param foundDirectory physical directory to store logical files founded
     * @param mimeTypes      map with accepted mimetypes
     * @return physical file object
     * @see FilePhysical
     * @see DirectoryPhysical
     */
    @NotNull
    public static FilePhysical getFilenameWithExtension(
            @NotNull FileNodeDto fileDto,
            DirectoryPhysical foundDirectory,
            @NotNull Map<String, String> mimeTypes) {

        String filename = fileDto.getName();
        String mimeType = fileDto.getMimeType();
        // Could raise NullPointerException
        String extension = mimeTypes.get(mimeType);

        // needed for instantiate
        if (filename.endsWith(extension)) {
            int index = filename.lastIndexOf(".");
            filename = filename.substring(0, index);
        }

        return new FilePhysical(filename, // without extension
                extension, foundDirectory);
    }

    /**
     * Creates physical directory and returns a physical directory created
     *
     * @param directoryPath absolute directory path
     * @return physical directory created
     * @throws NoRequirementsMeted if cant create a directory
     * @see DirectoryPhysical
     */
    @NotNull
    @Contract("_ -> new")
    public static DirectoryPhysical createDirectoryPhysical(@NotNull Path directoryPath) throws NoRequirementsMeted {
        // Attempt to create the directory
        try {
            if (!Files.exists(directoryPath)) {
                Files.createDirectories(directoryPath);
            }
        } catch (Exception ex) {
            throw new NoRequirementsMeted("Unable to create necessary directory");
        }

        return new DirectoryPhysical(directoryPath);
    }

    /**
     * Show migration status resume
     *
     * @param found             directory where the files found in the database are stored
     * @param notFound          directory where the files not found in the database are stored
     * @param sourceDirectories directories on which the migration has been performed
     */
    public static void showResume(@NotNull DirectoryPhysical found, @NotNull DirectoryPhysical notFound, @NotNull List<Path> sourceDirectories) {

        File file = new File(found.getFullPath());
        int filesFound = Objects.requireNonNull(file.listFiles(File::isFile)).length;

        file = new File(notFound.getFullPath());
        int filesMissing = Objects.requireNonNull(file.listFiles(File::isFile)).length;

        log.info("*******************************************************");
        log.info("*******************************************************");
        log.info("                After Migrate Operation");
        log.info("*******************************************************");
        log.info("*******************  Source  **************************");
        for (Path directory : sourceDirectories) {
            log.info("Total files remaining in {}: {}", directory, fileCount(directory));
        }

        log.info("*******************************************************");
        log.info("*******************  Target  **************************");
        log.info("Total files processed: {}", filesFound + filesMissing);
        log.info("*******************************************************");
        log.info("Founded files in {}: {}", found.getFullPath(), filesFound);
        log.info("Missing files in {}: {}", notFound.getFullPath(), filesMissing);
        log.info("*******************************************************");
    }

    /**
     * Validate UUID
     *
     * @param name name
     * @return {@code true} if element name is a valid UUID, {@code false} otherwise
     */
    @NotNull
    public static Boolean isValidUUID(String name) {
        try {
            // for validation only
            UUID.fromString(name);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return true;
    }

    /**
     * Delete all physical directory structure after migrate if all files was processed
     *
     * @param sourceDirectories list of directories
     */
    public static void cleanPhysicalSourceDirectories(@NotNull List<Path> sourceDirectories) {
        for (Path directory : sourceDirectories) {
            long count = fileCount(directory);

            if (count == 0) {
                try {
                    FileUtils.deleteDirectory(new File(directory.toString()));
                    log.info("Delete after migrate: {}", directory);
                } catch (IOException e) {
                    log.warn("Unable to delete: {}", directory);
                }
            } else {
                log.warn("Check directory content, not all data was migrated: {}", directory);
            }
        }
    }
}
