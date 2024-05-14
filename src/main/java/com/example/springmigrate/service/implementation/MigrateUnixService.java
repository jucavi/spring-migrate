package com.example.springmigrate.service.implementation;

import com.example.springmigrate.config.utils.error.NoRequirementsMeted;
import com.example.springmigrate.dto.*;
import com.example.springmigrate.model.DirectoryPhysical;
import com.example.springmigrate.model.FilePhysical;
import com.example.springmigrate.service.IDirectoryLogicalService;
import com.example.springmigrate.service.IFileLogicalService;
import com.example.springmigrate.service.IRootDirectoryService;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Log4j2
public class MigrateUnixService {

    private final IDirectoryLogicalService directoryLogicalService;
    private final IFileLogicalService fileLogicalService;
    private final FileTypeMappingService typeMappingService;
    private final IRootDirectoryService rootDirectoryService;

    private PhysicalLogicalDirectoryDto unixRoot;
    private DirectoryNodeDto directoryNotFoundLogical;
    private DirectoryPhysical unixDirectoryNotFoundInDatabase;
    private List<DirectoryNodeDto> unlinkedDirectories;
    private List<FileNodeDto> unlinkedFiles;

    /**
     * Constructor
     *
     * @param directoryLogicalService directory service
     * @param fileLogicalService      file service
     * @param typeMappingService      file type service
     */
    public MigrateUnixService(
            IDirectoryLogicalService directoryLogicalService,
            IFileLogicalService fileLogicalService,
            FileTypeMappingService typeMappingService,
            IRootDirectoryService rootDirectoryService) {

        this.directoryLogicalService = directoryLogicalService;
        this.fileLogicalService = fileLogicalService;
        this.typeMappingService = typeMappingService;
        this.rootDirectoryService = rootDirectoryService;
    }

    /**
     * Create and keep database integrity from physical data
     *
     * @throws IOException if an I/O error occurred
     */
    public void migrate(String pathBase, String foundDirectoryName, String notFoundDirectoryName, List<Path> directories)
            throws IOException, NoRequirementsMeted {

        // Create logical and physical node
        log.info("Creating directories...");
        setupNodes(pathBase, foundDirectoryName, notFoundDirectoryName);
        makeMigration(directories);
        log.info("Deleting roots...");
        deleteRoots();
        log.info("Deleting Directories...");
        deleteDirectories();

        // Show statistics
        showResume(directories);
    }

    private void showResume(List<Path> directories) {

        File file = new File(unixRoot.getDirectory().getFullPath());
        int foundedFiles = Objects.requireNonNull(file.listFiles(File::isFile)).length;

        file = new File(unixDirectoryNotFoundInDatabase.getFullPath());
        int notFoundedFiles = Objects.requireNonNull(file.listFiles(File::isFile)).length;

        log.info("*******************************************************");
        log.info("*******************************************************");
        log.info("                After Migrate Operation");
        log.info("*******************************************************");
        log.info("*******************************************************");
        for (Path directory : directories) {
            try (Stream<Path> files = Files.walk(directory).parallel().filter(p -> p.toFile().isFile())) {
                long numFiles = files.count();
                log.info("*******************  Source  **************************");
                log.info("Total files remaining in {}: {}", directory, numFiles);
            } catch (IOException ex) {
                //
            }
        }

        log.info("*******************  Target  **************************");
        log.info("Total files processed: {}", foundedFiles + notFoundedFiles);
        log.info("*******************************************************");
        log.info("Founded files in (/old): {}", foundedFiles);
        log.info("Missing files in (/notfound/database): {}", notFoundedFiles);
        log.info("*******************************************************");
    }

    /**
     * Deletes all directories except {@code unixRoot} created
     *
     * @throws IOException if IOException occurred
     */
    private void deleteDirectories() throws IOException {

        String foundId = unixRoot.getNode().getId();
        String notFoundId = directoryNotFoundLogical.getId();

        for (DirectoryNodeDto directory : unlinkedDirectories) {
            // directory is one of new nodes created
            String id = directory.getId();
            boolean isNewNode = id.equals(foundId) || id.equals(notFoundId);

            if (!isNewNode) {
                try {
                    directoryLogicalService.deleteDirectoryHard(id);
                } catch (Exception ex) {
                    log.error("Error deleting {}", directory.getId());
                }
            }
        }
    }

    /**
     * Deletes all root directories except the one whose child is {@code unixRoot}
     *
     * @throws IOException if IOException occurred
     */
    private void deleteRoots() throws IOException {

        // Get all directories before unlink them from root
        unlinkedDirectories = directoryLogicalService.findALl();
        // Get all files before unlink from root
        unlinkedFiles = fileLogicalService.findAll();

        // Move files that have a parent yet
        moveLinkedDirectoriesToNotFound();

        // delete root data
        rootDirectoryService.truncate();

        // create again root directory
        RootNodeDto root = new RootNodeDto();
        root.setPathBase(unixRoot.getNode().getPathBase());
        root.setDirectory(unixRoot.getNode());

        rootDirectoryService.createRoot(root);
    }

