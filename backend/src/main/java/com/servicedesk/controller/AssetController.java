package com.servicedesk.controller;

import com.servicedesk.dto.CommonDtos.AssetCreateRequest;
import com.servicedesk.dto.CommonDtos.AssetUpdateRequest;
import com.servicedesk.model.Asset;
import com.servicedesk.security.UserPrincipal;
import com.servicedesk.service.AssetService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    public ResponseEntity<List<Asset>> getAssets(
            @RequestParam(required = false) String status,
            @RequestParam(name = "device_type", required = false) String deviceType,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(assetService.getAssets(status, deviceType, search));
    }

    @GetMapping("/my-devices")
    public ResponseEntity<List<Asset>> getMyDevices(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(assetService.getMyDevices(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Asset> getAsset(@PathVariable String id) {
        return ResponseEntity.ok(assetService.getAssetById(id));
    }

    @PostMapping
    public ResponseEntity<Asset> createAsset(
            @Valid @RequestBody AssetCreateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        Asset asset = assetService.createAsset(req, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(asset);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Asset> updateAsset(
            @PathVariable String id,
            @RequestBody AssetUpdateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        Asset asset = assetService.updateAsset(id, req, principal);
        return ResponseEntity.ok(asset);
    }
}
