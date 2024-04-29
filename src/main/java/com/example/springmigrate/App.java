package com.example.springmigrate;

import com.example.springmigrate.service.implementation.DirectoryLogicalServiceImpl;
import com.example.springmigrate.service.implementation.Migrate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Paths;
import java.util.Scanner;

@SpringBootApplication
public class App implements CommandLineRunner {

	private static DirectoryLogicalServiceImpl directoryLogicalService;
	private static Migrate migrate;

	public App(DirectoryLogicalServiceImpl directoryLogicalService, Migrate migrate) {
		App.directoryLogicalService = directoryLogicalService;
		App.migrate = migrate;
	}

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		if (args.length == 1) {
//			if (userWarningElection()) {
//
//				// TODO: IMPLEMENTATION
//				migrate.migrate(Paths.get(args[0]));
//			}

			migrate.migrate(Paths.get(args[0]));

		} else {
			System.out.println("************************************************\n\n" +
					"Usage: java -jar migrate.jar <directories_path>\n\n" +
					"************************************************\n");
		}
	}

	private boolean userWarningElection() {

		System.out.print("***********************************************************************************" +
				"\n                                WARNING!!!" +
				"\n***********************************************************************************" +
				"\n***********************************************************************************" +
				"\n    La ejecución de la aplicación puede producir daños en la base de datos," +
				"\n   antes de lanzar el script recuerde hacer una copia de seguridad de la misma." +
				"\n***********************************************************************************" +
				"\n\nAún así desea ejecutar la aplicación S/n: ");

		Scanner sc =new Scanner(System.in);
		String input = sc.nextLine();

		return input.equalsIgnoreCase("s");
	}
}
