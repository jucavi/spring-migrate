package com.example.springmigrate.config.utils;

import com.example.springmigrate.config.utils.error.NoRequirementsMeted;
import com.example.springmigrate.model.DirectoryPhysical;
import com.example.springmigrate.model.FilePhysical;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

@Log4j2
public class MigrationUtils {


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
}
