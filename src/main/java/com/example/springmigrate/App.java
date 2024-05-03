package com.example.springmigrate;

import com.example.springmigrate.service.implementation.DirectoryLogicalServiceImpl;
import com.example.springmigrate.service.implementation.MigratePhysicalDataService;
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

	private static DirectoryLogicalServiceImpl directoryLogicalService;
	private static MigratePhysicalDataService migrate;

	public App(DirectoryLogicalServiceImpl directoryLogicalService, MigratePhysicalDataService migrate) {
		App.directoryLogicalService = directoryLogicalService;
		App.migrate = migrate;
	}

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		if (args.length > 0) {

			Path rootDirectory = Paths.get(args[0]);

			userWarningMessage();

			try {
				migrate.migrate(rootDirectory);
			} catch (ConnectException ex) {
				log.error("Unable to connect with API.");
			} catch (Exception ex) {
				log.error("Unexpected error occurred during the migrate process: {}.", ex.getMessage());
			}


		} else {
			System.out.println("************************************************\n\n" +
					"Usage: java -jar migrate.jar <directories_path>\n\n" +
					"************************************************\n");
		}
	}

	private static void userWarningMessage() {

		System.out.print("***********************************************************************************" +
				"\n                                WARNING!!!" +
				"\n***********************************************************************************" +
				"\n***********************************************************************************" +
				"\n    La ejecución de la aplicación puede producir daños en la base de datos." +
				"\n   Antes de lanzar el script recuerde hacer una copia de seguridad de la base de   " +
				"\n   datos y del sistema de archivos con el que se va a trabajar.                    " +
				"\n***********************************************************************************" +
				"\n\nAún así desea ejecutar la aplicación S/n: ");

		Scanner sc =new Scanner(System.in);
		String input = sc.nextLine();

		if (!input.equalsIgnoreCase("s") ) {
			System.exit(0);
		}
	}
}
