package com.servicedesk.repository;

import com.servicedesk.model.AuditLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public class AuditRepository {

    private final JdbcTemplate jdbcTemplate;

    public AuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<AuditLog> ROW_MAPPER = (rs, rowNum) -> {
        AuditLog a = new AuditLog();
        a.setId(rs.getString("id"));
        a.setUserId(rs.getString("user_id"));
        a.setAction(rs.getString("action"));
        a.setEntityType(rs.getString("entity_type"));
        a.setEntityId(rs.getString("entity_id"));
        a.setDetails(rs.getString("details"));
        String created = rs.getString("created_at");
        if (created != null) a.setCreatedAt(Instant.parse(created));
        try { a.setUserName(rs.getString("user_name")); } catch (Exception ignored) {}
        return a;
    };

    public void save(AuditLog a) {
        String sql = "INSERT INTO audit_logs (id, user_id, action, entity_type, entity_id, details, created_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                a.getId(),
                a.getUserId(),
                a.getAction(),
                a.getEntityType(),
                a.getEntityId(),
                a.getDetails(),
                a.getCreatedAt().toString());
    }

    public List<AuditLog> findRecent(int limit) {
        String sql = "SELECT a.*, u.name as user_name FROM audit_logs a " +
                "LEFT JOIN users u ON a.user_id = u.id " +
                "ORDER BY a.created_at DESC LIMIT ?";
        return jdbcTemplate.query(sql, ROW_MAPPER, limit);
    }
}
