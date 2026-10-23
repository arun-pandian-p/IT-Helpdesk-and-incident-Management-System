package com.servicedesk.service;

import com.servicedesk.dto.CommonDtos.AccessRequestCreateRequest;
import com.servicedesk.dto.CommonDtos.AccessRequestStatusUpdateRequest;
import com.servicedesk.exception.AppException;
import com.servicedesk.model.AccessRequest;
import com.servicedesk.model.Constants.AccessRequestStatus;
import com.servicedesk.model.Constants.UserRole;
import com.servicedesk.repository.AccessRequestRepository;
import com.servicedesk.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AccessRequestService {

    private final AccessRequestRepository accessRequestRepository;
    private final AuditService auditService;

    public AccessRequestService(AccessRequestRepository accessRequestRepository, AuditService auditService) {
        this.accessRequestRepository = accessRequestRepository;
        this.auditService = auditService;
    }

    public List<AccessRequest> getRequests(String status) {
        return accessRequestRepository.findAll(status);
    }

    public AccessRequest getRequestById(String id) {
        return accessRequestRepository.findById(id)
                .orElseThrow(() -> new AppException("Access request not found.", HttpStatus.NOT_FOUND));
    }

    @Transactional
    public AccessRequest createRequest(AccessRequestCreateRequest req, UserPrincipal principal) {
        Instant now = Instant.now();
        AccessRequest a = new AccessRequest();
        a.setId(UUID.randomUUID().toString());
        a.setRequestType(req.getRequestType());
        a.setRequestedSystem(req.getRequestedSystem());
        a.setAccessLevel(req.getAccessLevel() != null ? req.getAccessLevel() : "Read/Write");
        a.setReason(req.getReason());
        a.setRequestedBy(principal.getId());
        a.setStatus(AccessRequestStatus.REQUESTED);
        a.setCreatedAt(now);
        a.setUpdatedAt(now);

        accessRequestRepository.save(a);
        auditService.logAudit(principal.getId(), "ACCESS_REQUEST_CREATED", "AccessRequest", a.getId(),
                "Requested access to " + a.getRequestedSystem());

        return getRequestById(a.getId());
    }

    @Transactional
    public AccessRequest updateStatus(String id, AccessRequestStatusUpdateRequest req, UserPrincipal principal) {
        requireSupportOrAdmin(principal);

        AccessRequest a = accessRequestRepository.findById(id)
                .orElseThrow(() -> new AppException("Access request not found.", HttpStatus.NOT_FOUND));

        a.setStatus(req.getStatus());
        if (AccessRequestStatus.APPROVED.equals(req.getStatus())) {
            a.setApprovedBy(principal.getId());
        } else if (AccessRequestStatus.COMPLETED.equals(req.getStatus())) {
            a.setCompletedAt(Instant.now());
            a.setAssignedEngineerId(principal.getId());
        }
        a.setUpdatedAt(Instant.now());

        accessRequestRepository.update(a);
        auditService.logAudit(principal.getId(), "ACCESS_STATUS_UPDATED", "AccessRequest", a.getId(),
                "Status set to " + req.getStatus());

        return getRequestById(a.getId());
    }

    private void requireSupportOrAdmin(UserPrincipal principal) {
        if (principal == null || (!UserRole.SUPPORT.equals(principal.getRole()) && !UserRole.ADMIN.equals(principal.getRole()))) {
            throw new AppException("Access forbidden: requires SUPPORT or ADMIN role.", HttpStatus.FORBIDDEN);
        }
    }
}
