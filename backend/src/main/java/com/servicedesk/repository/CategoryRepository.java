package com.servicedesk.repository;

import com.servicedesk.model.TicketCategory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public class CategoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public CategoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<TicketCategory> ROW_MAPPER = (rs, rowNum) -> {
        TicketCategory c = new TicketCategory();
        c.setId(rs.getString("id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        c.setActive(rs.getInt("is_active") == 1);
        String created = rs.getString("created_at");
        if (created != null) c.setCreatedAt(Instant.parse(created));
        return c;
    };

    public List<TicketCategory> findAll() {
        return jdbcTemplate.query("SELECT * FROM ticket_categories WHERE is_active = 1 ORDER BY name ASC", ROW_MAPPER);
    }

    public Optional<TicketCategory> findById(String id) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT * FROM ticket_categories WHERE id = ?", ROW_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<TicketCategory> findByName(String name) {
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(
                    "SELECT * FROM ticket_categories WHERE LOWER(name) = LOWER(?)", ROW_MAPPER, name));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void save(TicketCategory category) {
        String sql = "INSERT INTO ticket_categories (id, name, description, is_active, created_at) VALUES (?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive() ? 1 : 0,
                category.getCreatedAt().toString());
    }

    public int count() {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ticket_categories", Integer.class);
        return c != null ? c : 0;
    }
}
