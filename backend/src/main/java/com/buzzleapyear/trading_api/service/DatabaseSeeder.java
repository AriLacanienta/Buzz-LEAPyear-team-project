package com.buzzleapyear.trading_api.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class DatabaseSeeder implements CommandLineRunner {
    
    private static final Logger logger = LoggerFactory.getLogger(DatabaseSeeder.class);
    private final ImportCSVService importCSVService;

    public DatabaseSeeder(ImportCSVService importCSVService) {
        this.importCSVService = importCSVService;
    }

    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting database seeding with trades.csv...");
        try {
            ClassPathResource resource = new ClassPathResource("trades.csv");
            importCSVService.importFromTradesCSV(resource.getInputStream());
            logger.info("✓ Database seeding completed successfully!");
        } catch (Exception e) {
            logger.error("✗ Database seeding failed", e);
            throw e;
        }
    }
}
