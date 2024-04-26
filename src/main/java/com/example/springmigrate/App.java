package com.example.springmigrate;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;

@SpringBootApplication
public class App implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}

	@Override
	public void run(String... args) throws Exception {

		if (userWarningElection()) {
			// TODO: IMPLEMENTATION
		}
	}

	private boolean userWarningElection() {

		System.out.print("***********************************************************************************" +
				"\n                                WARNING!!!" +
				"\n***********************************************************************************" +
				"\n***********************************************************************************" +
				"\n    La ejecución de la aplicación puede producir daños en la base de datos." +
				"\n   Antes de lanzar el script recuerde hacer una copia de seguridad de la misma." +
				"\n***********************************************************************************" +
				"\n\nAún así desea ejecutar la aplicación S/n: ");

		Scanner sc =new Scanner(System.in);
		String input = sc.nextLine();

		return input.equalsIgnoreCase("s");
	}
}
