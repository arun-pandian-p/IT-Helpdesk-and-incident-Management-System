package com.servicedesk.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
@Order(1)
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseInitializer.class);
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private boolean initialized = false;

    public DatabaseInitializer(JdbcTemplate jdbcTemplate, DataSource dataSource) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataSource = dataSource;
    }

    @PostConstruct
    public synchronized void initializeDatabase() {
        if (initialized) {
            return;
        }
        log.info("Initializing SQLite database connection and pragmas...");

        try {
            // Enforce SQLite Pragmas
            jdbcTemplate.execute("PRAGMA foreign_keys = ON;");
            jdbcTemplate.execute("PRAGMA journal_mode = WAL;");
            jdbcTemplate.execute("PRAGMA busy_timeout = 5000;");

            // Execute schema.sql using Spring ResourceDatabasePopulator
            ClassPathResource resource = new ClassPathResource("schema.sql");
            if (resource.exists()) {
                ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                populator.addScript(resource);
                populator.setContinueOnError(false);
                populator.execute(dataSource);
                log.info("SQLite schema initialized successfully.");
            }
            initialized = true;
        } catch (Exception e) {
            log.error("Failed to initialize SQLite schema: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize SQLite database", e);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        initializeDatabase();
    }
}
