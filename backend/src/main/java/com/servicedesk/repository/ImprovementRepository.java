package com.servicedesk.repository;

import com.servicedesk.model.ImprovementRequest;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ImprovementRepository {

    private final JdbcTemplate jdbcTemplate;

    public ImprovementRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<ImprovementRequest> ROW_MAPPER = (rs, rowNum) -> {
        ImprovementRequest r = new ImprovementRequest();
        r.setId(rs.getString("id"));
        r.setTitle(rs.getString("title"));
        r.setCategory(rs.getString("category"));
        r.setDescription(rs.getString("description"));
        r.setBusinessBenefit(rs.getString("business_benefit"));
        r.setStatus(rs.getString("status"));
        r.setPriority(rs.getString("priority"));
        r.setSubmittedBy(rs.getString("submitted_by"));
        r.setAssignedTo(rs.getString("assigned_to"));
        String created = rs.getString("created_at");
        if (created != null) r.setCreatedAt(Instant.parse(created));
        String updated = rs.getString("updated_at");
        if (updated != null) r.setUpdatedAt(Instant.parse(updated));

        try { r.setSubmittedByName(rs.getString("submitted_by_name")); } catch (Exception ignored) {}
        try { r.setAssignedToName(rs.getString("assigned_to_name")); } catch (Exception ignored) {}

        return r;
    };

    private static final String BASE_SELECT =
            "SELECT i.*, u1.name as submitted_by_name, u2.name as assigned_to_name " +
            "FROM improvement_requests i " +
            "LEFT JOIN users u1 ON i.submitted_by = u1.id " +
            "LEFT JOIN users u2 ON i.assigned_to = u2.id ";

    public List<ImprovementRequest> findAll(String status, String priority) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            sql.append(" AND i.status = ?");
            params.add(status);
        }
        if (priority != null && !priority.isBlank()) {
            sql.append(" AND i.priority = ?");
            params.add(priority);
        }
        sql.append(" ORDER BY i.created_at DESC");
        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
    }

    public Optional<ImprovementRequest> findById(String id) {
        String sql = BASE_SELECT + " WHERE i.id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, ROW_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void save(ImprovementRequest r) {
        String sql = "INSERT INTO improvement_requests (" +
                "id, title, category, description, business_benefit, status, priority, submitted_by, assigned_to, created_at, updated_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                r.getId(),
                r.getTitle(),
                r.getCategory(),
                r.getDescription(),
                r.getBusinessBenefit(),
                r.getStatus(),
                r.getPriority(),
                r.getSubmittedBy(),
                r.getAssignedTo(),
                r.getCreatedAt().toString(),
                r.getUpdatedAt().toString());
    }

    public void update(ImprovementRequest r) {
        String sql = "UPDATE improvement_requests SET status = ?, priority = ?, assigned_to = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                r.getStatus(),
                r.getPriority(),
                r.getAssignedTo(),
                r.getUpdatedAt().toString(),
                r.getId());
    }
}
