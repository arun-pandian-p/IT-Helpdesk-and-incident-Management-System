package com.servicedesk.repository;

import com.servicedesk.model.Asset;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class AssetRepository {

    private final JdbcTemplate jdbcTemplate;

    public AssetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Asset> ROW_MAPPER = (rs, rowNum) -> {
        Asset a = new Asset();
        a.setId(rs.getString("id"));
        a.setAssetTag(rs.getString("asset_tag"));
        a.setDeviceType(rs.getString("device_type"));
        a.setManufacturer(rs.getString("manufacturer"));
        a.setModel(rs.getString("model"));
        a.setSerialNumber(rs.getString("serial_number"));
        a.setStatus(rs.getString("status"));
        a.setAssignedUserId(rs.getString("assigned_user_id"));
        a.setPurchaseDate(rs.getString("purchase_date"));
        a.setWarrantyExpiry(rs.getString("warranty_expiry"));
        a.setNotes(rs.getString("notes"));
        String created = rs.getString("created_at");
        if (created != null) a.setCreatedAt(Instant.parse(created));
        String updated = rs.getString("updated_at");
        if (updated != null) a.setUpdatedAt(Instant.parse(updated));

        try {
            a.setAssignedUserName(rs.getString("assigned_user_name"));
        } catch (Exception ignored) {}

        return a;
    };

    public List<Asset> findAll(String status, String deviceType, String search) {
        StringBuilder sql = new StringBuilder(
                "SELECT a.*, u.name as assigned_user_name FROM assets a " +
                "LEFT JOIN users u ON a.assigned_user_id = u.id WHERE 1=1"
        );
        List<Object> params = new ArrayList<>();

        if (status != null && !status.isBlank()) {
            sql.append(" AND a.status = ?");
            params.add(status);
        }
        if (deviceType != null && !deviceType.isBlank()) {
            sql.append(" AND a.device_type = ?");
            params.add(deviceType);
        }
        if (search != null && !search.isBlank()) {
            sql.append(" AND (a.asset_tag LIKE ? OR a.model LIKE ? OR a.serial_number LIKE ?)");
            String pattern = "%" + search + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        sql.append(" ORDER BY a.created_at DESC");

        return jdbcTemplate.query(sql.toString(), ROW_MAPPER, params.toArray());
    }

    public List<Asset> findByAssignedUserId(String userId) {
        String sql = "SELECT a.*, u.name as assigned_user_name FROM assets a " +
                "LEFT JOIN users u ON a.assigned_user_id = u.id WHERE a.assigned_user_id = ? ORDER BY a.created_at DESC";
        return jdbcTemplate.query(sql, ROW_MAPPER, userId);
    }

    public Optional<Asset> findById(String id) {
        String sql = "SELECT a.*, u.name as assigned_user_name FROM assets a " +
                "LEFT JOIN users u ON a.assigned_user_id = u.id WHERE a.id = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, ROW_MAPPER, id));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Asset> findByAssetTag(String tag) {
        String sql = "SELECT a.*, u.name as assigned_user_name FROM assets a " +
                "LEFT JOIN users u ON a.assigned_user_id = u.id WHERE a.asset_tag = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, ROW_MAPPER, tag));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public Optional<Asset> findBySerialNumber(String serial) {
        String sql = "SELECT a.*, u.name as assigned_user_name FROM assets a " +
                "LEFT JOIN users u ON a.assigned_user_id = u.id WHERE a.serial_number = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, ROW_MAPPER, serial));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void save(Asset a) {
        String sql = "INSERT INTO assets (id, asset_tag, device_type, manufacturer, model, serial_number, status, assigned_user_id, purchase_date, warranty_expiry, notes, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.update(sql,
                a.getId(),
                a.getAssetTag(),
                a.getDeviceType(),
                a.getManufacturer(),
                a.getModel(),
                a.getSerialNumber(),
                a.getStatus(),
                a.getAssignedUserId(),
                a.getPurchaseDate(),
                a.getWarrantyExpiry(),
                a.getNotes(),
                a.getCreatedAt().toString(),
                a.getUpdatedAt().toString());
    }

    public void update(Asset a) {
        String sql = "UPDATE assets SET device_type = ?, manufacturer = ?, model = ?, status = ?, assigned_user_id = ?, purchase_date = ?, warranty_expiry = ?, notes = ?, updated_at = ? " +
                "WHERE id = ?";
        jdbcTemplate.update(sql,
                a.getDeviceType(),
                a.getManufacturer(),
                a.getModel(),
                a.getStatus(),
                a.getAssignedUserId(),
                a.getPurchaseDate(),
                a.getWarrantyExpiry(),
                a.getNotes(),
                a.getUpdatedAt().toString(),
                a.getId());
    }

    public int count() {
        Integer c = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM assets", Integer.class);
        return c != null ? c : 0;
    }
}
