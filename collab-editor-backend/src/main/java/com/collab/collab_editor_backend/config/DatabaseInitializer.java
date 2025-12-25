package com.collab.collab_editor_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) throws Exception {
        logger.info("Checking and creating required database tables...");

        try {
            // Ensure t_document_version has required columns
            String ensureDocumentVersionTable = "CREATE TABLE IF NOT EXISTS t_document_version (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "doc_id BIGINT NOT NULL, " +
                    "version_number INT NOT NULL, " +
                    "content TEXT, " +
                    "version_name VARCHAR(255), " +
                    "created_by BIGINT, " +
                    "created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "is_locked TINYINT(1) DEFAULT 0, " +
                    "description VARCHAR(255)" +
                    ")";
            jdbcTemplate.execute(ensureDocumentVersionTable);
            // Add missing columns if table already exists (MySQL <8.0 compatibility: check information_schema)
            addColumnIfMissing("t_document_version", "version_name", "VARCHAR(255)");
            addColumnIfMissing("t_document_version", "created_by", "BIGINT");
            addColumnIfMissing("t_document_version", "created_time", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            addColumnIfMissing("t_document_version", "is_locked", "TINYINT(1) DEFAULT 0");
            logger.info("Table 't_document_version' check/creation completed.");
            // Create t_video_meeting table if not exists
            String createVideoMeetingTable = "CREATE TABLE IF NOT EXISTS t_video_meeting (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "meeting_id VARCHAR(50) NOT NULL, " +
                    "channel_name VARCHAR(50) NOT NULL, " +
                    "token VARCHAR(255), " +
                    "creator_id BIGINT NOT NULL, " +
                    "doc_id BIGINT NOT NULL, " +
                    "title VARCHAR(255), " +
                    "status INT DEFAULT 0, " +
                    "start_time TIMESTAMP, " +
                    "end_time TIMESTAMP, " +
                    "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
                    ")";
            
            jdbcTemplate.execute(createVideoMeetingTable);
            logger.info("Table 't_video_meeting' check/creation completed.");

            // Create error_log table if not exists
            String createErrorLogTable = "CREATE TABLE IF NOT EXISTS error_log (" +
                    "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                    "`timestamp` DATETIME NOT NULL, " +
                    "`type` VARCHAR(50) NOT NULL, " +
                    "`message` TEXT NOT NULL, " +
                    "`stack` TEXT, " +
                    "`url` VARCHAR(255), " +
                    "`line` INT, " +
                    "`column` INT, " +
                    "`user_agent` TEXT, " +
                    "`user_id` BIGINT, " +
                    "`doc_id` BIGINT, " +
                    "`additional_info` TEXT, " +
                    "`create_time` DATETIME NOT NULL" +
                    ")";
            
            jdbcTemplate.execute(createErrorLogTable);
            logger.info("Table 'error_log' check/creation completed.");
            
        } catch (Exception e) {
            logger.error("Error initializing database tables: " + e.getMessage(), e);
        }
    }

    private void addColumnIfMissing(String tableName, String columnName, String columnDefinition) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class, tableName, columnName);
            if (count != null && count == 0) {
                String sql = String.format("ALTER TABLE %s ADD COLUMN %s %s", tableName, columnName, columnDefinition);
                jdbcTemplate.execute(sql);
                logger.info("Added missing column '{}' to table '{}'", columnName, tableName);
            }
        } catch (Exception ex) {
            logger.error("Failed to add column '{}' to table '{}': {}", columnName, tableName, ex.getMessage());
        }
    }
}
