package com.servicedesk.controller;

import com.servicedesk.dto.CommonDtos.AccessRequestCreateRequest;
import com.servicedesk.dto.CommonDtos.AccessRequestStatusUpdateRequest;
import com.servicedesk.model.AccessRequest;
import com.servicedesk.security.UserPrincipal;
import com.servicedesk.service.AccessRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/access-requests")
public class AccessRequestController {

    private final AccessRequestService accessRequestService;

    public AccessRequestController(AccessRequestService accessRequestService) {
        this.accessRequestService = accessRequestService;
    }

    @GetMapping
    public ResponseEntity<List<AccessRequest>> getRequests(
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(accessRequestService.getRequests(status));
    }

    @PostMapping
    public ResponseEntity<AccessRequest> createRequest(
            @Valid @RequestBody AccessRequestCreateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AccessRequest created = accessRequestService.createRequest(req, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AccessRequest> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody AccessRequestStatusUpdateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        AccessRequest updated = accessRequestService.updateStatus(id, req, principal);
        return ResponseEntity.ok(updated);
    }
}
