package com.accela;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.accela.service.AzureB2CmigrationService;

@SpringBootApplication
public class AzureB2CMigrationApplication implements CommandLineRunner {

	@Autowired
	private AzureB2CmigrationService azureB2CmigrationService;

	private static final Logger LOGGER = LoggerFactory
		.getLogger(AzureB2CMigrationApplication.class);

	public static void main(String[] args) {
		LOGGER.info("\n\nUSER MIGRATION INITIATED...");
		SpringApplication.run(AzureB2CMigrationApplication.class, args);
		LOGGER.info("\n\nUSER MIGRATION FINISHED.");
	}

	@Override
	public void run(String... args) {
		if(args.length != 2) {
			LOGGER.error("Please supply the following command line argruments; "
					+ "\n[1]the path to your user migration file, "
					+ "\n[2]the client secrete for your tenat application");
			return;
		}
		azureB2CmigrationService.migrateUserFromFile(args[0], args[1]);
	}
}
