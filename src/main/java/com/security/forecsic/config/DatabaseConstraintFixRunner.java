package com.security.forecsic.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseConstraintFixRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        try {
            // Drop stale foreign key constraints that might have pointed to old table names in Postgres
            jdbcTemplate.execute("ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS fk7nvh9thn56dbvq6fofaasdxgc;");
            log.info("Database constraint checks and cleanups completed successfully.");
        } catch (Exception e) {
            log.warn("Non-critical notice during database constraint cleanup: {}", e.getMessage());
        }
    }
}
