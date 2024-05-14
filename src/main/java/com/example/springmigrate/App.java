package com.example.springmigrate;

import com.example.springmigrate.config.utils.Command;
import com.example.springmigrate.service.implementation.MigratePhysicalDataService;
import com.example.springmigrate.service.implementation.MigrateUnixService;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import picocli.CommandLine;

@Log4j2
@SpringBootApplication
public class App implements CommandLineRunner {

	Command command;
	MigratePhysicalDataService normalMigrate;
	MigrateUnixService customMigrate;

	public App(MigratePhysicalDataService normalMigrate, MigrateUnixService customMigrate) {
		this.normalMigrate = normalMigrate;
		this.customMigrate = customMigrate;
		this.command = new Command(normalMigrate, customMigrate);
	}

	public static void main(String[] args) {
		SpringApplication.run(App.class, args);
	}


	@Override
	public void run(String... args) {
		CommandLine.run(command, args);
	}
}
