package com.servicedesk.repository;

import com.servicedesk.model.KnowledgeArticle;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class KnowledgeRepository {

    private final JdbcTemplate jdbcTemplate;

    public KnowledgeRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<KnowledgeArticle> ROW_MAPPER = (rs, rowNum) -> {
        KnowledgeArticle a = new KnowledgeArticle();
        a.setId(rs.getString("id"));
        a.setTitle(rs.getString("title"));
        a.setCategory(rs.getString("category"));
        a.setOsTarget(rs.getString("os_target"));
        a.setContent(rs.getString("content"));
        a.setTags(rs.getString("tags"));
        a.setViewCount(rs.getInt("view_count"));
        a.setHelpfulCount(rs.getInt("helpful_count"));
        a.setNotHelpfulCount(rs.getInt("not_helpful_count"));
        a.setCreatedBy(rs.getString("created_by"));
        String created = rs.getString("created_at");
        if (created != null) a.setCreatedAt(Instant.parse(created));
        String updated = rs.getString("updated_at");
        if (updated != null) a.setUpdatedAt(Instant.parse(updated));

        try { a.setCreatedByName(rs.getString("created_by_name")); } catch (Exception ignored) {}

        return a;
    };

    private static final String BASE_SELECT =
            "SELECT k.*, u.name as created_by_name FROM knowledge_articles k " +
            "LEFT JOIN users u ON k.created_by = u.id ";

    public List<KnowledgeArticle> findAll(String category, String osTarget, String search) {
        StringBuilder sql = new StringBuilder(BASE_SELECT).append(" WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (category != null && !category.isBlank()) {
            sql.append(" AND k.category = ?");
            params.add(category);
        }
        if (osTarget != null && !osTarget.isBlank()) {
            sql.append(" AND (k.os_target = ? OR k.os_target = 'All')");
            params.add(osTarget);
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (k.title LIKE ? OR k.content LIKE ? OR k.tags LIKE ?)");
            String p = "%" + search + "%";
            params.add(p);
            params.add(p);
            params.add(p);
        }
        sql.append(" ORDER BY k.view_count DESC, k.created_at DESC");

        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
    }

    public Optional<KnowledgeArticle> findById(String id) {
        String sql = BASE_SELECT + " WHERE k.id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, ROW_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void save(KnowledgeArticle a) {
        String sql = "INSERT INTO knowledge_articles (" +
                "id, title, category, os_target, content, tags, view_count, helpful_count, not_helpful_count, created_by, created_at, updated_at" +
                ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                a.getId(),
                a.getTitle(),
                a.getCategory(),
                a.getOsTarget(),
                a.getContent(),
                a.getTags(),
                a.getViewCount(),
                a.getHelpfulCount(),
                a.getNotHelpfulCount(),
                a.getCreatedBy(),
                a.getCreatedAt().toString(),
                a.getUpdatedAt().toString());
    }

    public void incrementViewCount(String id) {
        jdbcTemplate.update("UPDATE knowledge_articles SET view_count = view_count + 1 WHERE id = ?", id);
    }

    public void recordVote(String id, boolean helpful) {
        if (helpful) {
            jdbcTemplate.update("UPDATE knowledge_articles SET helpful_count = helpful_count + 1 WHERE id = ?", id);
        } else {
            jdbcTemplate.update("UPDATE knowledge_articles SET not_helpful_count = not_helpful_count + 1 WHERE id = ?", id);
        }
    }

    public int count() {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM knowledge_articles", Integer.class);
        return c != null ? c : 0;
    }
}
