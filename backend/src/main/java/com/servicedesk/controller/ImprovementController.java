package com.servicedesk.controller;

import com.servicedesk.dto.CommonDtos.ImprovementCreateRequest;
import com.servicedesk.dto.CommonDtos.ImprovementUpdateRequest;
import com.servicedesk.model.ImprovementRequest;
import com.servicedesk.security.UserPrincipal;
import com.servicedesk.service.ImprovementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/improvements")
public class ImprovementController {

    private final ImprovementService improvementService;

    public ImprovementController(ImprovementService improvementService) {
        this.improvementService = improvementService;
    }

    @GetMapping
    public ResponseEntity<List<ImprovementRequest>> getImprovements(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority) {
        return ResponseEntity.ok(improvementService.getImprovements(status, priority));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ImprovementRequest> getImprovement(@PathVariable String id) {
        return ResponseEntity.ok(improvementService.getImprovementById(id));
    }

    @PostMapping
    public ResponseEntity<ImprovementRequest> createImprovement(
            @Valid @RequestBody ImprovementCreateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        ImprovementRequest created = improvementService.createImprovement(req, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ImprovementRequest> updateImprovement(
            @PathVariable String id,
            @RequestBody ImprovementUpdateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(improvementService.updateImprovement(id, req, principal));
    }
}
