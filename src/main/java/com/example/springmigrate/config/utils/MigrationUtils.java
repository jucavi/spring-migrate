package com.example.springmigrate.config.utils;

import com.example.springmigrate.config.utils.error.NoRequirementsMeted;
import com.example.springmigrate.config.utils.error.NodeAlreadyProcessed;
import com.example.springmigrate.dto.DirectoryNodeDto;
import com.example.springmigrate.dto.FileNodeDto;
import com.example.springmigrate.model.DirectoryPhysical;
import com.example.springmigrate.model.FilePhysical;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

@Log4j2
public class MigrationUtils {

    /**
     * Check if physical and logical representations have same name and same parent directory
     *
     * @param dto          logical file node
     * @param filePhysical physical file
     * @param foundNode    logical node where the files that have been migrated are stored
     * @param notFoundNode logical node where the files that have not been migrated because they do not have a
     *                     physical representation are stored
     * @param mimeTypes    map of mime types
     * @return {@code true} if the names and parents match, otherwise {@code false}
     * @throws IOException          if I/O exception occurred
     * @throws NodeAlreadyProcessed if the file has already been migrated
     */
    public static boolean isLogicalRepresentationOfDirectory(
            @NotNull FileNodeDto dto,
            FilePhysical filePhysical,
            @NotNull DirectoryNodeDto foundNode,
            DirectoryNodeDto notFoundNode,
            Map<String, String> mimeTypes) throws IOException {

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
        boolean isProcessed = dto.getParentDirectoryId().equals(foundNode.getId())
                || dto.getParentDirectoryId().equals(notFoundNode.getId());

        if (isProcessed) {
            throw new NodeAlreadyProcessed("Node already processed.");
        }

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
     * @throws IOException if I/O exception occurred
     */
    public static String setFileNameWithExtension(
            @NotNull FilePhysical filePhysical,
            @NotNull Map<String, String> mimeTypes) throws IOException {

        String physicalName = filePhysical.getName();
        // get mimetype from metadata
        String mimeType = Files.probeContentType(filePhysical.getAbsolutePath());

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
    public static DirectoryPhysical createDirectoryPhysical(@NotNull Path directoryPath) throws NoRequirementsMeted, IOException {
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
            try (Stream<Path> files = Files.walk(directory)
                    .parallel()
                    .filter(p -> p.toFile().isFile())) {

                long numFiles = files.count();
                log.info("Total files remaining in {}: {}", directory, numFiles);

            } catch (IOException ex) {
                //
            }
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
}
