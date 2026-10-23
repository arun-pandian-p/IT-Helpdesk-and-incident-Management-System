package com.servicedesk.repository;

import com.servicedesk.model.AccessRequest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AccessRequestRepository {

    private final JdbcTemplate jdbcTemplate;

    public AccessRequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<AccessRequest> ROW_MAPPER = (rs, rowNum) -> {
        AccessRequest a = new AccessRequest();
        a.setId(rs.getString("id"));
        a.setRequestType(rs.getString("request_type"));
        a.setRequestedSystem(rs.getString("requested_system"));
        a.setAccessLevel(rs.getString("access_level"));
        a.setReason(rs.getString("reason"));
        a.setRequestedBy(rs.getString("requested_by"));
        a.setApprovedBy(rs.getString("approved_by"));
        a.setAssignedEngineerId(rs.getString("assigned_engineer_id"));
        a.setStatus(rs.getString("status"));
        String comp = rs.getString("completed_at");
        if (comp != null) a.setCompletedAt(Instant.parse(comp));
        String created = rs.getString("created_at");
        if (created != null) a.setCreatedAt(Instant.parse(created));
        String updated = rs.getString("updated_at");
        if (updated != null) a.setUpdatedAt(Instant.parse(updated));

        try { a.setRequestedByName(rs.getString("requested_by_name")); } catch (Exception ignored) {}
        try { a.setApprovedByName(rs.getString("approved_by_name")); } catch (Exception ignored) {}
        try { a.setAssignedEngineerName(rs.getString("assigned_engineer_name")); } catch (Exception ignored) {}

        return a;
    };

    private static final String BASE_SELECT =
            "SELECT a.*, " +
            "u1.name as requested_by_name, " +
            "u2.name as approved_by_name, " +
            "u3.name as assigned_engineer_name " +
            "FROM access_requests a " +
            "LEFT JOIN users u1 ON a.requested_by = u1.id " +
            "LEFT JOIN users u2 ON a.approved_by = u2.id " +
            "LEFT JOIN users u3 ON a.assigned_engineer_id = u3.id ";

    public List<AccessRequest> findAll(String status) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            sql.append(" AND a.status = ?");
            params.add(status);
        }
        sql.append(" ORDER BY a.created_at DESC");
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
    }

    public Optional<AccessRequest> findById(String id) {
        String sql = BASE_SELECT + " WHERE a.id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, ROW_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void save(AccessRequest a) {
        String sql = "INSERT INTO access_requests (" +
                "id, request_type, requested_system, access_level, reason, requested_by, approved_by, " +
                "assigned_engineer_id, status, completed_at, created_at, updated_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                a.getId(),
                a.getRequestType(),
                a.getRequestedSystem(),
                a.getAccessLevel(),
                a.getReason(),
                a.getRequestedBy(),
                a.getApprovedBy(),
                a.getAssignedEngineerId(),
                a.getStatus(),
                a.getCompletedAt() != null ? a.getCompletedAt().toString() : null,
                a.getCreatedAt().toString(),
                a.getUpdatedAt().toString());
    }

    public void update(AccessRequest a) {
        String sql = "UPDATE access_requests SET status = ?, approved_by = ?, assigned_engineer_id = ?, " +
                "completed_at = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                a.getStatus(),
                a.getApprovedBy(),
                a.getAssignedEngineerId(),
                a.getCompletedAt() != null ? a.getCompletedAt().toString() : null,
                a.getUpdatedAt().toString(),
                a.getId());
    }

    public int countPending() {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM access_requests WHERE status IN ('Requested', 'Pending Approval')", Integer.class);
        return c != null ? c : 0;
    }
}
