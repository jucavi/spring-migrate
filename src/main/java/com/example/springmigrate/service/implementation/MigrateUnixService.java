package com.example.springmigrate.service.implementation;

import com.example.springmigrate.config.utils.MigrationUtils;
import com.example.springmigrate.config.utils.error.NoRequirementsMeted;
import com.example.springmigrate.dto.*;
import com.example.springmigrate.model.DirectoryPhysical;
import com.example.springmigrate.model.FilePhysical;
import com.example.springmigrate.service.IDirectoryLogicalService;
import com.example.springmigrate.service.IFileLogicalService;
import com.example.springmigrate.service.IFileTypeLogicalService;
import com.example.springmigrate.service.IRootDirectoryService;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Log4j2
public class MigrateUnixService {

    private final IDirectoryLogicalService directoryLogicalService;
    private final IFileLogicalService fileLogicalService;
    private final IFileTypeLogicalService fileTypeService;
    private final IRootDirectoryService rootDirectoryService;

    private final Map<String, String> mimeTypes;
    private PhysicalLogicalDirectoryDto physicalLogicalRoot;
    private DirectoryNodeDto nodeNotFound;
    private DirectoryPhysical directoryNotFoundInDatabase;
    private List<DirectoryNodeDto> unlinkedDirectories;
    private List<FileNodeDto> unlinkedFiles;

    /**
     * Constructor
     *
     * @param directoryLogicalService directory service
     * @param fileLogicalService      file service
     * @param fileTypeLogicalService  file type service
     */
    public MigrateUnixService(
            IDirectoryLogicalService directoryLogicalService,
            IFileLogicalService fileLogicalService,
            IFileTypeLogicalService fileTypeLogicalService,
            IRootDirectoryService rootDirectoryService) throws IOException {

        this.directoryLogicalService = directoryLogicalService;
        this.fileLogicalService = fileLogicalService;
        this.fileTypeService = fileTypeLogicalService;
        this.rootDirectoryService = rootDirectoryService;
        this.mimeTypes = this.fileTypeService.findAllFileTypes();
    }

    /**
     * Create and keep database integrity from physical data
     *
     * @throws IOException if an I/O error occurred
     */
    public void migrate(String pathBase, String foundDirectoryName, String notFoundDirectoryName, List<Path> directories) throws IOException, NoRequirementsMeted {

        // Create logical and physical scaffold
        log.info("Creating directories...");
        makeInitialScaffold(pathBase, foundDirectoryName, notFoundDirectoryName);

        // make migration
        runMigrations(directories);

        // clean logical
        log.info("Deleting roots...");
        deleteRoots();

        log.info("Deleting Directories...");
        deleteDirectories();

        // check migration status

        // clean physical

        // Show statistics
        MigrationUtils.showResume(physicalLogicalRoot.getDirectory(), directoryNotFoundInDatabase, directories);
    }

    /**
     * Deletes all directories except {@code unixRoot} created
     *
     * @throws IOException if IOException occurred
     */
    private void deleteDirectories() throws IOException {

        String foundId = physicalLogicalRoot.getNode().getId();
        String notFoundId = nodeNotFound.getId();

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
        moveLinkedFilesToNotFoundNode();

        // delete root data
        rootDirectoryService.truncate();

        // create again root directory
        RootNodeDto root = new RootNodeDto();
        root.setPathBase(physicalLogicalRoot.getNode().getPathBase());
        root.setDirectory(physicalLogicalRoot.getNode());

        rootDirectoryService.createRoot(root);
    }

    /**
     * Move to {@code nodeNotFound} files that can not find in physical storage
     *
     * @throws IOException if IOException occurred
     */
    private void moveLinkedFilesToNotFoundNode() throws IOException {

        String directoryFoundId = physicalLogicalRoot.getNode().getId();
        String directoryNotFoundId = nodeNotFound.getId();

        for (FileNodeDto child : unlinkedFiles) {
            String parentId = child.getParentDirectoryId();

            if (!parentId.equals(directoryFoundId) && !parentId.equals(directoryNotFoundId)) {
                child.setParentDirectoryId(directoryNotFoundId);
                fileLogicalService.updateFile(child);
            }
        }
    }

