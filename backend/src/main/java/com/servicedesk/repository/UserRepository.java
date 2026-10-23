package com.servicedesk.repository;

import com.servicedesk.model.User;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<User> ROW_MAPPER = (rs, rowNum) -> {
        User u = new User();
        u.setId(rs.getString("id"));
        u.setName(rs.getString("name"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setDepartment(rs.getString("department"));
        u.setJobTitle(rs.getString("job_title"));
        u.setPhone(rs.getString("phone"));
        u.setLocation(rs.getString("location"));
        u.setManagerId(rs.getString("manager_id"));
        u.setActive(rs.getInt("is_active") == 1);
        String created = rs.getString("created_at");
        if (created != null) u.setCreatedAt(Instant.parse(created));
        String updated = rs.getString("updated_at");
        if (updated != null) u.setUpdatedAt(Instant.parse(updated));
        return u;
    };

    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, ROW_MAPPER, id);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?)";
        try {
            User user = jdbcTemplate.queryForObject(sql, ROW_MAPPER, email);
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public List<User> findAll(String role, String department) {
        StringBuilder sql = new StringBuilder("SELECT * FROM users WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (role != null && !role.isBlank()) {
            sql.append(" AND role = ?");
            params.add(role);
        }
        if (department != null && !department.isBlank()) {
            sql.append(" AND department = ?");
            params.add(department);
        }
        sql.append(" ORDER BY name ASC");

        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
    }

    public List<User> findSupportStaff() {
        String sql = "SELECT * FROM users WHERE role IN ('SUPPORT', 'ADMIN') AND is_active = 1 ORDER BY name ASC";
        return jdbcTemplate.query(sql, ROW_MAPPER);
    }

    public void save(User user) {
        String sql = "INSERT INTO users (id, name, email, password_hash, role, department, job_title, phone, location, manager_id, is_active, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                user.getId(),
                user.getName(),
                user.getEmail().toLowerCase(),
                user.getPasswordHash(),
                user.getRole(),
                user.getDepartment(),
                user.getJobTitle(),
                user.getPhone(),
                user.getLocation(),
                user.getManagerId(),
                user.isActive() ? 1 : 0,
                user.getCreatedAt().toString(),
                user.getUpdatedAt().toString());
    }

    public void update(User user) {
        String sql = "UPDATE users SET name = ?, email = ?, role = ?, department = ?, job_title = ?, phone = ?, location = ?, manager_id = ?, is_active = ?, updated_at = ? " +
                "WHERE id = ?";
        jdbcTemplate.update(sql,
                user.getName(),
                user.getEmail().toLowerCase(),
                user.getRole(),
                user.getDepartment(),
                user.getJobTitle(),
                user.getPhone(),
                user.getLocation(),
                user.getManagerId(),
                user.isActive() ? 1 : 0,
                user.getUpdatedAt().toString(),
                user.getId());
    }

    public int count() {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
        return c != null ? c : 0;
    }
}
