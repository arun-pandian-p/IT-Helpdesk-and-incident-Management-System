package com.servicedesk.repository;

import com.servicedesk.model.OnboardingRequest;
import com.servicedesk.model.OnboardingTask;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class OnboardingRepository {

    private final JdbcTemplate jdbcTemplate;

    public OnboardingRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<OnboardingRequest> REQ_ROW_MAPPER = (rs, rowNum) -> {
        OnboardingRequest r = new OnboardingRequest();
        r.setId(rs.getString("id"));
        r.setEmployeeName(rs.getString("employee_name"));
        r.setDepartment(rs.getString("department"));
        r.setJobTitle(rs.getString("job_title"));
        r.setManagerName(rs.getString("manager_name"));
        r.setStartDate(rs.getString("start_date"));
        r.setLocation(rs.getString("location"));
        r.setDeviceRequirement(rs.getString("device_requirement"));
        r.setSoftwareRequirement(rs.getString("software_requirement"));
        r.setAccessRequirement(rs.getString("access_requirement"));
        r.setStatus(rs.getString("status"));
        r.setProgressPercent(rs.getInt("progress_percent"));
        r.setCreatedBy(rs.getString("created_by"));
        String created = rs.getString("created_at");
        if (created != null) r.setCreatedAt(Instant.parse(created));
        String updated = rs.getString("updated_at");
        if (updated != null) r.setUpdatedAt(Instant.parse(updated));
        return r;
    };

    private static final RowMapper<OnboardingTask> TASK_ROW_MAPPER = (rs, rowNum) -> {
        OnboardingTask t = new OnboardingTask();
        t.setId(rs.getString("id"));
        t.setRequestId(rs.getString("request_id"));
        t.setTaskName(rs.getString("task_name"));
        t.setOwnerId(rs.getString("owner_id"));
        t.setStatus(rs.getString("status"));
        t.setDueDate(rs.getString("due_date"));
        String comp = rs.getString("completed_at");
        if (comp != null) t.setCompletedAt(Instant.parse(comp));
        t.setNotes(rs.getString("notes"));
        try { t.setOwnerName(rs.getString("owner_name")); } catch (Exception ignored) {}
        return t;
    };

    public List<OnboardingRequest> findAll(String department) {
        StringBuilder sql = new StringBuilder("SELECT * FROM onboarding_requests WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (department != null && !department.isBlank()) {
            sql.append(" AND department = ?");
            params.add(department);
        }
        sql.append(" ORDER BY start_date DESC");
        List<OnboardingRequest> requests = jdbcTemplate.query(sql.toString(), REQ_ROW_MAPPER, params.toArray());
        for (OnboardingRequest req : requests) {
            req.setTasks(findTasksByRequestId(req.getId()));
        }
        return requests;
    }

    public Optional<OnboardingRequest> findById(String id) {
        String sql = "SELECT * FROM onboarding_requests WHERE id = ?";
        try {
            OnboardingRequest req = jdbcTemplate.queryForObject(sql, REQ_ROW_MAPPER, id);
            if (req != null) {
                req.setTasks(findTasksByRequestId(req.getId()));
            }
            return Optional.ofNullable(req);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void saveRequest(OnboardingRequest r) {
        String sql = "INSERT INTO onboarding_requests (" +
                "id, employee_name, department, job_title, manager_name, start_date, location, device_requirement, " +
                "software_requirement, access_requirement, status, progress_percent, created_by, created_at, updated_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                r.getId(),
                r.getEmployeeName(),
                r.getDepartment(),
                r.getJobTitle(),
                r.getManagerName(),
                r.getStartDate(),
                r.getLocation(),
                r.getDeviceRequirement(),
                r.getSoftwareRequirement(),
                r.getAccessRequirement(),
                r.getStatus(),
                r.getProgressPercent(),
                r.getCreatedBy(),
                r.getCreatedAt().toString(),
                r.getUpdatedAt().toString());
    }

    public void updateRequest(OnboardingRequest r) {
        String sql = "UPDATE onboarding_requests SET status = ?, progress_percent = ?, updated_at = ? WHERE id = ?";
        jdbcTemplate.update(sql, r.getStatus(), r.getProgressPercent(), r.getUpdatedAt().toString(), r.getId());
    }

    public List<OnboardingTask> findTasksByRequestId(String requestId) {
        String sql = "SELECT t.*, u.name as owner_name FROM onboarding_tasks t " +
                "LEFT JOIN users u ON t.owner_id = u.id WHERE t.request_id = ? ORDER BY t.id ASC";
        return jdbcTemplate.query(sql, TASK_ROW_MAPPER, requestId);
    }

    public Optional<OnboardingTask> findTaskById(String taskId) {
        String sql = "SELECT t.*, u.name as owner_name FROM onboarding_tasks t " +
                "LEFT JOIN users u ON t.owner_id = u.id WHERE t.id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, TASK_ROW_MAPPER, taskId));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void saveTask(OnboardingTask t) {
        String sql = "INSERT INTO onboarding_tasks (id, request_id, task_name, owner_id, status, due_date, completed_at, notes) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                t.getId(),
                t.getRequestId(),
                t.getTaskName(),
                t.getOwnerId(),
                t.getStatus(),
                t.getDueDate(),
                t.getCompletedAt() != null ? t.getCompletedAt().toString() : null,
                t.getNotes());
    }

    public void updateTask(OnboardingTask t) {
        String sql = "UPDATE onboarding_tasks SET status = ?, notes = ?, owner_id = ?, completed_at = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                t.getStatus(),
                t.getNotes(),
                t.getOwnerId(),
                t.getCompletedAt() != null ? t.getCompletedAt().toString() : null,
                t.getId());
    }

    public int countActive() {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM onboarding_requests WHERE status != 'Completed'", Integer.class);
        return c != null ? c : 0;
    }
}
