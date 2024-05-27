package com.example.springmigrate.config.utils;

import com.example.springmigrate.service.implementation.MigratePhysicalDataService;
import com.example.springmigrate.service.implementation.MigrateUnixService;
import lombok.extern.log4j.Log4j2;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import picocli.CommandLine;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;


@Component
@CommandLine.Command(name = "Migrate", mixinStandardHelpOptions = true, version = "1.0",
        description = "Migrate CLI")
@Log4j2
public class Command implements Runnable {

    private final MigratePhysicalDataService normalMigrate;
    private final MigrateUnixService customMigrate;

    @CommandLine.Option(names = {"-u", "--url"}, description = "URL de la API", defaultValue = "http://localhost:9004/")
    private String url;

    @CommandLine.Option(names = {"-D", "--directorios"}, split = ",", description = "Lista de directorios a migrar.", required = true)
    private String[] directories;

    @CommandLine.Option(names = {"-c", "--custom"}, description = "Tipo de migración a ejecutar [normal, custom]")
    private boolean custom;

    @CommandLine.Option(names = {"-f", "--founded-directory"}, description = "Directory to store files found in database", defaultValue = "old")
    private String foundDirectoryName;

    @CommandLine.Option(names = {"-N", "--not-found-directory"}, description = "Directory to store files not found in database", defaultValue = "notfound")
    private String notFoundDirectoryName;


    public Command(MigratePhysicalDataService normalMigrate, MigrateUnixService customMigrate) {
        this.normalMigrate = normalMigrate;
        this.customMigrate = customMigrate;
    }

    @Override
    public void run() {

        userWarningMessage();

        String root = getRootDrive();
        // Bean creation to inject in Config#Retrofit
        new ApiUrl(url);
        List<Path> paths = Arrays.stream(directories).map(Paths::get).collect(Collectors.toList());

        try {
            if (custom) {
                // Unix like
                customMigrate.migrate(
                        root.toLowerCase(),
                        foundDirectoryName.toLowerCase(),
                        notFoundDirectoryName.toLowerCase(),
                        paths);

            } else {

                // Flynka like
                normalMigrate.migrate(paths);
            }

        } catch (IOException ex) {
            log.error("Unexpected error: {}", ex.getMessage());
        }
    }

    private static @NotNull String getRootDrive() {
        String root = "/";
        try {
            root = Paths.get(System.getProperty("user.dir"))
                    .getFileSystem()
                    .getRootDirectories()
                    .iterator()
                    .next()
                    .toString();
        } catch (Exception ex) {

            String os = System.getProperty("os.name");

            if (os.startsWith("Windows")) {
                root = "c:/";
            }
        }

        return root;
    }

    private void userWarningMessage() {

        System.out.print("***********************************************************************************" +
                "\n                                WARNING!!!" +
                "\n***********************************************************************************" +
                "\n***********************************************************************************" +
                "\n   La ejecución de la aplicación puede producir daños en la base de datos." +
                "\n    * Antes de lanzar el script recuerde" +
                "\n         - IMPORTANTE: Ejecutar con permisos de administrador.             " +
                "\n         - Hacer una copia de seguridad de la base de datos.   " +
                "\n         - Hacer una copia del sistema de archivos que se migrará." +
                "\n***********************************************************************************" +
                "\n\nSeguro que desea ejecutar la aplicación s/n: ");

        Scanner sc = new Scanner(System.in);
        String input = sc.nextLine();

        if (!input.equalsIgnoreCase("s")) {
            System.exit(0);
        }
    }
}
