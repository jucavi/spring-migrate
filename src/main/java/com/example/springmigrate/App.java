package com.example.springmigrate;

import com.example.springmigrate.config.utils.ApiUrl;
import com.example.springmigrate.config.utils.RetrofitClient;
import com.example.springmigrate.service.implementation.DirectoryLogicalServiceImpl;
import com.example.springmigrate.service.implementation.MigratePhysicalDataService;
import com.example.springmigrate.service.implementation.MigrateUnixService;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.ConnectException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

@SpringBootApplication
@Log4j2
public class App implements CommandLineRunner {

	private static MigratePhysicalDataService normalMigrate;
	private static MigrateUnixService customMigrate;

	public App(MigratePhysicalDataService normalMigrate, MigrateUnixService customMigrate) {
		App.normalMigrate = normalMigrate;
		App.customMigrate = customMigrate;
	}

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		normalClient(args);
		customClient(args);
	}

	private static void customClient(String[] args) {
		if (args.length > 0) {

			Path rootDirectory = Paths.get(args[0]);

			userWarningMessage();

			try {
				normalMigrate.migrate(rootDirectory);
			} catch (ConnectException ex) {
				log.error("Unable to connect with API.");
			} catch (Exception ex) {
				log.error("Unexpected error occurred during the migrate process: {}.", ex.getMessage());
			}


		} else {
			System.out.println("***********************************************************\n\n" +
					"Usage: java -jar migrate.jar <directories_path> [api_url]\n\n" +
					"***********************************************************\n");
		}
	}

	private static void normalClient(String[] args) {
		ApiUrl apiUrl;

		if (args.length > 0) {
			Path rootDirectory = Paths.get(args[0]);

			userWarningMessage();

			apiUrl = new ApiUrl("http://localhost:9004/");

			if (args.length == 2) {
				String baseUrl = args[1];

				if (!baseUrl.endsWith("/")) {
					baseUrl = baseUrl.concat("/");
				}

				apiUrl = new ApiUrl(baseUrl);
			}


			userWarningMessage();

			try {
				normalMigrate.migrate(rootDirectory);

			} catch (ConnectException ex) {
				log.error("Unable to connect with API.");
			} catch (Exception ex) {
				log.error("Unexpected error occurred during the migrate process.");
			}

		} else {
			System.out.println("Usage:" +
					"\n\tjava -jar migrate.jar <c:/ruta/absoluta/carpeta/archivos> [http://base/url/api]" +
					"\n\t  URL API (default: <http://localhost:9004>)");
		}
	}

	private static void userWarningMessage() {

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
