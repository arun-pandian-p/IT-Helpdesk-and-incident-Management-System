package com.servicedesk.service;

import com.servicedesk.dto.CommonDtos.OnboardingCreateRequest;
import com.servicedesk.dto.CommonDtos.OnboardingTaskUpdateRequest;
import com.servicedesk.exception.AppException;
import com.servicedesk.model.Constants.OnboardingTaskStatus;
import com.servicedesk.model.Constants.UserRole;
import com.servicedesk.model.OnboardingRequest;
import com.servicedesk.model.OnboardingTask;
import com.servicedesk.repository.OnboardingRepository;
import com.servicedesk.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OnboardingService {

    private final OnboardingRepository onboardingRepository;
    private final AuditService auditService;

    public OnboardingService(OnboardingRepository onboardingRepository, AuditService auditService) {
        this.onboardingRepository = onboardingRepository;
        this.auditService = auditService;
    }

    public List<OnboardingRequest> getRequests(String department) {
        return onboardingRepository.findAll(department);
    }

    public OnboardingRequest getRequestById(String id) {
        return onboardingRepository.findById(id)
                .orElseThrow(() -> new AppException("Onboarding request not found.", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public OnboardingRequest createRequest(OnboardingCreateRequest req, UserPrincipal principal) {
        Instant now = Instant.now();
        OnboardingRequest r = new OnboardingRequest();
        r.setId(UUID.randomUUID().toString());
        r.setEmployeeName(req.getEmployeeName());
        r.setDepartment(req.getDepartment());
        r.setJobTitle(req.getJobTitle());
        r.setManagerName(req.getManagerName());
        r.setStartDate(req.getStartDate());
        r.setLocation(req.getLocation());
        r.setDeviceRequirement(req.getDeviceRequirement());
        r.setSoftwareRequirement(req.getSoftwareRequirement());
        r.setAccessRequirement(req.getAccessRequirement());
        r.setStatus("In Progress");
        r.setProgressPercent(0);
        r.setCreatedBy(principal.getId());
        r.setCreatedAt(now);
        r.setUpdatedAt(now);

        onboardingRepository.saveRequest(r);

        // Default onboarding tasks
        List<String> defaultTasks = List.of(
                "Assign hardware & provision laptop",
                "Create Google Workspace / Microsoft 365 account",
                "Configure VPN & SSO credentials",
                "Provision department SaaS software",
                "Security compliance & phishing orientation",
                "Deliver hardware & workstation setup"
        );

        for (String taskName : defaultTasks) {
            OnboardingTask task = new OnboardingTask();
            task.setId(UUID.randomUUID().toString());
            task.setRequestId(r.getId());
            task.setTaskName(taskName);
            task.setStatus(OnboardingTaskStatus.NOT_STARTED);
            task.setDueDate(r.getStartDate());
            onboardingRepository.saveTask(task);
        }

        auditService.logAudit(principal.getId(), "ONBOARDING_CREATED", "OnboardingRequest", r.getId(),
                "Created onboarding request for " + r.getEmployeeName());

        return getRequestById(r.getId());
    }

    @Transactional
    public OnboardingTask updateTask(String taskId, OnboardingTaskUpdateRequest req, UserPrincipal principal) {
        requireSupportOrAdmin(principal);

        OnboardingTask task = onboardingRepository.findTaskById(taskId)
                .orElseThrow(() -> new AppException("Onboarding task not found.", HttpStatus.NOT_FOUND));

        if (req.getStatus() != null) {
            task.setStatus(req.getStatus());
            if (OnboardingTaskStatus.COMPLETED.equals(req.getStatus())) {
                task.setCompletedAt(Instant.now());
            } else {
                task.setCompletedAt(null);
            }
        }
        if (req.getNotes() != null) task.setNotes(req.getNotes());
        if (req.getOwnerId() != null) task.setOwnerId(req.getOwnerId());

        onboardingRepository.updateTask(task);

        // Recalculate request progress
        OnboardingRequest request = onboardingRepository.findById(task.getRequestId()).orElse(null);
        if (request != null) {
            List<OnboardingTask> tasks = onboardingRepository.findTasksByRequestId(request.getId());
            long completedCount = tasks.stream().filter(t -> OnboardingTaskStatus.COMPLETED.equals(t.getStatus())).count();
            int progress = tasks.isEmpty() ? 0 : (int) Math.round((double) completedCount / tasks.size() * 100);
            request.setProgressPercent(progress);
            if (progress == 100) {
                request.setStatus("Completed");
            } else {
                request.setStatus("In Progress");
            }
            onboardingRepository.updateRequest(request);
        }

        return onboardingRepository.findTaskById(taskId).orElse(task);
    }

    private void requireSupportOrAdmin(UserPrincipal principal) {
        if (principal == null || (!UserRole.SUPPORT.equals(principal.getRole()) && !UserRole.ADMIN.equals(principal.getRole()))) {
            throw new AppException("Access forbidden: requires SUPPORT or ADMIN role.", HttpStatus.FORBIDDEN);
        }
    }
}
