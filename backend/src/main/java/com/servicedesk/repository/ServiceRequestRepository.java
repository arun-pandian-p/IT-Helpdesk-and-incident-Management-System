package com.servicedesk.repository;

import com.servicedesk.model.ServiceRequest;
import com.servicedesk.model.ServiceRequestComment;
import com.servicedesk.model.ServiceRequestTimeline;
import com.servicedesk.model.ServiceRequestType;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ServiceRequestRepository {

    private final JdbcTemplate jdbcTemplate;

    public ServiceRequestRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // -------------------------------------------------------------------------
    // Types
    // -------------------------------------------------------------------------
    private static final RowMapper<ServiceRequestType> TYPE_ROW_MAPPER = (rs, rowNum) -> {
        ServiceRequestType t = new ServiceRequestType();
        t.setId(rs.getString("id"));
        t.setName(rs.getString("name"));
        t.setDescription(rs.getString("description"));
        t.setCategory(rs.getString("category"));
        t.setDefaultPriority(rs.getString("default_priority"));
        t.setApprovalRequired(rs.getInt("approval_required") == 1);
        t.setDefaultSlaHours(rs.getInt("default_sla_hours"));
        t.setActive(rs.getInt("is_active") == 1);
        String created = rs.getString("created_at");
        if (created != null) t.setCreatedAt(Instant.parse(created));
        String updated = rs.getString("updated_at");
        if (updated != null) t.setUpdatedAt(Instant.parse(updated));
        return t;
    };

    public List<ServiceRequestType> findAllTypes(String category) {
        StringBuilder sql = new StringBuilder("SELECT * FROM service_request_types WHERE is_active = 1");
        List<Object> params = new ArrayList<>();
        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        sql.append(" ORDER BY category, name ASC");
        return jdbcTemplate.query(sql.toString(), TYPE_ROW_MAPPER, params.toArray());
    }

    public Optional<ServiceRequestType> findTypeById(String id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT * FROM service_request_types WHERE id = ?", TYPE_ROW_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<ServiceRequestType> findTypeByName(String name) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT * FROM service_request_types WHERE LOWER(name) = LOWER(?)", TYPE_ROW_MAPPER, name));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void saveType(ServiceRequestType t) {
        String sql = "INSERT INTO service_request_types (id, name, description, category, default_priority, approval_required, default_sla_hours, is_active, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                t.getId(),
                t.getName(),
                t.getDescription(),
                t.getCategory(),
                t.getDefaultPriority(),
                t.isApprovalRequired() ? 1 : 0,
                t.getDefaultSlaHours(),
                t.isActive() ? 1 : 0,
                t.getCreatedAt().toString(),
                t.getUpdatedAt().toString());
    }

    public void updateType(ServiceRequestType t) {
        String sql = "UPDATE service_request_types SET name = ?, description = ?, category = ?, default_priority = ?, approval_required = ?, default_sla_hours = ?, is_active = ?, updated_at = ? " +
                "WHERE id = ?";
        jdbcTemplate.update(sql,
                t.getName(),
                t.getDescription(),
                t.getCategory(),
                t.getDefaultPriority(),
                t.isApprovalRequired() ? 1 : 0,
                t.getDefaultSlaHours(),
                t.isActive() ? 1 : 0,
                t.getUpdatedAt().toString(),
                t.getId());
    }

    // -------------------------------------------------------------------------
    // Service Requests
    // -------------------------------------------------------------------------
    private static final RowMapper<ServiceRequest> SR_ROW_MAPPER = (rs, rowNum) -> {
        ServiceRequest s = new ServiceRequest();
        s.setId(rs.getString("id"));
        s.setRequestNumber(rs.getString("request_number"));
        s.setTitle(rs.getString("title"));
        s.setDescription(rs.getString("description"));
        s.setCategory(rs.getString("category"));
        s.setPriority(rs.getString("priority"));
        s.setStatus(rs.getString("status"));
        s.setRequestTypeId(rs.getString("request_type_id"));
        s.setRequesterId(rs.getString("requester_id"));
        s.setApprovalRequired(rs.getInt("approval_required") == 1);
        s.setApprovalStatus(rs.getString("approval_status"));
        s.setApproverId(rs.getString("approver_id"));
        s.setApprovalReason(rs.getString("approval_reason"));

        String appAt = rs.getString("approved_at");
        if (appAt != null) s.setApprovedAt(Instant.parse(appAt));

        s.setAssignedTo(rs.getString("assigned_to"));
        s.setAssignedTeam(rs.getString("assigned_team"));
        s.setBusinessJustification(rs.getString("business_justification"));
        s.setRequiredDate(rs.getString("required_date"));
        s.setAssetId(rs.getString("asset_id"));
        s.setApplicationName(rs.getString("application_name"));
        s.setAccessLevel(rs.getString("access_level"));
        s.setSoftwareName(rs.getString("software_name"));
        s.setSoftwareVersion(rs.getString("software_version"));
        s.setDeviceType(rs.getString("device_type"));
        s.setFulfillmentDetails(rs.getString("fulfillment_details"));
        s.setResolutionNotes(rs.getString("resolution_notes"));

        String due = rs.getString("due_at");
        if (due != null) s.setDueAt(Instant.parse(due));
        String fulfilled = rs.getString("fulfilled_at");
        if (fulfilled != null) s.setFulfilledAt(Instant.parse(fulfilled));
        String closed = rs.getString("closed_at");
        if (closed != null) s.setClosedAt(Instant.parse(closed));
        String created = rs.getString("created_at");
        if (created != null) s.setCreatedAt(Instant.parse(created));
        String updated = rs.getString("updated_at");
        if (updated != null) s.setUpdatedAt(Instant.parse(updated));

        try { s.setRequestTypeName(rs.getString("request_type_name")); } catch (Exception ignored) {}
        try { s.setRequesterName(rs.getString("requester_name")); } catch (Exception ignored) {}
        try { s.setRequesterEmail(rs.getString("requester_email")); } catch (Exception ignored) {}
        try { s.setRequesterDepartment(rs.getString("requester_department")); } catch (Exception ignored) {}
        try { s.setApproverName(rs.getString("approver_name")); } catch (Exception ignored) {}
        try { s.setAssigneeName(rs.getString("assignee_name")); } catch (Exception ignored) {}

        return s;
    };

    private static final String SR_BASE_SELECT =
            "SELECT s.*, " +
            "t.name as request_type_name, " +
            "u1.name as requester_name, u1.email as requester_email, u1.department as requester_department, " +
            "u2.name as approver_name, " +
            "u3.name as assignee_name " +
            "FROM service_requests s " +
            "LEFT JOIN service_request_types t ON s.request_type_id = t.id " +
            "LEFT JOIN users u1 ON s.requester_id = u1.id " +
            "LEFT JOIN users u2 ON s.approver_id = u2.id " +
            "LEFT JOIN users u3 ON s.assigned_to = u3.id ";

    public List<ServiceRequest> findAll(String status, String priority, String category,
                                        String assignedTo, String requesterId, String search,
                                        int page, int pageSize) {
        StringBuilder sql = new StringBuilder(SR_BASE_SELECT).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        buildSrFilters(sql, params, status, priority, category, assignedTo, requesterId, search);

        sql.append(" ORDER BY s.created_at DESC");
        int offset = (page - 1) * pageSize;
        sql.append(" LIMIT ? OFFSET ?");
        params.add(pageSize);
        params.add(offset);

        return jdbcTemplate.query(sql.toString(), SR_ROW_MAPPER, params.toArray());
    }

    public int count(String status, String priority, String category,
                     String assignedTo, String requesterId, String search) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM service_requests s WHERE 1=1");
        List<Object> params = new ArrayList<>();

        buildSrFilters(sql, params, status, priority, category, assignedTo, requesterId, search);

        Integer c = jdbcTemplate.queryForObject(sql.toString(), Integer.class, params.toArray());
        return c != null ? c : 0;
    }

    private void buildSrFilters(StringBuilder sql, List<Object> params, String status, String priority,
                                String category, String assignedTo, String requesterId, String search) {
        if (status != null && !status.isBlank()) {
            sql.append(" AND s.status = ?");
            params.add(status);
        }
        if (priority != null && !priority.isBlank()) {
            sql.append(" AND s.priority = ?");
            params.add(priority);
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND s.category = ?");
            params.add(category);
        }
        if (assignedTo != null && !assignedTo.isBlank()) {
            sql.append(" AND s.assigned_to = ?");
            params.add(assignedTo);
        }
        if (requesterId != null && !requesterId.isBlank()) {
            sql.append(" AND s.requester_id = ?");
            params.add(requesterId);
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (s.title LIKE ? OR s.description LIKE ? OR s.request_number LIKE ?)");
            String p = "%" + search + "%";
            params.add(p);
            params.add(p);
            params.add(p);
        }
    }

    public Optional<ServiceRequest> findById(String id) {
        String sql = SR_BASE_SELECT + " WHERE s.id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, SR_ROW_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<ServiceRequest> findByRequestNumber(String requestNumber) {
        String sql = SR_BASE_SELECT + " WHERE s.request_number = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, SR_ROW_MAPPER, requestNumber));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public String findHighestRequestNumberForYear(int year) {
        String prefix = String.format("SR-%d-%%", year);
        String sql = "SELECT request_number FROM service_requests WHERE request_number LIKE ? ORDER BY request_number DESC LIMIT 1";
        try {
            return jdbcTemplate.queryForObject(sql, String.class, prefix);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public void save(ServiceRequest s) {
        String sql = "INSERT INTO service_requests (" +
                "id, request_number, title, description, category, priority, status, request_type_id, requester_id, " +
                "approval_required, approval_status, approver_id, approval_reason, approved_at, assigned_to, assigned_team, " +
                "business_justification, required_date, asset_id, application_name, access_level, software_name, software_version, " +
                "device_type, fulfillment_details, resolution_notes, due_at, fulfilled_at, closed_at, created_at, updated_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                s.getId(),
                s.getRequestNumber(),
                s.getTitle(),
                s.getDescription(),
                s.getCategory(),
                s.getPriority(),
                s.getStatus(),
                s.getRequestTypeId(),
                s.getRequesterId(),
                s.isApprovalRequired() ? 1 : 0,
                s.getApprovalStatus(),
                s.getApproverId(),
                s.getApprovalReason(),
                s.getApprovedAt() != null ? s.getApprovedAt().toString() : null,
                s.getAssignedTo(),
                s.getAssignedTeam(),
                s.getBusinessJustification(),
                s.getRequiredDate(),
                s.getAssetId(),
                s.getApplicationName(),
                s.getAccessLevel(),
                s.getSoftwareName(),
                s.getSoftwareVersion(),
                s.getDeviceType(),
                s.getFulfillmentDetails(),
                s.getResolutionNotes(),
                s.getDueAt() != null ? s.getDueAt().toString() : null,
                s.getFulfilledAt() != null ? s.getFulfilledAt().toString() : null,
                s.getClosedAt() != null ? s.getClosedAt().toString() : null,
                s.getCreatedAt() != null ? s.getCreatedAt().toString() : null,
                s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null);
    }

    public void update(ServiceRequest s) {
        String sql = "UPDATE service_requests SET " +
                "title = ?, description = ?, category = ?, priority = ?, status = ?, approval_status = ?, approver_id = ?, " +
                "approval_reason = ?, approved_at = ?, assigned_to = ?, assigned_team = ?, fulfillment_details = ?, " +
                "resolution_notes = ?, fulfilled_at = ?, closed_at = ?, updated_at = ? " +
                "WHERE id = ?";
        jdbcTemplate.update(sql,
                s.getTitle(),
                s.getDescription(),
                s.getCategory(),
                s.getPriority(),
                s.getStatus(),
                s.getApprovalStatus(),
                s.getApproverId(),
                s.getApprovalReason(),
                s.getApprovedAt() != null ? s.getApprovedAt().toString() : null,
                s.getAssignedTo(),
                s.getAssignedTeam(),
                s.getFulfillmentDetails(),
                s.getResolutionNotes(),
                s.getFulfilledAt() != null ? s.getFulfilledAt().toString() : null,
                s.getClosedAt() != null ? s.getClosedAt().toString() : null,
                s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null,
                s.getId());
    }

    public List<ServiceRequest> getAllRequestsForMetrics() {
        return jdbcTemplate.query(SR_BASE_SELECT + " ORDER BY s.created_at ASC", SR_ROW_MAPPER);
    }

    // -------------------------------------------------------------------------
    // Comments & Timeline
    // -------------------------------------------------------------------------
    public List<ServiceRequestComment> findCommentsByRequestId(String requestId) {
        String sql = "SELECT c.*, u.name as user_name, u.role as user_role FROM service_request_comments c " +
                "LEFT JOIN users u ON c.user_id = u.id " +
                "WHERE c.request_id = ? ORDER BY c.created_at ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ServiceRequestComment comment = new ServiceRequestComment();
            comment.setId(rs.getString("id"));
            comment.setRequestId(rs.getString("request_id"));
            comment.setUserId(rs.getString("user_id"));
            comment.setUserName(rs.getString("user_name"));
            comment.setUserRole(rs.getString("user_role"));
            comment.setComment(rs.getString("comment"));
            comment.setInternal(rs.getInt("is_internal") == 1);
            String created = rs.getString("created_at");
            if (created != null) comment.setCreatedAt(Instant.parse(created));
            return comment;
        }, requestId);
    }

    public void saveComment(ServiceRequestComment c) {
        String sql = "INSERT INTO service_request_comments (id, request_id, user_id, comment, is_internal, created_at) VALUES (?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                c.getId(),
                c.getRequestId(),
                c.getUserId(),
                c.getComment(),
                c.isInternal() ? 1 : 0,
                c.getCreatedAt().toString());
    }

    public List<ServiceRequestTimeline> findTimelineByRequestId(String requestId) {
        String sql = "SELECT t.*, u.name as actor_name FROM service_request_timeline t " +
                "LEFT JOIN users u ON t.actor_id = u.id " +
                "WHERE t.request_id = ? ORDER BY t.created_at ASC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            ServiceRequestTimeline item = new ServiceRequestTimeline();
            item.setId(rs.getString("id"));
            item.setRequestId(rs.getString("request_id"));
            item.setEventType(rs.getString("event_type"));
            item.setTitle(rs.getString("title"));
            item.setDescription(rs.getString("description"));
            item.setActorId(rs.getString("actor_id"));
            item.setActorName(rs.getString("actor_name"));
            String created = rs.getString("created_at");
            if (created != null) item.setCreatedAt(Instant.parse(created));
            return item;
        }, requestId);
    }

    public void saveTimeline(ServiceRequestTimeline t) {
        String sql = "INSERT INTO service_request_timeline (id, request_id, event_type, title, description, actor_id, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                t.getId(),
                t.getRequestId(),
                t.getEventType(),
                t.getTitle(),
                t.getDescription(),
                t.getActorId(),
                t.getCreatedAt().toString());
    }
}
