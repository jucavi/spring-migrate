package com.example.springmigrate.service.implementation;

import com.example.springmigrate.config.utils.error.NoRequirementsMeted;
import com.example.springmigrate.dto.*;
import com.example.springmigrate.model.DirectoryPhysical;
import com.example.springmigrate.model.FilePhysical;
import com.example.springmigrate.service.IDirectoryLogicalService;
import com.example.springmigrate.service.IFileLogicalService;
import com.example.springmigrate.service.IRootDirectoryService;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
public class MigrateUnixService {

    private final IDirectoryLogicalService directoryLogicalService;
    private final IFileLogicalService fileLogicalService;
    private final FileTypeMappingService typeMappingService;
    private final IRootDirectoryService rootDirectoryService;
    private PhysicalLogicalDirectoryDto unixRoot;
    private PhysicalLogicalDirectoryDto unixRootNotFound;
    private List<DirectoryNodeDto> unlinkedDirectories;

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
        log.info("Creating root directory...");
        setupNodes(pathBase, foundDirectoryName, notFoundDirectoryName);
        makeMigration(directories);
        deleteRoots();
        log.info("Deleting Directories...");
        deleteDirectories();
    }

    /**
     * Deletes all directories except {@code unixRoot} created
     *
     * @throws IOException if IOException occurred
     */
    private void deleteDirectories() throws IOException {

        String directoryId = unixRoot.getNode().getId();

        for (DirectoryNodeDto directory : unlinkedDirectories) {
            if (!directory.getId().equals(directoryId) || directory.getParentDirectoryId().equals(directoryId)) {

                try {
                    //directoryLogicalService.deleteDirectoryHard(directory.getId());
                    directoryLogicalService.deleteDirectory(directory.getId());
                    log.info("Deleting... {}", directory.getId());
                } catch (Exception ex) {
                    log.error("Error deleting {}, must be unlinked directory", directory.getId());
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

//        unlinkedDirectories = directoryLogicalService.findALl();
//
//        List<RootNodeDto> roots = rootDirectoryService.findAll();
//
//        assert unixRoot != null;
//        String directoryId = unixRoot.getNode().getId();
//
//        for (RootNodeDto root : roots) {
//            String childDirectoryId = root.getDirectory().getId();
//
//            if (!childDirectoryId.equals(directoryId)) {
//                rootDirectoryService.deleteByDirectoryId(childDirectoryId);
//            }
//        }

        log.info("Searching for directories...");
        unlinkedDirectories = directoryLogicalService.findALl();

        log.info("Deleting roots...");
        rootDirectoryService.truncate();
        RootNodeDto root = new RootNodeDto();
        root.setPathBase(unixRoot.getNode().getPathBase());
        root.setDirectory(unixRoot.getNode());

        rootDirectoryService.createRoot(root);
    }

    /**
     * Makes migration process
     *
     * @param directories list of all path to migrate
     */
    private void makeMigration(List<Path> directories) {

        for (Path directory : directories) {

            log.info("Working on {}", directory);
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
//                    log.info("Renaming physical directory...");
//                    path = renamePhysicalDirectoryNamedWithUUID(path);
//                }
                traverse(path);
            }

        }
    }

    private void migrateDataByName(FilePhysical filePhysicalUUID) throws IOException {
        // find all candidates whose names can match the physical file
        List<FileNodeDto> candidateFiles = findCandidateFilesByFilter(filePhysicalUUID);

        if (candidateFiles != null && !candidateFiles.isEmpty()) {
            // iterate over
            for (FileNodeDto candidate : candidateFiles) {
                try {
                    // update logical into /foundDirectory or /notFoundDirectory and move in physical storage
                    updateNodeAndMoveToPhysicalPath(candidate, filePhysicalUUID);
                } catch (IOException ex) {
                    log.error("Error #migrateDataByName {}", filePhysicalUUID.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Updates the node and move physical file if names and parent folder match.
     *
     * @param dto file node dto
     * @param filePhysical physical file object
     * @throws IOException if I/O exception occurred
     */
    private void updateNodeAndMoveToPhysicalPath(FileNodeDto dto, FilePhysical filePhysical)
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

        boolean nameExist =  (physicalName.equals(nodeName) || physicalName.equals(fullNodeName));
        boolean isPathBaseEquals = filePhysical.getParentPath().replace(File.separator, "")
                .equals(dto.getPathBase().replace(File.separator, ""));

        DirectoryPhysical directory;

        FilePhysical renamedPhysical  = getPathNameIfDuplicatedFile(filePhysical);

        // node found in database
        if (nameExist && isPathBaseEquals) {
            dto.setName(renamedPhysical.getFileName());
            // Update dto with invalid UUID into database and set parent root
            dto.setParentDirectoryId(unixRoot.getNode().getId());
            directory = unixRoot.getDirectory();

        } else { // node not found
            // update parent directory to not found
            dto.setParentDirectoryId(unixRootNotFound.getNode().getId());
            directory = unixRootNotFound.getDirectory();
            renamedPhysical.setParentDirectory(unixRootNotFound.getDirectory());
        }

        try {
            FileNodeDto updated = fileLogicalService.updateFile(dto);

            if (updated == null) {
                log.error("Unable to update candidate node: {}", dto.getId());
            }

            movePhysicalFile(filePhysical, renamedPhysical.getFileName(), directory);

        } catch (IOException ex) {
            log.error("Error #updateNodeAndMoveToPhysicalPath {}", filePhysical.getAbsolutePath());
        }

    }

    /**
     * Add extension to filename
     *
     * @param filePhysical physical file object
     * @return filename with extension
     * @throws IOException if I/O exception occurred
     */
    private String createPhysicalNameWithExtension(FilePhysical filePhysical) throws IOException {

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
    private List<FileNodeDto> findCandidateFilesByFilter(@NotNull FilePhysical filePhysical) throws IOException {
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
     * @param sourceDir directory path
     * @return directory name extracted from database
     */
    private Path renamePhysicalDirectoryNamedWithUUID(@NotNull Path sourceDir) {
        Path targetDir;

        try {
            DirectoryNodeDto dto = directoryLogicalService.findDirectoryById(sourceDir.getFileName().toString());

            if (dto != null) {
                targetDir = sourceDir.resolveSibling(dto.getName());

                if (!Files.exists(targetDir)) {
                    // Update physical folder name
                    Files.createDirectories(targetDir);
                    Files.move(sourceDir, targetDir, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    Files.walk(sourceDir)
                            .forEach(source -> {
                                try {
                                    Path target = targetDir.resolve(sourceDir.relativize(source));
                                    Files.move(source, target);
                                } catch (IOException e) {
                                    //log.error("Unable to copy form source to target directory");
                                }
                            });

                    Files.deleteIfExists(sourceDir);
                }
                return targetDir;
            }
        } catch (IOException e) {
            //log.error("Unable to rename {}: {}", sourceDir, e.getMessage());
            // can't rename a directory
        }

        return sourceDir;
    }

    /**
     * Move file to {@code unixRoot} directory
     *
     * @param file file
     * @throws IOException if IOException occurred
     */
    private void movePhysicalFile(FilePhysical file, String newName, DirectoryPhysical parent) throws IOException {

        Path originPath = file.getAbsolutePath();
        Path destinyPath = Paths.get(parent.getFullPath(), newName.toLowerCase());

        if (Files.exists(destinyPath)) {
            file.setName(newName);
            file.setParentDirectory(parent);
            FilePhysical renamed = getPathNameIfDuplicatedFile(file);

            Files.move(originPath, renamed.getAbsolutePath());

        } else {

            // Renames physical not uuid filename with updated data logical
            Files.move(originPath, destinyPath);
        }
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

            // If UUID doesn't exist
            if (dto == null) {
                // try to find by name and migrate
                movePhysicalFile(filePhysicalUUID, filePhysicalUUID.getName().toLowerCase(), unixRootNotFound.getDirectory());
                //migrateDataByName(filePhysicalUUID);

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
        unixRootNotFound = makeDirectoryScaffold(notFoundDirectoryName, Paths.get(pathBase, foundDirectoryName).toString());

    }

    /**
     * Creates physical and logical directory
     *
     * @param name directory name
     * @param basePath parent directory path
     * @return logical and physical object
     * @throws IOException if I/O exception occurred
     */
    private PhysicalLogicalDirectoryDto makeDirectoryScaffold(String name, String basePath) throws IOException {

        Path directoryPath = Paths.get(basePath, name);
        boolean directoryCreated;

        DirectoryPhysical directoryPhysical = new DirectoryPhysical(directoryPath);
        DirectoryNodeDto directoryNode = directoryLogicalService.createDirectory(name, basePath);

        // Could be already created
        if (directoryNode == null) {
            directoryNode = directoryLogicalService.findDirectoryByBasePath(name, basePath);
        }

        File directory = new File(directoryPath.toString());

        // Attempt to create the directory
        if (Files.exists(directory.toPath())) {
            directoryCreated = true;
        } else {
            directoryCreated = directory.mkdir();
        }

        // if directory node is null the folder already exist and could be integrity problems
        if (!directoryCreated || directoryNode == null) {
            throw new NoRequirementsMeted("Unable to create necessary scaffold");
        }

        return new PhysicalLogicalDirectoryDto(directoryPhysical, directoryNode);
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