    /**
     * Execute migration process over all directories passed
     *
     * @param directories list directory paths to migrate
     */
    private void runMigrations(@NotNull List<Path> directories) {

        for (Path directory : directories) {
            log.info("Working on {}, migrating files...", directory);
            migrate(directory);
        }
    }

    /**
     * Recursive logic for traverse a path, rename all physical files and points them to {@code unixRoot}, then
     * moves physical directories to {@code unixRoot}
     *
     * @param directoryPath path
     * @see FilePhysical
     */
    private void migrate(Path directoryPath) {

        for (Path path : getPathList(directoryPath)) {

            FilePhysical filePhysicalUUID = FilePhysical
                    .builder()
                    .name(path.getFileName().toString())
                    .parentDirectory(new DirectoryPhysical(path.getParent())).build();

            // Always work on absolute path
            if (Files.isRegularFile(path)) {

                try {
                    if (isValidUUID(filePhysicalUUID.getName())) {
                        // migration based on uuid filename
                        migrateDataByUUID(filePhysicalUUID);

                    } else {
                        // migration with human-readable filename
                        migrateDataByName(filePhysicalUUID);
                    }

                } catch (IllegalArgumentException ex) {
                    log.error("Interrupted Exception: {}", ex.getMessage());
                } catch (IOException ex) {
                    log.error("I/O error: {}", ex.getMessage());
                } catch (Exception ex) {
                    log.error("Unexpected exception occurred: {}", ex.getMessage());
                }


            } else { // FOLDERS LOGIC
                // recursive call
                migrate(path);
            }
        }
    }

    /**
     * Make migration based on huma-readable filename
     *
     * @param filePhysical file
     * @see FilePhysical
     */
    private void migrateDataByName(FilePhysical filePhysical) {
        try {
            // find all candidates whose names could match with physical filename
            List<FileNodeDto> candidateFiles = findCandidateFilesByFilterName(filePhysical);

            if (candidateFiles != null) {
                // iterate over all candidates
                for (FileNodeDto candidate : candidateFiles) {

                    // update logical and physical info if filenames match
                    if (Boolean.TRUE.equals(
                            updateNodeAndMoveToPhysicalPath(candidate, filePhysical))) {
                        return;
                    }
                }
            }
            // not found any match in the database, then store it in not found directory
            movePhysicalFile(filePhysical, filePhysical.getName(), directoryNotFoundInDatabase);

        } catch (Exception ex) {
            log.error("Error: {}", ex.getMessage());
        }
    }

