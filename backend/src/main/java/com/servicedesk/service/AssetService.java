package com.servicedesk.service;

import com.servicedesk.dto.CommonDtos.AssetCreateRequest;
import com.servicedesk.dto.CommonDtos.AssetUpdateRequest;
import com.servicedesk.exception.AppException;
import com.servicedesk.model.Asset;
import com.servicedesk.model.Constants.UserRole;
import com.servicedesk.repository.AssetRepository;
import com.servicedesk.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final AuditService auditService;

    public AssetService(AssetRepository assetRepository, AuditService auditService) {
        this.assetRepository = assetRepository;
        this.auditService = auditService;
    }

    public List<Asset> getAssets(String status, String deviceType, String search) {
        return assetRepository.findAll(status, deviceType, search);
    }

    public List<Asset> getMyDevices(String userId) {
        return assetRepository.findByAssignedUserId(userId);
    }

    public Asset getAssetById(String id) {
        return assetRepository.findById(id)
                .orElseThrow(() -> new AppException("Asset not found.", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Asset createAsset(AssetCreateRequest req, UserPrincipal principal) {
        requireSupportOrAdmin(principal);

        if (assetRepository.findByAssetTag(req.getAssetTag()).isPresent()) {
            throw new AppException("Asset with tag '" + req.getAssetTag() + "' already exists.", HttpStatus.BAD_REQUEST);
        }
        if (assetRepository.findBySerialNumber(req.getSerialNumber()).isPresent()) {
            throw new AppException("Asset with serial number '" + req.getSerialNumber() + "' already exists.", HttpStatus.BAD_REQUEST);
        }

        Instant now = Instant.now();
        Asset a = new Asset();
        a.setId(UUID.randomUUID().toString());
        a.setAssetTag(req.getAssetTag());
        a.setDeviceType(req.getDeviceType());
        a.setManufacturer(req.getManufacturer());
        a.setModel(req.getModel());
        a.setSerialNumber(req.getSerialNumber());
        a.setStatus(req.getStatus() != null ? req.getStatus() : "In Use");
        a.setAssignedUserId(req.getAssignedUserId());
        a.setPurchaseDate(req.getPurchaseDate());
        a.setWarrantyExpiry(req.getWarrantyExpiry());
        a.setNotes(req.getNotes());
        a.setCreatedAt(now);
        a.setUpdatedAt(now);

        assetRepository.save(a);
        auditService.logAudit(principal.getId(), "ASSET_CREATED", "Asset", a.getId(), "Created asset tag: " + a.getAssetTag());
        return getAssetById(a.getId());
    }

    @Transactional
    public Asset updateAsset(String id, AssetUpdateRequest req, UserPrincipal principal) {
        requireSupportOrAdmin(principal);

        Asset a = assetRepository.findById(id)
                .orElseThrow(() -> new AppException("Asset not found.", HttpStatus.NOT_FOUND));

        if (req.getDeviceType() != null) a.setDeviceType(req.getDeviceType());
        if (req.getManufacturer() != null) a.setManufacturer(req.getManufacturer());
        if (req.getModel() != null) a.setModel(req.getModel());
        if (req.getStatus() != null) a.setStatus(req.getStatus());
        if (req.getAssignedUserId() != null) a.setAssignedUserId(req.getAssignedUserId());
        if (req.getPurchaseDate() != null) a.setPurchaseDate(req.getPurchaseDate());
        if (req.getWarrantyExpiry() != null) a.setWarrantyExpiry(req.getWarrantyExpiry());
        if (req.getNotes() != null) a.setNotes(req.getNotes());
        a.setUpdatedAt(Instant.now());

        assetRepository.update(a);
        auditService.logAudit(principal.getId(), "ASSET_UPDATED", "Asset", a.getId(), "Updated asset tag: " + a.getAssetTag());
        return getAssetById(a.getId());
    }

    private void requireSupportOrAdmin(UserPrincipal principal) {
        if (principal == null || (!UserRole.SUPPORT.equals(principal.getRole()) && !UserRole.ADMIN.equals(principal.getRole()))) {
            throw new AppException("Access forbidden: requires SUPPORT or ADMIN role.", HttpStatus.FORBIDDEN);
        }
    }
}
