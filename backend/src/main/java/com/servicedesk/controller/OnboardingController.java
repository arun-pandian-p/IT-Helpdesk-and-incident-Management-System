package com.servicedesk.controller;

import com.servicedesk.dto.CommonDtos.OnboardingCreateRequest;
import com.servicedesk.dto.CommonDtos.OnboardingTaskUpdateRequest;
import com.servicedesk.model.OnboardingRequest;
import com.servicedesk.model.OnboardingTask;
import com.servicedesk.security.UserPrincipal;
import com.servicedesk.service.OnboardingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping
    public ResponseEntity<List<OnboardingRequest>> getRequests(
            @RequestParam(required = false) String department) {
        return ResponseEntity.ok(onboardingService.getRequests(department));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OnboardingRequest> getRequest(@PathVariable String id) {
        return ResponseEntity.ok(onboardingService.getRequestById(id));
    }

    @PostMapping
    public ResponseEntity<OnboardingRequest> createRequest(
            @Valid @RequestBody OnboardingCreateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        OnboardingRequest created = onboardingService.createRequest(req, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/tasks/{taskId}")
    public ResponseEntity<OnboardingTask> updateTask(
            @PathVariable String taskId,
            @RequestBody OnboardingTaskUpdateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        OnboardingTask updated = onboardingService.updateTask(taskId, req, principal);
        return ResponseEntity.ok(updated);
    }
}
