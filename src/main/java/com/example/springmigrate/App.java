package com.example.springmigrate;

import com.example.springmigrate.config.utils.ApiUrl;
import com.example.springmigrate.service.implementation.MigratePhysicalDataService;
import com.example.springmigrate.service.implementation.MigrateUnixService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

//@SpringBootApplication
//@Log4j2
//public class App implements CommandLineRunner {
//
//	private static MigratePhysicalDataService normalMigrate;
//	private static MigrateUnixService customMigrate;
////	@CommandLine.Option(names = {"-u", "--url"}, defaultValue = "http://localhost:9004/", description = "API url")
////	private static String url = "http://localhost:9004/";
////	@CommandLine.Option(names = {"-d", "--directory"}, description = "Directory path to migrate")
////	private String directory;
////	@CommandLine.Option(names = {"-D", "--directories"}, split = ",", description = "Directories paths to migrate")
////	private String[] directories;
//
//
//	public App(MigratePhysicalDataService normalMigrate, MigrateUnixService customMigrate) {
//		App.normalMigrate = normalMigrate;
//		App.customMigrate = customMigrate;
//	}
//
//	public static void main(String[] args) {
//		ApplicationContext context = SpringApplication.run(App.class, args);
//	}
//
//	@Override
//	public void run(String[] args) throws Exception {
//
////		if (directory != null && directories != null) {
////			System.out.println("Use application [-h, --help].");
////			System.exit(0);
////		}
//
//		String url = "http://localhost:9004";
////		String directory = "C:\\Soincon\\EMI\\document-manager";
//		String directory = null;
//		String[] directories = new String[]{"/root/GestorDocumental", "/opt/tools/tomcat/latest"};
//
//		// TODO: Uncoment
//		//userWarningMessage();
//
//		if (!url.endsWith("/")) {
//			url = url.concat("/");
//		}
//
//		ApiUrl apiUrl = new ApiUrl(url);
//
//		if (directory != null) {
//			Path rootDirectory = Paths.get(directory);
//
//			try {
//				App.normalMigrate.migrate(rootDirectory);
//
//			} catch (NoRequirementsMeted ex) {
//				log.error(ex.getMessage());
//			} catch (RuntimeException ex) {
//				log.error("Unexpected error occurred during the migrate process.");
//			}
//		} else {
//
//			List<Path> paths = Arrays.stream(directories).map(Paths::get).collect(Collectors.toList());
//
//			try {
//				// TODO cuando se haga dinamico poner a lower case
//				App.customMigrate.migrate("old", "/", paths);
//
//			} catch (NoRequirementsMeted ex) {
//				log.error(ex.getMessage());
//			} catch (RuntimeException | IOException ex) {
//				log.error("Unexpected error occurred during the migrate process: {}.", ex.getMessage());
//			}
//		}
//	}
//
//	private void userWarningMessage() {
//
//		System.out.print("***********************************************************************************" +
//				"\n                                WARNING!!!" +
//				"\n***********************************************************************************" +
//				"\n***********************************************************************************" +
//				"\n   La ejecución de la aplicación puede producir daños en la base de datos." +
//				"\n   Antes de lanzar el script recuerde hacer una copia de seguridad de la base de   " +
//				"\n   datos y del sistema de archivos con el que se va a trabajar." +
//				"\n   Para ejecutar la aplicación es necesario permisos de administrador.             " +
//				"\n***********************************************************************************" +
//				"\n\nAún así desea ejecutar la aplicación S/n: ");
//
//		Scanner sc =new Scanner(System.in);
//		String input = sc.nextLine();
//
//		if (!input.equalsIgnoreCase("s") ) {
//			System.exit(0);
//		}
//	}
//}


@Log4j2
@SpringBootApplication
public class App implements CommandLineRunner {

	MigratePhysicalDataService normalMigrate;
	MigrateUnixService customMigrate;

	public App(MigratePhysicalDataService normalMigrate, MigrateUnixService customMigrate) {
		this.normalMigrate = normalMigrate;
		this.customMigrate = customMigrate;
	}

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}


	@Override
	public void run(String... args) {
		CommandLine.run(new Command(normalMigrate, customMigrate), args);
	}
}

@Component
@CommandLine.Command(name = "Migrate", mixinStandardHelpOptions = true, version = "1.0",
		description = "Migrate CLI")
class Command implements Runnable {

	private final MigratePhysicalDataService normalMigrate;
	private final MigrateUnixService customMigrate;

	@Option(names = {"-u", "--url"}, description = "URL de la API", defaultValue = "http://localhost:9004/")
	private String url;

	@Option(names = {"-D", "--directorios"}, split = ",", description = "Lista de directorios a migrar.", required = true)
	private String[] directories;

	@Option(names = {"-c", "--custom"}, description = "Tipo de migración a ejecutar [normal, custom]")
	private boolean custom;

	@Option(names = {"-f", "--founded-directory"}, description = "Directory to store files found in database", defaultValue = "old")
	private String foundDirectoryName;

	@Option(names = {"-N", "--not-found-directory"}, description = "Directory to store files not found in database", defaultValue = "notfound")
	private String notFoundDirectoryName;


	public Command(MigratePhysicalDataService normalMigrate, MigrateUnixService customMigrate) {
		this.normalMigrate = normalMigrate;
		this.customMigrate = customMigrate;
	}

	@Override
	public void run() {

		userWarningMessage();

		String root = getRoot();
		// Bean creation to inject in Config#Retrofit
		new ApiUrl(url);
		List<Path> paths = Arrays.stream(directories).map(Paths::get).collect(Collectors.toList());
		try {
			if (custom) {
				// Unix like
				customMigrate.migrate(root, foundDirectoryName, notFoundDirectoryName, paths);

			} else {

				// Flynka like
				normalMigrate.migrate(paths);
			}

		} catch (IOException e) {
			System.out.println("Error");
		}
	}

	private static @NotNull String getRoot() {
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
				"\n   Antes de lanzar el script recuerde hacer una copia de seguridad de la base de   " +
				"\n   datos y del sistema de archivos con el que se va a trabajar." +
				"\n   Para ejecutar la aplicación son necesario permisos de administrador.             " +
				"\n***********************************************************************************" +
				"\n\nAún así desea ejecutar la aplicación S/n: ");

		Scanner sc =new Scanner(System.in);
		String input = sc.nextLine();

		if (!input.equalsIgnoreCase("s") ) {
			System.exit(0);
		}
	}
}