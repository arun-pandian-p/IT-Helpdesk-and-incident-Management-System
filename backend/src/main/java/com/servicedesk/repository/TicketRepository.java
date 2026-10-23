package com.servicedesk.repository;

import com.servicedesk.model.*;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class TicketRepository {

    private final JdbcTemplate jdbcTemplate;

    public TicketRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Ticket> TICKET_ROW_MAPPER = (rs, rowNum) -> {
        Ticket t = new Ticket();
        t.setId(rs.getString("id"));
        t.setTicketNumber(rs.getString("ticket_number"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));
        t.setCategoryId(rs.getString("category_id"));
        t.setPriority(rs.getString("priority"));
        t.setStatus(rs.getString("status"));
        t.setImpact(rs.getString("impact"));
        t.setUrgency(rs.getString("urgency"));
        t.setPriorityOverrideReason(rs.getString("priority_override_reason"));
        t.setAssetId(rs.getString("asset_id"));
        t.setDeviceType(rs.getString("device_type"));
        t.setOsName(rs.getString("os_name"));
        t.setApplicationName(rs.getString("application_name"));
        t.setLocation(rs.getString("location"));
        t.setCreatedBy(rs.getString("created_by"));
        t.setAssignedTo(rs.getString("assigned_to"));

        String due = rs.getString("due_at");
        if (due != null) t.setDueAt(Instant.parse(due));
        String firstResp = rs.getString("first_response_at");
        if (firstResp != null) t.setFirstResponseAt(Instant.parse(firstResp));
        String resolved = rs.getString("resolved_at");
        if (resolved != null) t.setResolvedAt(Instant.parse(resolved));
        String closed = rs.getString("closed_at");
        if (closed != null) t.setClosedAt(Instant.parse(closed));

        t.setResolutionNotes(rs.getString("resolution_notes"));
        t.setEscalated(rs.getInt("is_escalated") == 1);
        t.setEscalationLevel(rs.getInt("escalation_level"));
        t.setChecklistState(rs.getString("checklist_state"));

        String created = rs.getString("created_at");
        if (created != null) t.setCreatedAt(Instant.parse(created));
        String updated = rs.getString("updated_at");
        if (updated != null) t.setUpdatedAt(Instant.parse(updated));

        try { t.setCategoryName(rs.getString("category_name")); } catch (Exception ignored) {}
        try { t.setCreatorName(rs.getString("creator_name")); } catch (Exception ignored) {}
        try { t.setCreatorEmail(rs.getString("creator_email")); } catch (Exception ignored) {}
        try { t.setCreatorDepartment(rs.getString("creator_department")); } catch (Exception ignored) {}
        try { t.setAssigneeName(rs.getString("assignee_name")); } catch (Exception ignored) {}

        return t;
    };

    private static final String BASE_SELECT =
            "SELECT t.*, " +
            "c.name as category_name, " +
            "u1.name as creator_name, u1.email as creator_email, u1.department as creator_department, " +
            "u2.name as assignee_name " +
            "FROM tickets t " +
            "LEFT JOIN ticket_categories c ON t.category_id = c.id " +
            "LEFT JOIN users u1 ON t.created_by = u1.id " +
            "LEFT JOIN users u2 ON t.assigned_to = u2.id ";

    public List<Ticket> findAll(String status, String priority, String categoryId,
                               String assignedTo, String createdBy, String search,
                               int page, int pageSize) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        buildFilters(sql, params, status, priority, categoryId, assignedTo, createdBy, search);

        sql.append(" ORDER BY t.created_at DESC");
        int offset = (page - 1) * pageSize;
        sql.append(" LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), TICKET_ROW_MAPPER, params.toArray());
    }

    public int count(String status, String priority, String categoryId,
                     String assignedTo, String createdBy, String search) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM tickets t WHERE 1=1");
        List<Object> params = new ArrayList<>();

        buildFilters(sql, params, status, priority, categoryId, assignedTo, createdBy, search);

        Integer c = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return c != null ? c : 0;
    }

    private void buildFilters(StringBuilder sql, List<Object> params, String status, String priority,
                              String categoryId, String assignedTo, String createdBy, String search) {
        if (status != null && !status.isBlank()) {
            sql.append(" AND t.status = ?");
            params.add(status);
        }
        if (priority != null && !priority.isBlank()) {
            sql.append(" AND t.priority = ?");
            params.add(priority);
        }
        if (categoryId != null && !categoryId.isBlank()) {
            sql.append(" AND t.category_id = ?");
            params.add(categoryId);
        }
        if (assignedTo != null && !assignedTo.isBlank()) {
            sql.append(" AND t.assigned_to = ?");
            params.add(assignedTo);
        }
        if (createdBy != null && !createdBy.isBlank()) {
            sql.append(" AND t.created_by = ?");
            params.add(createdBy);
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (t.title LIKE ? OR t.description LIKE ? OR t.ticket_number LIKE ?)");
            String p = "%" + search + "%";
            params.add(p);
            params.add(p);
            params.add(p);
        }
    }

    public Optional<Ticket> findById(String id) {
        String sql = BASE_SELECT + " WHERE t.id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, TICKET_ROW_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Ticket> findByTicketNumber(String ticketNumber) {
        String sql = BASE_SELECT + " WHERE t.ticket_number = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, TICKET_ROW_MAPPER, ticketNumber));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public String findHighestTicketNumberForYear(int year) {
        String prefix = String.format("INC-%d-%%", year);
        String sql = "SELECT ticket_number FROM tickets WHERE ticket_number LIKE ? ORDER BY ticket_number DESC LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, prefix);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public void save(Ticket t) {
        String sql = "INSERT INTO tickets (" +
                "id, ticket_number, title, description, category_id, priority, status, impact, urgency, priority_override_reason, " +
                "asset_id, device_type, os_name, application_name, location, created_by, assigned_to, due_at, first_response_at, " +
                "resolved_at, closed_at, resolution_notes, is_escalated, escalation_level, checklist_state, created_at, updated_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                t.getId(),
                t.getTicketNumber(),
                t.getTitle(),
                t.getDescription(),
                t.getCategoryId(),
                t.getPriority(),
                t.getStatus(),
                t.getImpact(),
                t.getUrgency(),
                t.getPriorityOverrideReason(),
                t.getAssetId(),
                t.getDeviceType(),
                t.getOsName(),
                t.getApplicationName(),
                t.getLocation(),
                t.getCreatedBy(),
                t.getAssignedTo(),
                t.getDueAt() != null ? t.getDueAt().toString() : null,
                t.getFirstResponseAt() != null ? t.getFirstResponseAt().toString() : null,
                t.getResolvedAt() != null ? t.getResolvedAt().toString() : null,
                t.getClosedAt() != null ? t.getClosedAt().toString() : null,
                t.getResolutionNotes(),
                t.isEscalated() ? 1 : 0,
                t.getEscalationLevel(),
                t.getChecklistState(),
                t.getCreatedAt() != null ? t.getCreatedAt().toString() : null,
                t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : null);
    }

    public void update(Ticket t) {
        String sql = "UPDATE tickets SET " +
                "title = ?, description = ?, category_id = ?, priority = ?, status = ?, impact = ?, urgency = ?, " +
                "priority_override_reason = ?, asset_id = ?, device_type = ?, os_name = ?, application_name = ?, location = ?, " +
                "assigned_to = ?, due_at = ?, first_response_at = ?, resolved_at = ?, closed_at = ?, resolution_notes = ?, " +
                "is_escalated = ?, escalation_level = ?, checklist_state = ?, updated_at = ? " +
                "WHERE id = ?";
        jdbcTemplate.update(sql,
                t.getTitle(),
                t.getDescription(),
                t.getCategoryId(),
                t.getPriority(),
                t.getStatus(),
                t.getImpact(),
                t.getUrgency(),
                t.getPriorityOverrideReason(),
                t.getAssetId(),
                t.getDeviceType(),
                t.getOsName(),
                t.getApplicationName(),
                t.getLocation(),
                t.getAssignedTo(),
                t.getDueAt() != null ? t.getDueAt().toString() : null,
                t.getFirstResponseAt() != null ? t.getFirstResponseAt().toString() : null,
                t.getResolvedAt() != null ? t.getResolvedAt().toString() : null,
                t.getClosedAt() != null ? t.getClosedAt().toString() : null,
                t.getResolutionNotes(),
                t.isEscalated() ? 1 : 0,
                t.getEscalationLevel(),
                t.getChecklistState(),
                t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : null,
                t.getId());
    }

    public List<Ticket> getAllTicketsForMetrics() {
        return jdbcTemplate.query(BASE_SELECT + " ORDER BY t.created_at ASC", TICKET_ROW_MAPPER);
    }

    // -------------------------------------------------------------------------
    // Comments
    // -------------------------------------------------------------------------
    public List<TicketComment> findCommentsByTicketId(String ticketId) {
        String sql = "SELECT c.*, u.name as user_name, u.role as user_role FROM ticket_comments c " +
                "LEFT JOIN users u ON c.user_id = u.id " +
                "WHERE c.ticket_id = ? ORDER BY c.created_at ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            TicketComment comment = new TicketComment();
            comment.setId(rs.getString("id"));
            comment.setTicketId(rs.getString("ticket_id"));
            comment.setUserId(rs.getString("user_id"));
            comment.setUserName(rs.getString("user_name"));
            comment.setUserRole(rs.getString("user_role"));
            comment.setComment(rs.getString("comment"));
            comment.setInternal(rs.getInt("is_internal") == 1);
            String created = rs.getString("created_at");
            if (created != null) comment.setCreatedAt(Instant.parse(created));
            return comment;
        }, ticketId);
    }

    public void saveComment(TicketComment comment) {
        String sql = "INSERT INTO ticket_comments (id, ticket_id, user_id, comment, is_internal, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                comment.getId(),
                comment.getTicketId(),
                comment.getUserId(),
                comment.getComment(),
                comment.isInternal() ? 1 : 0,
                comment.getCreatedAt().toString());
    }

    // -------------------------------------------------------------------------
    // Status History
    // -------------------------------------------------------------------------
    public List<TicketStatusHistory> findStatusHistoryByTicketId(String ticketId) {
        String sql = "SELECT h.*, u.name as changed_by_name FROM ticket_status_history h " +
                "LEFT JOIN users u ON h.changed_by = u.id " +
                "WHERE h.ticket_id = ? ORDER BY h.changed_at ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            TicketStatusHistory hist = new TicketStatusHistory();
            hist.setId(rs.getString("id"));
            hist.setTicketId(rs.getString("ticket_id"));
            hist.setOldStatus(rs.getString("old_status"));
            hist.setNewStatus(rs.getString("new_status"));
            hist.setChangedBy(rs.getString("changed_by"));
            hist.setChangedByName(rs.getString("changed_by_name"));
            String changed = rs.getString("changed_at");
            if (changed != null) hist.setChangedAt(Instant.parse(changed));
            hist.setReason(rs.getString("reason"));
            return hist;
        }, ticketId);
    }

    public void saveStatusHistory(TicketStatusHistory hist) {
        String sql = "INSERT INTO ticket_status_history (id, ticket_id, old_status, new_status, changed_by, changed_at, reason) VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                hist.getId(),
                hist.getTicketId(),
                hist.getOldStatus(),
                hist.getNewStatus(),
                hist.getChangedBy(),
                hist.getChangedAt().toString(),
                hist.getReason());
    }

    // -------------------------------------------------------------------------
    // Escalations
    // -------------------------------------------------------------------------
    public List<TicketEscalation> findEscalationsByTicketId(String ticketId) {
        String sql = "SELECT e.*, u1.name as from_engineer_name, u2.name as to_engineer_name " +
                "FROM ticket_escalations e " +
                "LEFT JOIN users u1 ON e.from_engineer = u1.id " +
                "LEFT JOIN users u2 ON e.to_engineer = u2.id " +
                "WHERE e.ticket_id = ? ORDER BY e.created_at ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            TicketEscalation esc = new TicketEscalation();
            esc.setId(rs.getString("id"));
            esc.setTicketId(rs.getString("ticket_id"));
            esc.setFromEngineer(rs.getString("from_engineer"));
            esc.setFromEngineerName(rs.getString("from_engineer_name"));
            esc.setToEngineer(rs.getString("to_engineer"));
            esc.setToEngineerName(rs.getString("to_engineer_name"));
            esc.setReason(rs.getString("reason"));
            esc.setEscalationLevel(rs.getInt("escalation_level"));
            String created = rs.getString("created_at");
            if (created != null) esc.setCreatedAt(Instant.parse(created));
            String resolved = rs.getString("resolved_at");
            if (resolved != null) esc.setResolvedAt(Instant.parse(resolved));
            return esc;
        }, ticketId);
    }

    public void saveEscalation(TicketEscalation esc) {
        String sql = "INSERT INTO ticket_escalations (id, ticket_id, from_engineer, to_engineer, reason, escalation_level, created_at, resolved_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                esc.getId(),
                esc.getTicketId(),
                esc.getFromEngineer(),
                esc.getToEngineer(),
                esc.getReason(),
                esc.getEscalationLevel(),
                esc.getCreatedAt().toString(),
                esc.getResolvedAt() != null ? esc.getResolvedAt().toString() : null);
    }

    // -------------------------------------------------------------------------
    // Feedback
    // -------------------------------------------------------------------------
    public Optional<TicketFeedback> findFeedbackByTicketId(String ticketId) {
        String sql = "SELECT * FROM ticket_feedback WHERE ticket_id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                TicketFeedback f = new TicketFeedback();
                f.setId(rs.getString("id"));
                f.setTicketId(rs.getString("ticket_id"));
                f.setUserId(rs.getString("user_id"));
                f.setRating(rs.getInt("rating"));
                f.setComments(rs.getString("comments"));
                String created = rs.getString("created_at");
                if (created != null) f.setCreatedAt(Instant.parse(created));
                return f;
            }, ticketId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void saveFeedback(TicketFeedback f) {
        String sql = "INSERT INTO ticket_feedback (id, ticket_id, user_id, rating, comments, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                f.getId(),
                f.getTicketId(),
                f.getUserId(),
                f.getRating(),
                f.getComments(),
                f.getCreatedAt().toString());
    }
}