    /**
     * Updates the node and move physical file if names and parent folder match.
     *
     * @param dto          file node dto
     * @param filePhysical physical file object
     * @throws IOException if I/O exception occurred
     */
    @Nullable
    private Boolean updateNodeAndMoveToPhysicalPath(@NotNull FileNodeDto dto, FilePhysical filePhysical) throws IOException {

        //
        String physicalName = MigrationUtils.setFileNameWithExtension(filePhysical, mimeTypes);
        String nodeName = dto.getName();
        // Use cases:
        //      nodeName without extension (name)
        //      nodeName with extension (name.pdf)
        //      fullNodeName name with extension (name.pdf)
        //      fullNodeName name with extension, with extension (name.pdf.pdf)
        String fullNodeName = nodeName.concat(mimeTypes.get(dto.getMimeType())).toLowerCase();

        boolean alreadyProcessed = dto.getParentDirectoryId().equals(physicalLogicalRoot.getNode().getId()) || dto.getParentDirectoryId().equals(nodeNotFound.getId());

        if (alreadyProcessed) {
            log.info("Node already processed {}", filePhysical.getAbsolutePath());
            return null;
        }

        boolean nameExist = (physicalName.equals(nodeName) || physicalName.equals(fullNodeName));
        boolean isPathBaseEquals = filePhysical.getParentPath().replace(File.separator, "").equals(dto.getPathBase().replace(File.separator, ""));

        // rename it if exists in physical
        //FilePhysical renamePhysical = getPathNameIfDuplicatedFile(filePhysical);

        DirectoryPhysical directory;
        // node found in database
        if (nameExist && isPathBaseEquals) {

            try { // error debug
                FilePhysical moved = movePhysicalFile(filePhysical, filePhysical.getName(), physicalLogicalRoot.getDirectory());

                // update directory
                // Update dto with invalid UUID into database and set parent root
                dto.setName(moved.getFileName()); // lowercased
                dto.setParentDirectoryId(physicalLogicalRoot.getNode().getId());
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
                dto.setParentDirectoryId(physicalLogicalRoot.getNode().getId());
                FileNodeDto updated = fileLogicalService.updateFile(dto);

                // delete old logical parent
                if (updated != null) {
                    Path originPath = filePhysicalUUID.getAbsolutePathWithoutExtension();
                    Path destinyPath = Paths.get(physicalLogicalRoot.getDirectory().getFullPath(), updated.getName());

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
     * @param foundDirectoryName    directory name where the files found in the database will be saved
     * @param notFoundDirectoryName directory name where the files not found in the database will be saved
     * @param pathBase              directory path base
     * @throws IOException         if IOException occurred
     * @throws NoRequirementsMeted If the physical or logical path cannot be created
     */
    private void makeInitialScaffold(String pathBase, String foundDirectoryName, String notFoundDirectoryName) throws IOException, NoRequirementsMeted {

        physicalLogicalRoot = makeDirectoryScaffold(foundDirectoryName, pathBase);
        nodeNotFound = createLogicalNode(notFoundDirectoryName, Paths.get(pathBase, foundDirectoryName).toString());
        directoryNotFoundInDatabase = MigrationUtils.createDirectoryPhysical(Paths.get(pathBase, foundDirectoryName, notFoundDirectoryName, "database"));

    }

    /**
     * Creates physical and logical directories as twins directories
     *
     * @param name     directory name
     * @param basePath parent directory path
     * @return logical and physical object
     * @throws IOException if I/O exception occurred
     * @see PhysicalLogicalDirectoryDto
     */
    @NotNull
    public PhysicalLogicalDirectoryDto makeDirectoryScaffold(String name, String basePath) throws IOException {

        Path directoryPath = Paths.get(basePath, name);

        DirectoryNodeDto directoryNode = createLogicalNode(name, basePath);
        DirectoryPhysical directoryPhysical = MigrationUtils.createDirectoryPhysical(directoryPath);

        return new PhysicalLogicalDirectoryDto(directoryPhysical, directoryNode);
    }

    /**
     * Creates if not exists logical directory
     *
     * @param name     directory name
     * @param basePath directory path base
     * @return logical directory node
     * @throws IOException         if IOException occurred
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
            finalFileName = new File(Paths.get(filePhysical.getParentDirectory().getPath().toString(), name.concat(filePhysical.getExtension() == null ? "" : filePhysical.getExtension().toLowerCase())).toString());
            prefix++;
        }

        return FilePhysical.builder().name(name).extension(filePhysical.getExtension() == null ? "" : filePhysical.getExtension()).parentDirectory(filePhysical.getParentDirectory()) // Esto ya viene en minusculas del main
                .build();
    }

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
        String extension = mimeTypes.get(mimeType);

        if (filename.endsWith(extension)) {
            int index = filename.lastIndexOf(".");
            filename = filename.substring(0, index);
        }

        return new FilePhysical(filename, // without extension
                extension, physicalLogicalRoot.getDirectory());
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

        return Arrays.stream(Objects.requireNonNull(new File(directoryPath.toString()).listFiles())).map(file -> Paths.get(file.getAbsolutePath())).collect(Collectors.toList());
    }
}
