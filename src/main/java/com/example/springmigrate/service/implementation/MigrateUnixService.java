package com.example.springmigrate.service.implementation;

import com.example.springmigrate.config.utils.MigrationUtils;
import com.example.springmigrate.config.utils.error.NoRequirementsMeted;
import com.example.springmigrate.dto.DirectoryNodeDto;
import com.example.springmigrate.dto.FileNodeDto;
import com.example.springmigrate.dto.PhysicalLogicalDirectoryDto;
import com.example.springmigrate.dto.RootNodeDto;
import com.example.springmigrate.model.DirectoryPhysical;
import com.example.springmigrate.model.FilePhysical;
import com.example.springmigrate.service.IDirectoryLogicalService;
import com.example.springmigrate.service.IFileLogicalService;
import com.example.springmigrate.service.IFileTypeLogicalService;
import com.example.springmigrate.service.IRootDirectoryService;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

@Service
@Log4j2
public class MigrateUnixService {

    private final IDirectoryLogicalService directoryLogicalService;
    private final IFileLogicalService fileLogicalService;
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
        this.rootDirectoryService = rootDirectoryService;
        this.mimeTypes = fileTypeLogicalService.findAllFileTypes();
    }

    /**
     * Updates database from physical data storage
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

        // TODO: clean physical directories

        // Show statistics
        MigrationUtils.showResume(physicalLogicalRoot.getDirectory(), directoryNotFoundInDatabase, directories);
    }

    /**
     * Deletes all directories except nodes created for found and not found data
     */
    private void deleteDirectories() {

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
     * Deletes all root directories except from root drive
     *
     * @throws IOException if IOException occurred
     */
    private void deleteRoots() throws IOException {

        // Get all directories before unlink them from root
        unlinkedDirectories = directoryLogicalService.findALl();
        // Get all files before unlink them from root
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
     * Move to not found node, files that was not find in physical storage
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

            resolveDirectoryNames(directory);

            log.info("Working on {}, migrating files...", directory);
            migrate(directory);
        }
    }


    /**
     * Rename all directories with uuid names
     *
     * @param srcPath source directory path
     */
    private void resolveDirectoryNames(@NotNull Path srcPath) {


        // only folders(nodes)
        if (Files.isRegularFile(srcPath)) {
            return;
        }

        String uuid = srcPath.getFileName().toString();
        // we need to change to database stored name if exists
        String retrievedName = uuid;

        // find its name in database
        try {
            DirectoryNodeDto directoryNode = directoryLogicalService.findDirectoryById(uuid);
            // founded in database
            if (directoryNode != null) {
                retrievedName = directoryNode.getName();
            }

        } catch (IOException ex) {
            // retrievedName = uuid;
        }

        // check for directory children content
        for (Path child : MigrationUtils.getPathList(srcPath)) {
            // new call for child directory
            if (Files.isDirectory(child)) {
                resolveDirectoryNames(child);
            }
        }

        // removes all '/' characters and create a valid simple name for complex name directory node
        Path dstPath = srcPath.resolveSibling(retrievedName.replace("/", ""));

        try {

            MigrationUtils.copyDirectoryContentTo(srcPath, dstPath);
        } catch (Exception ex) {
            //
        }
    }

    /**
     * Recursive logic for traverse a path, rename all physical files, and points them
     * to {@code unixRoot}, then moves physical directories to {@code unixRoot}
     *
     * @param directoryPath path
     * @see FilePhysical
     */
    private void migrate(Path directoryPath) {

        for (Path path : MigrationUtils.getPathList(directoryPath)) {

            FilePhysical filePhysicalUUID = FilePhysical
                    .builder()
                    .name(path.getFileName().toString())
                    .parentDirectory(new DirectoryPhysical(path.getParent())).build();

            // Always work on absolute path
            if (Files.isRegularFile(path)) {

                try {
                    if (MigrationUtils.isValidUUID(filePhysicalUUID.getName())) {
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
            List<FileNodeDto> candidateFiles = fileLogicalService.findCandidateFilesByName(filePhysical.getName());

            if (candidateFiles != null) {
                // iterate over all candidates
                for (FileNodeDto candidate : candidateFiles) {

                    // update logical and physical info if filenames match
                    boolean isProcessed = candidate.getParentDirectoryId().equals(physicalLogicalRoot.getNode().getId())
                            || candidate.getParentDirectoryId().equals(nodeNotFound.getId());

                    if (!isProcessed) {
                        Boolean updated = updateNodeAndMoveToPhysicalPath(candidate, filePhysical);
                        if (updated) return;
                    }
                }
            }

            // not found any match in the database, then store it in not found directory
            MigrationUtils.movePhysicalFile(
                    filePhysical,
                    filePhysical.getName(),
                    directoryNotFoundInDatabase);

        } catch (Exception ex) {
            log.error("Error when trying to migrate data by name: {}", ex.getMessage());
        }
    }

    /**
     * Updates the node and move physical file if names and parent folder match.
     *
     * @param dto          file node dto
     * @param filePhysical physical file object
     * @return {@code true} if match, {@code false} if not match,
     * {@code null} if node math but has been already updated
     * @throws IOException if I/O exception occurred
     */
    private Boolean updateNodeAndMoveToPhysicalPath(@NotNull FileNodeDto dto, FilePhysical filePhysical) throws IOException {

        Boolean isUpdated = Boolean.FALSE;
        boolean isFound = MigrationUtils.isLogicalRepresentationOfDirectory(
                dto,
                filePhysical,
                mimeTypes
        );

        // node found in database
        if (isFound) {
            // move file to found directory
            FilePhysical moved = MigrationUtils.movePhysicalFile(
                    filePhysical,
                    filePhysical.getName(),
                    physicalLogicalRoot.getDirectory());

            // set file node with renamed filename and found directory node
            dto.setName(moved.getFileName()); // lowercased
            dto.setParentDirectoryId(physicalLogicalRoot.getNode().getId());
            // update file node
            FileNodeDto updated = fileLogicalService.updateFile(dto);

            if (updated == null) {
                log.error("IMPORTANT!!! Unable to update candidate node, but already move to physical folder: {}", dto.getId());
            }

            isUpdated = Boolean.TRUE;
        }

        return isUpdated;
    }

    /**
     * Rename physical files with uuid names based on database information.
     * And move to {@code unixRote}
     *
     * @param filePhysicalUUID physical file with uuid as filename
     * @throws IOException I/O exception during service call
     */
    private void migrateDataByUUID(@NotNull FilePhysical filePhysicalUUID) throws IOException, IllegalArgumentException {

        try {
            // find logical file node by uuid
            FileNodeDto dto = fileLogicalService.findFileById(filePhysicalUUID.getName());

            // If UUID doesn't exist try to migrate data by filename
            if (dto == null) {

                migrateDataByName(filePhysicalUUID);

            } else {
                // complete filename with extension from database information,
                // and with physical found root parent
                FilePhysical filePhysical = MigrationUtils
                        .getFilenameWithExtension(dto, physicalLogicalRoot.getDirectory(), mimeTypes);

                // if already exist, get file renamed with found path
                // with prefix (001-, 002, ...) and extension
                filePhysical = MigrationUtils.getPathNameIfDuplicatedFile(filePhysical);

                // set file node(logical) with actual physicalName
                // and found parent directory
                dto.setName(filePhysical.getFileName());
                dto.setParentDirectoryId(physicalLogicalRoot.getNode().getId());

                // update node
                FileNodeDto updated = fileLogicalService.updateFile(dto);

                // move to found physical directory
                if (updated != null) {
                    Path originPath = filePhysicalUUID.getAbsolutePathWithoutExtension();
                    Path destinyPath = Paths.get(physicalLogicalRoot.getDirectory().getFullPath(), updated.getName());

                    // Renames physical uuid filename with updated node name(logical)
                    Files.move(originPath, destinyPath);

                } else {
                    log.error("Unable to rename '{}' due unable update database record.", filePhysical.getFileName());
                }
            }
        } catch (Exception ex) {
            log.error("Unable to migrate {}: {}", filePhysicalUUID.getAbsolutePath(), ex.getMessage());
        }
    }

    /**
     * Creates logical and physical directory scaffold needed for migrate logic
     *
     * @param foundDirectoryName    directory name where the files found in the database will be saved
     * @param notFoundDirectoryName directory name where the files not found in the database will be saved
     * @param pathBase              directory path base
     * @throws IOException         if IOException occurred
     * @throws NoRequirementsMeted If the physical or logical path cannot be created
     */
    private void makeInitialScaffold(
            String pathBase,
            String foundDirectoryName,
            String notFoundDirectoryName) throws IOException, NoRequirementsMeted {

        physicalLogicalRoot = makeDirectoryScaffold(foundDirectoryName, pathBase);

        nodeNotFound = directoryLogicalService.createLogicalNode(
                notFoundDirectoryName,
                Paths.get(pathBase, foundDirectoryName).toString());

        directoryNotFoundInDatabase =
                MigrationUtils.createDirectoryPhysical(
                        Paths.get(
                                pathBase,
                                foundDirectoryName,
                                notFoundDirectoryName,
                                "database"));

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

        DirectoryNodeDto directoryNode = directoryLogicalService.createLogicalNode(name, basePath);
        DirectoryPhysical directoryPhysical = MigrationUtils.createDirectoryPhysical(directoryPath);

        return new PhysicalLogicalDirectoryDto(directoryPhysical, directoryNode);
    }
}
