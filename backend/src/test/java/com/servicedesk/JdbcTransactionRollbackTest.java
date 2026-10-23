package com.servicedesk;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(JdbcTransactionRollbackTest.TransactionalTestService.class)
class JdbcTransactionRollbackTest {

    @Autowired
    private TransactionalTestService transactionalTestService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Transaction must rollback changes when an unhandled runtime exception occurs")
    void testTransactionRollback() {
        String testEmail = "rollback_test_" + UUID.randomUUID() + "@example.com";

        assertThrows(RuntimeException.class, () -> {
            transactionalTestService.executeFailedTransaction(testEmail);
        });

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE email = ?", Integer.class, testEmail);

        assertEquals(0, count, "User should have been rolled back and not persisted in database");
    }

    @Service
    public static class TransactionalTestService {
        private final JdbcTemplate jdbcTemplate;

        public TransactionalTestService(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @Transactional
        public void executeFailedTransaction(String email) {
            String id = UUID.randomUUID().toString();
            jdbcTemplate.update(
                    "INSERT INTO users (id, name, email, password_hash, role, department, is_active, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, datetime('now'), datetime('now'))",
                    id, "Rollback User", email, "hash", "EMPLOYEE", "Test", 1);

            // Trigger forced runtime error to test rollback
            throw new RuntimeException("Forced simulated database failure during transactional workflow");
        }
    }
}