    /**
     * Move to {@code directoryNotFoundLogical} files that can not find in physical storage
     *
     * @throws IOException if IOException occurred
     */
    private void moveLinkedDirectoriesToNotFound() throws IOException {

        String directoryFoundId = unixRoot.getNode().getId();
        String directoryNotFoundId = directoryNotFoundLogical.getId();

        for (FileNodeDto child : unlinkedFiles) {
            String parentId = child.getParentDirectoryId();

            if (!parentId.equals(directoryFoundId) && !parentId.equals(directoryNotFoundId)) {
                child.setParentDirectoryId(directoryNotFoundId);
                fileLogicalService.updateFile(child);
            }
        }
    }

    /**
     * Makes migration process
     *
     * @param directories list of all path to migrate
     */
    private void makeMigration(@NotNull List<Path> directories) {

        for (Path directory : directories) {

            log.info("Working on {}...", directory);
            log.info("Migrating files...");
            traverse(directory);
        }
    }

    /**
     * Traverse a path, rename all physical files and points them to {@code unixRoot}, then
     * moves physical directories to {@code unixRoot}
     *
     * @param directoryPath path
     */
    private void traverse(Path directoryPath) {

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
                    if (isValidUUID(filePhysicalUUID.getName())) {

                        migrateDataByUUID(filePhysicalUUID);

                    } else {

                        migrateDataByName(filePhysicalUUID);
                    }

                } catch (IllegalArgumentException ex) {
                    log.error("Interrupted Exception: {}", ex.getMessage());
                } catch (IOException ex) {
                    log.error("I/O error during API request: {}", ex.getMessage());
                } catch (Exception ex) {
                    log.error("Unexpected exception occurred: {}", ex.getMessage());
                }


            } else { // FOLDERS LOGIC
//                if (isValidUUID(filePhysicalUUID.getName())) {
//                    path = renamePhysicalDirectoryNamedWithUUID(path);
//                }
                traverse(path);
            }

        }
    }

    private void migrateDataByName(FilePhysical filePhysicalUUID) {
        try {
            // find all candidates whose names can match the physical file
            List<FileNodeDto> candidateFiles = findCandidateFilesByFilterName(filePhysicalUUID);
            Boolean isUpdated = false;

            if (candidateFiles != null) {
                // iterate over
                for (FileNodeDto candidate : candidateFiles) {

                    // update logical into /foundDirectory or /notFoundDirectory and move in physical storage
                    isUpdated = updateNodeAndMoveToPhysicalPath(candidate, filePhysicalUUID);

                    if (Boolean.TRUE.equals(isUpdated)) {
                        return;
                    }
                }
            }

            movePhysicalFile(filePhysicalUUID, filePhysicalUUID.getName(), unixDirectoryNotFoundInDatabase);

        } catch (Exception ex) {
            // retrofit exception
            log.error("Retrofit exception when find by name in database: {}", ex.getMessage());
        }
    }

    /**
     * Updates the node and move physical file if names and parent folder match.
     *
     * @param dto file node dto
     * @param filePhysical physical file object
     * @throws IOException if I/O exception occurred
     */
    @Nullable
    private Boolean updateNodeAndMoveToPhysicalPath(@NotNull FileNodeDto dto, FilePhysical filePhysical)
            throws IOException {

        String physicalName = createPhysicalNameWithExtension(filePhysical);
        String nodeName = dto.getName();
        // Use cases:
        //      nodeName without extension (name)
        //      nodeName with extension (name.pdf)
        //      fullNodeName name with extension (name.pdf)
        //      fullNodeName name with extension, with extension (name.pdf.pdf)
        String fullNodeName = nodeName.concat(
                        typeMappingService.getFileExtension(dto.getMimeType()))
                .toLowerCase();

        boolean alreadyProcessed = dto.getParentDirectoryId().equals(unixRoot.getNode().getId())
                || dto.getParentDirectoryId().equals(directoryNotFoundLogical.getId());

        if (alreadyProcessed) {
            log.info("Node already processed {}", filePhysical.getAbsolutePath());
            return null;
        }

        boolean nameExist =  (physicalName.equals(nodeName) || physicalName.equals(fullNodeName));
        boolean isPathBaseEquals = filePhysical.getParentPath().replace(File.separator, "")
                .equals(dto.getPathBase().replace(File.separator, ""));

        // rename it if exists in physical
        //FilePhysical renamePhysical = getPathNameIfDuplicatedFile(filePhysical);

        DirectoryPhysical directory;
        // node found in database
        if (nameExist && isPathBaseEquals) {

            try { // error debug
                FilePhysical moved = movePhysicalFile(filePhysical, filePhysical.getName(), unixRoot.getDirectory());

                // update directory
                // Update dto with invalid UUID into database and set parent root
                dto.setName(moved.getFileName()); // lowercased
                dto.setParentDirectoryId(unixRoot.getNode().getId());
                FileNodeDto updated = fileLogicalService.updateFile(dto);

                if (updated == null) {
                    log.error("Unable to update candidate node, but already move to physical folder: {}", dto.getId());
                }

                return true;

            } catch (IOException ex) {
                log.error("Error #updateNodeAndMoveToPhysicalPath {}: {}", filePhysical.getAbsolutePath(), ex.getMessage());
            }
        }

        return false;
    }

    /**
     * Add extension to filename
     *
     * @param filePhysical physical file object
     * @return filename with extension
     * @throws IOException if I/O exception occurred
     */
    private String createPhysicalNameWithExtension(@NotNull FilePhysical filePhysical) throws IOException {
        String physicalName = filePhysical.getName();
        String mimeType = Files.probeContentType(filePhysical.getAbsolutePath());

        // Try set extension
        if (!filePhysical.isFullNameWithExtension()) {
            try {
                physicalName = filePhysical.getName()
                        .toLowerCase()
                        .concat(".")
                        .concat(typeMappingService.getFileExtension(mimeType));
            } catch (NullPointerException | ClassCastException ex) {
                //
            }
        }
        return physicalName;
    }

    /**
     * Find all logical files whose names includes the name of the physical file without extension
     *
     * @param filePhysical physical file object
     * @return the list of logical files that match the filter requirement
     * @throws IOException if I/O exception occurred
     */
    private List<FileNodeDto> findCandidateFilesByFilterName(@NotNull FilePhysical filePhysical) throws IOException {
        // Setting content for search files by name (include)
        ContentFileNodeDto content = new ContentFileNodeDto();
        content.setName(filePhysical.getName());

        // Setting filter
        FileFilterDto filter = new FileFilterDto();
        filter.setSize(1000);
        filter.setContent(content);

        // Find candidates with filename(invalid UUID)
        return fileLogicalService.findFilesByFilter(filter);
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
     * Move file to {@code unixRoot} directory
     *
     * @param file file
     * @throws IOException if IOException occurred
     */
    private FilePhysical movePhysicalFile(@NotNull FilePhysical file, @NotNull String newName, @NotNull DirectoryPhysical parent) throws IOException {

        Path originPath = file.getAbsolutePath();
        Path destinyPath = Paths.get(parent.getFullPath(), newName.toLowerCase());

        if (Files.exists(destinyPath)) {
            file.setName(newName);
            file.setParentDirectory(parent);
            FilePhysical renamed = getPathNameIfDuplicatedFile(file);

            Files.move(originPath, renamed.getAbsolutePath());

            return renamed;
        }

        // Renames physical not uuid filename with updated data logical
        Files.move(originPath, destinyPath);

        file.setName(newName.toLowerCase());
        file.setParentDirectory(parent);
        return file;
    }


    // Changed
    /**
     * Rename physical files with uuid names based on database information.
     * And move to {@code unixRote}
     *
     * @param filePhysicalUUID physical file with uuid as filename
     * @throws IOException I/O exception during service call
     */
    private void migrateDataByUUID(@NotNull FilePhysical filePhysicalUUID) throws IOException, IllegalArgumentException {
        // find logical file by uuid
        try {
            //TODO OJO CON LOS LOWER CASE!!!!!!
            FileNodeDto dto = fileLogicalService.findFileById(filePhysicalUUID.getName());

            // If UUID doesn't exist move but not else
            if (dto == null) {
                //movePhysicalFile(filePhysicalUUID, filePhysicalUUID.getName().toLowerCase(), unixRoot.getDirectory());
                migrateDataByName(filePhysicalUUID);

            } else {
                // complete filename with extension from database information, and with unix root parent
                FilePhysical filePhysical = getFilenameWithExtension(dto);

                // Full path to physical file renamed
                // if already exist(with prefix (001-, 002, ...) and extension)
                filePhysical = getPathNameIfDuplicatedFile(filePhysical); // extension added

                // update file(logical) with actual physicalName
                // and unix root parent directory
                dto.setName(filePhysical.getFileName());
                dto.setParentDirectoryId(unixRoot.getNode().getId());
                FileNodeDto updated = fileLogicalService.updateFile(dto);

                // delete old logical parent
                if (updated != null) {
                    Path originPath = filePhysicalUUID.getAbsolutePathWithoutExtension();
                    Path destinyPath = Paths.get(unixRoot.getDirectory().getFullPath(), updated.getName());

                    // Renames physical uuid filename with updated data logical
                    Files.move(originPath, destinyPath);
                } else {
                    log.error("Unable to rename(check rename) '{}' due unable update database record.", filePhysical.getFileName());
                }
            }
        } catch (Exception ex) {
            log.error("#migrate: {}", ex.getMessage());
        }
    }

    /**
     * Creates logical and physical directory
     *
     * @param foundDirectoryName directory name where the files found in the database will be saved
     * @param notFoundDirectoryName directory name where the files not found in the database will be saved
     * @param pathBase directory path base
     *
     * @throws IOException if IOException occurred
     * @throws NoRequirementsMeted If the physical or logical path cannot be created
     */
    private void setupNodes(String pathBase, String foundDirectoryName, String notFoundDirectoryName)
            throws IOException, NoRequirementsMeted {

        unixRoot = makeDirectoryScaffold(foundDirectoryName, pathBase);
        //directoryNotFoundLogical = makeDirectoryScaffold(notFoundDirectoryName, Paths.get(pathBase, foundDirectoryName).toString());
        directoryNotFoundLogical = createLogicalNode(notFoundDirectoryName, Paths.get(pathBase, foundDirectoryName).toString());
        unixDirectoryNotFoundInDatabase = createDirectoryPhysical(Paths.get(pathBase, foundDirectoryName, notFoundDirectoryName, "database"));

    }

    /**
     * Creates physical and logical directory
     *
     * @param name directory name
     * @param basePath parent directory path
     * @return logical and physical object
     * @throws IOException if I/O exception occurred
     */
    @NotNull
    private PhysicalLogicalDirectoryDto makeDirectoryScaffold(String name, String basePath) throws IOException {

        Path directoryPath = Paths.get(basePath, name);

        DirectoryNodeDto directoryNode = createLogicalNode(name, basePath);
        DirectoryPhysical directoryPhysical = createDirectoryPhysical(directoryPath);

        return new PhysicalLogicalDirectoryDto(directoryPhysical, directoryNode);
    }

    /**
     * Creates physical directory
     *
     * @param directoryPath absolute directory path
     * @return physical directory created
     * @throws NoRequirementsMeted if cant create a directory
     */
    @NotNull
    @Contract("_ -> new")
    private DirectoryPhysical createDirectoryPhysical(@NotNull Path directoryPath) throws NoRequirementsMeted, IOException {
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
     * Creates if not exists logical directory
     *
     * @param name directory name
     * @param basePath directory path base
     * @return logical directory node
     * @throws IOException if IOException occurred
     * @throws NoRequirementsMeted if cant create a directory node
     */
    private DirectoryNodeDto createLogicalNode(String name, String basePath) throws IOException, NoRequirementsMeted {
        DirectoryNodeDto directoryNode = directoryLogicalService.createDirectory(name, basePath);

        // Could be already created
        if (directoryNode == null) {
            directoryNode = directoryLogicalService.findDirectoryByBasePath(name, basePath);
        }

        // could be integrity problems
        if (directoryNode == null) {
            throw new NoRequirementsMeted("Unable to create necessary node");
        }

        return directoryNode;
    }


    /**
     * Check for duplicated files in physical storage and return a file with unique filename
     *
     * @param filePhysical physical file
     * @return new file with unique name in storage path
     */
    public FilePhysical getPathNameIfDuplicatedFile(@NotNull FilePhysical filePhysical) {
        String name = filePhysical.getName().toLowerCase();
        File finalFileName = new File(filePhysical.getAbsolutePath().toString().toLowerCase());
        int prefix = 1;


        while (finalFileName.exists()) {
            name = String.format("%03d-%s", prefix, filePhysical.getName().toLowerCase());
            finalFileName = new File(
                    Paths.get(
                                    filePhysical
                                            .getParentDirectory()
                                            .getPath()
                                            .toString(),
                                    name.concat(
                                            filePhysical.getExtension() == null ? "" : filePhysical.getExtension().toLowerCase()))
                            .toString());
            prefix++;
        }

        return FilePhysical.builder()
                .name(name)
                .extension(filePhysical.getExtension() == null ? "" : filePhysical.getExtension())
                .parentDirectory(filePhysical.getParentDirectory()) // Esto ya viene en minusculas del main
                .build();
    }


    // Changed
    /**
     * Create physical directory with correct extensión from database record
     *
     * @param fileDto file dto
     * @return physical file representation
     */
    private FilePhysical getFilenameWithExtension(@NotNull FileNodeDto fileDto) {

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
                unixRoot.getDirectory());
    }

    /**
     * Validate UUID
     *
     * @param name name
     * @return {@code true} if element name is a valid UUID, {@code false} otherwise
     */
    @NotNull
    private static Boolean isValidUUID(String name) {
        try {
            // for validation only
            UUID.fromString(name);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return true;
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
}
