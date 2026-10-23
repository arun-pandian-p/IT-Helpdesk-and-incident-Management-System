package com.servicedesk.service;

import com.servicedesk.dto.CommonDtos.ImprovementCreateRequest;
import com.servicedesk.dto.CommonDtos.ImprovementUpdateRequest;
import com.servicedesk.exception.AppException;
import com.servicedesk.model.Constants.ImprovementStatus;
import com.servicedesk.model.Constants.UserRole;
import com.servicedesk.model.ImprovementRequest;
import com.servicedesk.repository.ImprovementRepository;
import com.servicedesk.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ImprovementService {

    private final ImprovementRepository improvementRepository;
    private final AuditService auditService;

    public ImprovementService(ImprovementRepository improvementRepository, AuditService auditService) {
        this.improvementRepository = improvementRepository;
        this.auditService = auditService;
    }

    public List<ImprovementRequest> getImprovements(String status, String priority) {
        return improvementRepository.findAll(status, priority);
    }

    public ImprovementRequest getImprovementById(String id) {
        return improvementRepository.findById(id)
                .orElseThrow(() -> new AppException("Improvement request not found.", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public ImprovementRequest createImprovement(ImprovementCreateRequest req, UserPrincipal principal) {
        Instant now = Instant.now();
        ImprovementRequest r = new ImprovementRequest();
        r.setId(UUID.randomUUID().toString());
        r.setTitle(req.getTitle());
        r.setCategory(req.getCategory());
        r.setDescription(req.getDescription());
        r.setBusinessBenefit(req.getBusinessBenefit());
        r.setStatus(ImprovementStatus.PROPOSED);
        r.setPriority(req.getPriority() != null ? req.getPriority() : "Medium");
        r.setSubmittedBy(principal.getId());
        r.setCreatedAt(now);
        r.setUpdatedAt(now);

        improvementRepository.save(r);
        auditService.logAudit(principal.getId(), "IMPROVEMENT_CREATED", "ImprovementRequest", r.getId(),
                "Proposed improvement: " + r.getTitle());

        return getImprovementById(r.getId());
    }

    @Transactional
    public ImprovementRequest updateImprovement(String id, ImprovementUpdateRequest req, UserPrincipal principal) {
        requireSupportOrAdmin(principal);

        ImprovementRequest r = improvementRepository.findById(id)
                .orElseThrow(() -> new AppException("Improvement request not found.", HttpStatus.NOT_FOUND));

        if (req.getStatus() != null) r.setStatus(req.getStatus());
        if (req.getPriority() != null) r.setPriority(req.getPriority());
        if (req.getAssignedTo() != null) r.setAssignedTo(req.getAssignedTo());
        r.setUpdatedAt(Instant.now());

        improvementRepository.update(r);
        auditService.logAudit(principal.getId(), "IMPROVEMENT_UPDATED", "ImprovementRequest", r.getId(),
                "Updated status to " + r.getStatus());

        return getImprovementById(r.getId());
    }

    private void requireSupportOrAdmin(UserPrincipal principal) {
        if (principal == null || (!UserRole.SUPPORT.equals(principal.getRole()) && !UserRole.ADMIN.equals(principal.getRole()))) {
            throw new AppException("Access forbidden: requires SUPPORT or ADMIN role.", HttpStatus.FORBIDDEN);
        }
    }
}
