package com.servicedesk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.*;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class DataMigrationService {

    private static final Logger log = LoggerFactory.getLogger(DataMigrationService.class);

    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.datasource.url:jdbc:sqlite:helpdesk.db}")
    private String datasourceUrl;

    public DataMigrationService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Path backupDatabase(String customDestinationDir) throws IOException {
        String dbPathStr = extractDbPath(datasourceUrl);
        Path sourcePath = Path.of(dbPathStr);

        if (!Files.exists(sourcePath)) {
            throw new IOException("Database file does not exist at path: " + sourcePath.toAbsolutePath());
        }

        Path targetDir = customDestinationDir != null ? Path.of(customDestinationDir) : sourcePath.getParent();
        if (targetDir == null) {
            targetDir = Path.of(".");
        }
        Files.createDirectories(targetDir);

        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(":", "-");
        String backupFilename = "helpdesk_backup_" + timestamp + ".db";
        Path backupPath = targetDir.resolve(backupFilename);

        Files.copy(sourcePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Database backed up successfully to: {}", backupPath.toAbsolutePath());
        return backupPath;
    }

    /**
     * Optional utility to migrate data directly from an existing PostgreSQL database to SQLite.
     */
    @Transactional
    public int migrateFromPostgres(String pgJdbcUrl, String pgUser, String pgPassword) throws SQLException {
        log.info("Starting optional PostgreSQL to SQLite data migration from {}", pgJdbcUrl);
        List<String> tables = List.of(
                "users", "ticket_categories", "assets", "tickets", "ticket_comments",
                "ticket_status_history", "ticket_escalations", "ticket_feedback",
                "knowledge_articles", "onboarding_requests", "onboarding_tasks",
                "access_requests", "improvement_requests", "service_request_types",
                "service_requests", "service_request_comments", "service_request_timeline",
                "audit_logs"
        );

        int totalMigrated = 0;
        try (Connection pgConn = DriverManager.getConnection(pgJdbcUrl, pgUser, pgPassword)) {
            for (String table : tables) {
                try (Statement stmt = pgConn.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT * FROM " + table)) {
                    ResultSetMetaData md = rs.getMetaData();
                    int cols = md.getColumnCount();
                    if (cols == 0) continue;

                    StringBuilder insertSql = new StringBuilder("INSERT OR IGNORE INTO ").append(table).append(" (");
                    StringBuilder placeholders = new StringBuilder();
                    for (int i = 1; i <= cols; i++) {
                        insertSql.append(md.getColumnName(i));
                        placeholders.append("?");
                        if (i < cols) {
                            insertSql.append(", ");
                            placeholders.append(", ");
                        }
                    }
                    insertSql.append(") VALUES (").append(placeholders).append(")");

                    while (rs.next()) {
                        Object[] params = new Object[cols];
                        for (int i = 1; i <= cols; i++) {
                            Object val = rs.getObject(i);
                            if (val instanceof java.sql.Timestamp ts) {
                                params[i - 1] = ts.toInstant().toString();
                            } else if (val instanceof java.util.UUID uuid) {
                                params[i - 1] = uuid.toString();
                            } else {
                                params[i - 1] = val;
                            }
                        }
                        jdbcTemplate.update(insertSql.toString(), params);
                        totalMigrated++;
                    }
                } catch (SQLException e) {
                    log.warn("Table {} skipped or not found in PostgreSQL source: {}", table, e.getMessage());
                }
            }
        }
        log.info("PostgreSQL to SQLite data migration completed. Total rows processed: {}", totalMigrated);
        return totalMigrated;
    }

    private String extractDbPath(String jdbcUrl) {
        if (jdbcUrl.startsWith("jdbc:sqlite:")) {
            return jdbcUrl.substring("jdbc:sqlite:".length());
        }
        return jdbcUrl;
    }
}
