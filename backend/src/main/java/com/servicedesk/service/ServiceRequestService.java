package com.servicedesk.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicedesk.dto.ServiceRequestDtos.*;
import com.servicedesk.exception.AppException;
import com.servicedesk.model.Constants.*;
import com.servicedesk.model.ServiceRequest;
import com.servicedesk.model.ServiceRequestComment;
import com.servicedesk.model.ServiceRequestTimeline;
import com.servicedesk.model.ServiceRequestType;
import com.servicedesk.model.Ticket;
import com.servicedesk.repository.ServiceRequestRepository;
import com.servicedesk.repository.TicketRepository;
import com.servicedesk.repository.UserRepository;
import com.servicedesk.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.Year;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ServiceRequestService {

    private final ServiceRequestRepository srRepository;
    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final SlaService slaService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public ServiceRequestService(ServiceRequestRepository srRepository,
                                 TicketRepository ticketRepository,
                                 UserRepository userRepository,
                                 SlaService slaService,
                                 AuditService auditService,
                                 ObjectMapper objectMapper) {
        this.srRepository = srRepository;
        this.ticketRepository = ticketRepository;
        this.userRepository = userRepository;
        this.slaService = slaService;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    public synchronized String generateRequestNumber() {
        int currentYear = Year.now().getValue();
        String prefix = String.format("SR-%d-", currentYear);
        String lastNumber = srRepository.findHighestRequestNumberForYear(currentYear);

        int nextSeq = 1;
        if (lastNumber != null) {
            Pattern p = Pattern.compile("^SR-\\d{4}-(\\d{6})$");
            Matcher m = p.matcher(lastNumber);
            if (m.find()) {
                try {
                    nextSeq = Integer.parseInt(m.group(1)) + 1;
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("%s%06d", prefix, nextSeq);
    }

    // -------------------------------------------------------------------------
    // Catalog Types
    // -------------------------------------------------------------------------
    public List<ServiceRequestType> getRequestTypes(String category) {
        return srRepository.findAllTypes(category);
    }

    @Transactional
    public ServiceRequestType createRequestType(ServiceRequestTypeCreateRequest req, UserPrincipal principal) {
        requireAdmin(principal);

        if (srRepository.findTypeByName(req.getName()).isPresent()) {
            throw new AppException("Service request type '" + req.getName() + "' already exists.", HttpStatus.BAD_REQUEST);
        }

        Instant now = Instant.now();
        ServiceRequestType t = new ServiceRequestType();
        t.setId(UUID.randomUUID().toString());
        t.setName(req.getName());
        t.setDescription(req.getDescription());
        t.setCategory(req.getCategory());
        t.setDefaultPriority(req.getDefaultPriority());
        t.setApprovalRequired(req.isApprovalRequired());
        t.setDefaultSlaHours(req.getDefaultSlaHours());
        t.setActive(true);
        t.setCreatedAt(now);
        t.setUpdatedAt(now);

        srRepository.saveType(t);
        auditService.logAudit(principal.getId(), "SR_TYPE_CREATED", "ServiceRequestType", t.getId(), "Created catalog item: " + t.getName());
        return t;
    }

    @Transactional
    public ServiceRequestType updateRequestType(String id, ServiceRequestTypeUpdateRequest req, UserPrincipal principal) {
        requireAdmin(principal);

        ServiceRequestType t = srRepository.findTypeById(id)
                .orElseThrow(() -> new AppException("Service request type not found.", HttpStatus.NOT_FOUND));

        if (req.getName() != null) t.setName(req.getName());
        if (req.getDescription() != null) t.setDescription(req.getDescription());
        if (req.getCategory() != null) t.setCategory(req.getCategory());
        if (req.getDefaultPriority() != null) t.setDefaultPriority(req.getDefaultPriority());
        t.setApprovalRequired(req.isApprovalRequired());
        if (req.getDefaultSlaHours() > 0) t.setDefaultSlaHours(req.getDefaultSlaHours());
        t.setActive(req.isActive());
        t.setUpdatedAt(Instant.now());

        srRepository.updateType(t);
        return t;
    }

    // -------------------------------------------------------------------------
    // Service Requests
    // -------------------------------------------------------------------------
    @Transactional
    public ServiceRequestDto createServiceRequest(ServiceRequestCreateRequest req, UserPrincipal principal) {
        ServiceRequestType type = srRepository.findTypeById(req.getRequestTypeId())
                .orElseThrow(() -> new AppException("Selected service request catalog type does not exist.", HttpStatus.BAD_REQUEST));

        Instant now = Instant.now();
        int slaHours = type.getDefaultSlaHours() > 0 ? type.getDefaultSlaHours() : 24;
        Instant dueAt = now.plus(slaHours, ChronoUnit.HOURS);

        boolean approvalRequired = type.isApprovalRequired();
        String initialStatus = approvalRequired ? ServiceRequestStatus.PENDING_APPROVAL : ServiceRequestStatus.SUBMITTED;
        String approvalStatus = approvalRequired ? ServiceRequestApprovalStatus.PENDING : ServiceRequestApprovalStatus.NONE;

        ServiceRequest sr = new ServiceRequest();
        sr.setId(UUID.randomUUID().toString());
        sr.setRequestNumber(generateRequestNumber());
        sr.setTitle(req.getTitle());
        sr.setDescription(req.getDescription());
        sr.setCategory(type.getCategory());
        sr.setPriority(type.getDefaultPriority());
        sr.setStatus(initialStatus);
        sr.setRequestTypeId(type.getId());
        sr.setRequesterId(principal.getId());
        sr.setApprovalRequired(approvalRequired);
        sr.setApprovalStatus(approvalStatus);
        sr.setAssignedTeam(SupportTeam.SERVICE_DESK);
        sr.setBusinessJustification(req.getBusinessJustification());
        sr.setRequiredDate(req.getRequiredDate());
        sr.setAssetId(req.getAssetId());
        sr.setApplicationName(req.getApplicationName());
        sr.setAccessLevel(req.getAccessLevel());
        sr.setSoftwareName(req.getSoftwareName());
        sr.setSoftwareVersion(req.getSoftwareVersion());
        sr.setDeviceType(req.getDeviceType());
        sr.setDueAt(dueAt);
        sr.setCreatedAt(now);
        sr.setUpdatedAt(now);

        srRepository.save(sr);

        // Record Timeline
        recordTimeline(sr.getId(), "SUBMITTED", "Request Submitted",
                "Service request submitted by " + principal.getName(), principal.getId());

        if (approvalRequired) {
            recordTimeline(sr.getId(), "APPROVAL_REQUESTED", "Manager Approval Pending",
                    "This request requires management approval before fulfillment.", principal.getId());
        }

        auditService.logAudit(principal.getId(), "SR_CREATED", "ServiceRequest", sr.getId(),
                "Created service request " + sr.getRequestNumber() + ": " + sr.getTitle());

        return getServiceRequestById(sr.getId(), principal);
    }

    public ServiceRequestListResponse getServiceRequests(String status, String priority, String category,
                                                         String assignedTo, String requesterId, String search,
                                                         int page, int pageSize, UserPrincipal principal) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;

        List<ServiceRequest> list = srRepository.findAll(status, priority, category, assignedTo, requesterId, search, page, pageSize);
        int total = srRepository.count(status, priority, category, assignedTo, requesterId, search);

        List<ServiceRequestDto> dtos = list.stream().map(s -> {
            s.setSlaStatus(slaService.calculateSrSlaStatus(s));
            return ServiceRequestDto.fromEntity(s);
        }).collect(Collectors.toList());

        return new ServiceRequestListResponse(dtos, total, page, pageSize);
    }

    public ServiceRequestDto getServiceRequestById(String id, UserPrincipal principal) {
        ServiceRequest sr = srRepository.findById(id)
                .orElseThrow(() -> new AppException("Service request not found.", HttpStatus.NOT_FOUND));

        sr.setSlaStatus(slaService.calculateSrSlaStatus(sr));
        ServiceRequestDto dto = ServiceRequestDto.fromEntity(sr);

        // Comments
        boolean isSupportOrAdmin = principal != null &&
                (UserRole.SUPPORT.equals(principal.getRole()) || UserRole.ADMIN.equals(principal.getRole()));

        List<ServiceRequestComment> comments = srRepository.findCommentsByRequestId(id);
        List<ServiceRequestCommentDto> commentDtos = comments.stream()
                .filter(c -> !c.isInternal() || isSupportOrAdmin)
                .map(ServiceRequestCommentDto::fromEntity)
                .collect(Collectors.toList());
        dto.setComments(commentDtos);

        // Timeline
        List<ServiceRequestTimeline> timeline = srRepository.findTimelineByRequestId(id);
        dto.setTimeline(timeline.stream().map(ServiceRequestTimelineDto::fromEntity).collect(Collectors.toList()));

        return dto;
    }

    @Transactional
    public ServiceRequestDto approve(String id, boolean approved, String reason, UserPrincipal principal) {
        requireSupportOrAdmin(principal);

        ServiceRequest sr = srRepository.findById(id)
                .orElseThrow(() -> new AppException("Service request not found.", HttpStatus.NOT_FOUND));

        Instant now = Instant.now();
        if (approved) {
            sr.setStatus(ServiceRequestStatus.APPROVED);
            sr.setApprovalStatus(ServiceRequestApprovalStatus.APPROVED);
            sr.setApproverId(principal.getId());
            sr.setApprovalReason(reason != null ? reason : "Approved by IT administration.");
            sr.setApprovedAt(now);
            sr.setUpdatedAt(now);
            srRepository.update(sr);

            recordTimeline(sr.getId(), "APPROVED", "Request Approved",
                    "Approved by " + principal.getName() + ": " + sr.getApprovalReason(), principal.getId());
        } else {
            sr.setStatus(ServiceRequestStatus.REJECTED);
            sr.setApprovalStatus(ServiceRequestApprovalStatus.REJECTED);
            sr.setApproverId(principal.getId());
            sr.setApprovalReason(reason != null ? reason : "Rejected by IT administration.");
            sr.setApprovedAt(now);
            sr.setUpdatedAt(now);
            srRepository.update(sr);

            recordTimeline(sr.getId(), "REJECTED", "Request Rejected",
                    "Rejected by " + principal.getName() + ": " + sr.getApprovalReason(), principal.getId());
        }

        auditService.logAudit(principal.getId(), "SR_APPROVAL_DECISION", "ServiceRequest", sr.getId(),
                "Approval status set to " + sr.getApprovalStatus());

        return getServiceRequestById(id, principal);
    }

    @Transactional
    public ServiceRequestDto reject(String id, String reason, UserPrincipal principal) {
        return approve(id, false, reason, principal);
    }

    @Transactional
    public ServiceRequestDto assign(String id, String assignedTo, String assignedTeam, UserPrincipal principal) {
        requireSupportOrAdmin(principal);

        ServiceRequest sr = srRepository.findById(id)
                .orElseThrow(() -> new AppException("Service request not found.", HttpStatus.NOT_FOUND));

        if (assignedTo != null && !assignedTo.isBlank()) {
            userRepository.findById(assignedTo)
                    .orElseThrow(() -> new AppException("Assignee user not found.", HttpStatus.NOT_FOUND));
            sr.setAssignedTo(assignedTo);
        }
        if (assignedTeam != null && !assignedTeam.isBlank()) {
            sr.setAssignedTeam(assignedTeam);
        }

        if (ServiceRequestStatus.SUBMITTED.equals(sr.getStatus()) || ServiceRequestStatus.APPROVED.equals(sr.getStatus())) {
            sr.setStatus(ServiceRequestStatus.ASSIGNED);
        }
        sr.setUpdatedAt(Instant.now());
        srRepository.update(sr);

        recordTimeline(sr.getId(), "ASSIGNED", "Request Assigned",
                "Assigned to team [" + sr.getAssignedTeam() + "]", principal.getId());

        auditService.logAudit(principal.getId(), "SR_ASSIGNED", "ServiceRequest", sr.getId(),
                "Assigned request to " + assignedTo + " (" + assignedTeam + ")");

        return getServiceRequestById(id, principal);
    }

    @Transactional
    public ServiceRequestDto updateStatus(String id, String status, String reason, UserPrincipal principal) {
        requireSupportOrAdmin(principal);

        ServiceRequest sr = srRepository.findById(id)
                .orElseThrow(() -> new AppException("Service request not found.", HttpStatus.NOT_FOUND));

        sr.setStatus(status);
        sr.setUpdatedAt(Instant.now());
        srRepository.update(sr);

        recordTimeline(sr.getId(), "STATUS_CHANGED", "Status changed to " + status,
                reason != null ? reason : "Status updated by " + principal.getName(), principal.getId());

        auditService.logAudit(principal.getId(), "SR_STATUS_CHANGED", "ServiceRequest", sr.getId(),
                "Status updated to " + status);

        return getServiceRequestById(id, principal);
    }

    @Transactional
    public ServiceRequestDto fulfill(String id, Object fulfillmentDetails, String resolutionNotes, UserPrincipal principal) {
        requireSupportOrAdmin(principal);

        ServiceRequest sr = srRepository.findById(id)
                .orElseThrow(() -> new AppException("Service request not found.", HttpStatus.NOT_FOUND));

        Instant now = Instant.now();
        sr.setStatus(ServiceRequestStatus.FULFILLED);
        sr.setFulfilledAt(now);
        sr.setResolutionNotes(resolutionNotes);

        if (fulfillmentDetails != null) {
            try {
                sr.setFulfillmentDetails(objectMapper.writeValueAsString(fulfillmentDetails));
            } catch (JsonProcessingException e) {
                sr.setFulfillmentDetails(fulfillmentDetails.toString());
            }
        }
        sr.setUpdatedAt(now);
        srRepository.update(sr);

        recordTimeline(sr.getId(), "FULFILLED", "Service Request Fulfilled",
                "Fulfillment complete. Notes: " + resolutionNotes, principal.getId());

        auditService.logAudit(principal.getId(), "SR_FULFILLED", "ServiceRequest", sr.getId(),
                "Fulfilled request with notes: " + resolutionNotes);

        return getServiceRequestById(id, principal);
    }

    @Transactional
    public ServiceRequestDto confirm(String id, String feedbackNotes, UserPrincipal principal) {
        ServiceRequest sr = srRepository.findById(id)
                .orElseThrow(() -> new AppException("Service request not found.", HttpStatus.NOT_FOUND));

        Instant now = Instant.now();
        sr.setStatus(ServiceRequestStatus.CLOSED);
        sr.setClosedAt(now);
        sr.setUpdatedAt(now);
        srRepository.update(sr);

        recordTimeline(sr.getId(), "CONFIRMED", "Service Delivery Confirmed",
                feedbackNotes != null ? feedbackNotes : "Delivery confirmed by requester.", principal.getId());

        auditService.logAudit(principal.getId(), "SR_CONFIRMED", "ServiceRequest", sr.getId(), "Confirmed delivery");

        return getServiceRequestById(id, principal);
    }

    @Transactional
    public ServiceRequestDto reportProblem(String id, String problemDescription, UserPrincipal principal) {
        ServiceRequest sr = srRepository.findById(id)
                .orElseThrow(() -> new AppException("Service request not found.", HttpStatus.NOT_FOUND));

        sr.setStatus(ServiceRequestStatus.IN_PROGRESS);
        sr.setFulfilledAt(null);
        sr.setUpdatedAt(Instant.now());
        srRepository.update(sr);

        recordTimeline(sr.getId(), "PROBLEM_REPORTED", "Issue Reported After Delivery",
                problemDescription, principal.getId());

        auditService.logAudit(principal.getId(), "SR_PROBLEM_REPORTED", "ServiceRequest", sr.getId(),
                "Problem reported: " + problemDescription);

        return getServiceRequestById(id, principal);
    }

    @Transactional
    public ServiceRequestDto cancel(String id, String reason, UserPrincipal principal) {
        ServiceRequest sr = srRepository.findById(id)
                .orElseThrow(() -> new AppException("Service request not found.", HttpStatus.NOT_FOUND));

        sr.setStatus(ServiceRequestStatus.CANCELLED);
        sr.setUpdatedAt(Instant.now());
        srRepository.update(sr);

        recordTimeline(sr.getId(), "CANCELLED", "Request Cancelled",
                reason != null ? reason : "Cancelled by user.", principal.getId());

        auditService.logAudit(principal.getId(), "SR_CANCELLED", "ServiceRequest", sr.getId(), "Cancelled request");

        return getServiceRequestById(id, principal);
    }

    @Transactional
    public ServiceRequestCommentDto addComment(String id, String comment, boolean isInternal, UserPrincipal principal) {
        srRepository.findById(id)
                .orElseThrow(() -> new AppException("Service request not found.", HttpStatus.NOT_FOUND));

        if (isInternal && !UserRole.SUPPORT.equals(principal.getRole()) && !UserRole.ADMIN.equals(principal.getRole())) {
            isInternal = false;
        }

        Instant now = Instant.now();
        ServiceRequestComment c = new ServiceRequestComment();
        c.setId(UUID.randomUUID().toString());
        c.setRequestId(id);
        c.setUserId(principal.getId());
        c.setComment(comment);
        c.setInternal(isInternal);
        c.setCreatedAt(now);
        srRepository.saveComment(c);

        recordTimeline(id, "COMMENT_ADDED", isInternal ? "Internal Note Added" : "Comment Added",
                principal.getName() + ": " + comment, principal.getId());

        ServiceRequestCommentDto dto = new ServiceRequestCommentDto();
        dto.setId(c.getId());
        dto.setRequestId(id);
        dto.setUserId(principal.getId());
        dto.setUserName(principal.getName());
        dto.setUserRole(principal.getRole());
        dto.setComment(comment);
        dto.setInternal(isInternal);
        dto.setCreatedAt(now.toString());
        return dto;
    }

    public ServiceRequestDashboardSummaryDto getDashboardStats() {
        List<ServiceRequest> all = srRepository.getAllRequestsForMetrics();
        int total = all.size();
        int pending = (int) all.stream().filter(s -> ServiceRequestStatus.PENDING_APPROVAL.equals(s.getStatus())).count();
        int unassigned = (int) all.stream().filter(s -> s.getAssignedTo() == null || s.getAssignedTo().isBlank()).count();
        int inProgress = (int) all.stream().filter(s ->
                ServiceRequestStatus.IN_PROGRESS.equals(s.getStatus()) ||
                ServiceRequestStatus.ASSIGNED.equals(s.getStatus()) ||
                ServiceRequestStatus.WAITING_FOR_USER.equals(s.getStatus())).count();
        int fulfilled = (int) all.stream().filter(s -> ServiceRequestStatus.FULFILLED.equals(s.getStatus())).count();
        int closed = (int) all.stream().filter(s -> ServiceRequestStatus.CLOSED.equals(s.getStatus())).count();
        int breached = 0;
        int atRisk = 0;

        Map<String, Integer> byCategory = new HashMap<>();
        Map<String, Integer> byStatus = new HashMap<>();
        Map<String, Integer> byType = new HashMap<>();
        Map<String, Integer> byTeam = new HashMap<>();

        for (ServiceRequest s : all) {
            String sla = slaService.calculateSrSlaStatus(s);
            if ("Breached".equalsIgnoreCase(sla)) {
                breached++;
            } else if ("At Risk".equalsIgnoreCase(sla)) {
                atRisk++;
            }

            String cat = s.getCategory() != null ? s.getCategory() : "General";
            byCategory.put(cat, byCategory.getOrDefault(cat, 0) + 1);

            String st = s.getStatus() != null ? s.getStatus() : "Submitted";
            byStatus.put(st, byStatus.getOrDefault(st, 0) + 1);

            String tp = s.getRequestTypeName() != null ? s.getRequestTypeName() : "General Request";
            byType.put(tp, byType.getOrDefault(tp, 0) + 1);

            String tm = s.getAssignedTeam() != null ? s.getAssignedTeam() : "IT Support";
            byTeam.put(tm, byTeam.getOrDefault(tm, 0) + 1);
        }

        double complianceRate = total > 0 ? Math.round(((double) (total - breached) / total) * 1000.0) / 10.0 : 100.0;

        ServiceRequestDashboardSummaryDto dto = new ServiceRequestDashboardSummaryDto(total, pending, inProgress, fulfilled, closed, breached);
        dto.setUnassigned(unassigned);
        dto.setSlaAtRisk(atRisk);
        dto.setSlaComplianceRate(complianceRate);
        dto.setAverageFulfillmentHours(18.5);
        dto.setByCategory(byCategory);
        dto.setByStatus(byStatus);
        dto.setByType(byType);
        dto.setByTeam(byTeam);
        return dto;
    }

    public List<UnifiedWorkItemDto> getUnifiedWork(String status, String priority, String search, UserPrincipal principal) {
        List<UnifiedWorkItemDto> workItems = new ArrayList<>();

        // Add tickets
        List<Ticket> tickets = ticketRepository.findAll(status, priority, null, null, null, search, 1, 100);
        for (Ticket t : tickets) {
            UnifiedWorkItemDto item = new UnifiedWorkItemDto();
            item.setId(t.getId());
            item.setType("Incident");
            item.setItemNumber(t.getTicketNumber());
            item.setTitle(t.getTitle());
            item.setStatus(t.getStatus());
            item.setPriority(t.getPriority());
            item.setCategory(t.getCategoryName() != null ? t.getCategoryName() : "Incident");
            item.setRequesterName(t.getCreatorName());
            item.setAssigneeName(t.getAssigneeName());
            item.setCreatedAt(t.getCreatedAt() != null ? t.getCreatedAt().toString() : null);
            item.setDueAt(t.getDueAt() != null ? t.getDueAt().toString() : null);
            item.setSlaStatus(slaService.calculateSlaStatus(t));
            workItems.add(item);
        }

        // Add service requests
        List<ServiceRequest> srs = srRepository.findAll(status, priority, null, null, null, search, 1, 100);
        for (ServiceRequest s : srs) {
            UnifiedWorkItemDto item = new UnifiedWorkItemDto();
            item.setId(s.getId());
            item.setType("Service Request");
            item.setItemNumber(s.getRequestNumber());
            item.setTitle(s.getTitle());
            item.setStatus(s.getStatus());
            item.setPriority(s.getPriority());
            item.setCategory(s.getCategory());
            item.setRequesterName(s.getRequesterName());
            item.setAssigneeName(s.getAssigneeName());
            item.setCreatedAt(s.getCreatedAt() != null ? s.getCreatedAt().toString() : null);
            item.setDueAt(s.getDueAt() != null ? s.getDueAt().toString() : null);
            item.setSlaStatus(slaService.calculateSrSlaStatus(s));
            workItems.add(item);
        }

        // Sort descending by created_at
        workItems.sort((a, b) -> {
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        return workItems;
    }

    private void recordTimeline(String requestId, String eventType, String title, String description, String actorId) {
        ServiceRequestTimeline t = new ServiceRequestTimeline();
        t.setId(UUID.randomUUID().toString());
        t.setRequestId(requestId);
        t.setEventType(eventType);
        t.setTitle(title);
        t.setDescription(description);
        t.setActorId(actorId);
        t.setCreatedAt(Instant.now());
        srRepository.saveTimeline(t);
    }

    private void requireSupportOrAdmin(UserPrincipal principal) {
        if (principal == null || (!UserRole.SUPPORT.equals(principal.getRole()) && !UserRole.ADMIN.equals(principal.getRole()))) {
            throw new AppException("Access forbidden: requires SUPPORT or ADMIN role.", HttpStatus.FORBIDDEN);
        }
    }

    private void requireAdmin(UserPrincipal principal) {
        if (principal == null || !UserRole.ADMIN.equals(principal.getRole())) {
            throw new AppException("Access forbidden: requires ADMIN role.", HttpStatus.FORBIDDEN);
        }
    }
}
