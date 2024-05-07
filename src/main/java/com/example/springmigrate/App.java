package com.example.springmigrate;

import com.example.springmigrate.config.utils.ApiUrl;
import com.example.springmigrate.config.utils.error.NoRequirementsMeted;
import com.example.springmigrate.service.implementation.MigratePhysicalDataService;
import com.example.springmigrate.service.implementation.MigrateUnixService;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import picocli.CommandLine;

import javax.naming.Context;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

@SpringBootApplication
@Log4j2
public class App implements CommandLineRunner {

	private static MigratePhysicalDataService normalMigrate;
	private static MigrateUnixService customMigrate;
//	@CommandLine.Option(names = {"-u", "--url"}, defaultValue = "http://localhost:9004/", description = "API url")
//	private static String url = "http://localhost:9004/";
//	@CommandLine.Option(names = {"-d", "--directory"}, description = "Directory path to migrate")
//	private String directory;
//	@CommandLine.Option(names = {"-D", "--directories"}, split = ",", description = "Directories paths to migrate")
//	private String[] directories;


	public App(MigratePhysicalDataService normalMigrate, MigrateUnixService customMigrate) {
		App.normalMigrate = normalMigrate;
		App.customMigrate = customMigrate;
	}

	public static void main(String[] args) {
		ApplicationContext context = SpringApplication.run(App.class, args);
	}

	@Override
	public void run(String[] args) throws Exception {

//		if (directory != null && directories != null) {
//			System.out.println("Use application [-h, --help].");
//			System.exit(0);
//		}

		String url = "http://localhost:9004";
		String directory = "C:\\Soincon\\EMI\\document-manager";
		String[] directories = new String[]{"/opt/tools/tomcat/latest", "/GestorDocumental"};

		userWarningMessage();

		if (!url.endsWith("/")) {
			url = url.concat("/");
		}

		ApiUrl apiUrl = new ApiUrl(url);

		if (directory != null) {
			Path rootDirectory = Paths.get(directory);

			try {
				App.normalMigrate.migrate(rootDirectory);

			} catch (NoRequirementsMeted ex) {
				log.error(ex.getMessage());
			} catch (RuntimeException ex) {
				log.error("Unexpected error occurred during the migrate process.");
			}
		} else {

			List<Path> paths = Arrays.stream(directories).map(Paths::get).collect(Collectors.toList());

			try {
				App.customMigrate.migrate("old", "/", paths);

			} catch (NoRequirementsMeted ex) {
				log.error(ex.getMessage());
			} catch (RuntimeException | IOException ex) {
				log.error("Unexpected error occurred during the migrate process.");
			}
		}
	}

	private void userWarningMessage() {

		System.out.print("***********************************************************************************" +
				"\n                                WARNING!!!" +
				"\n***********************************************************************************" +
				"\n***********************************************************************************" +
				"\n   La ejecución de la aplicación puede producir daños en la base de datos." +
				"\n   Antes de lanzar el script recuerde hacer una copia de seguridad de la base de   " +
				"\n   datos y del sistema de archivos con el que se va a trabajar." +
				"\n   Para ejecutar la aplicación es necesario permisos de administrador.             " +
				"\n***********************************************************************************" +
				"\n\nAún así desea ejecutar la aplicación S/n: ");

		Scanner sc =new Scanner(System.in);
		String input = sc.nextLine();

		if (!input.equalsIgnoreCase("s") ) {
			System.exit(0);
		}
	}
}
