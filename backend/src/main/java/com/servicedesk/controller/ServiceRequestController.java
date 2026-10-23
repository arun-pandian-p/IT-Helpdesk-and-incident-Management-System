package com.servicedesk.controller;

import com.servicedesk.dto.ServiceRequestDtos.*;
import com.servicedesk.model.ServiceRequestType;
import com.servicedesk.security.UserPrincipal;
import com.servicedesk.service.ServiceRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ServiceRequestController {

    private final ServiceRequestService serviceRequestService;

    public ServiceRequestController(ServiceRequestService serviceRequestService) {
        this.serviceRequestService = serviceRequestService;
    }

    // -------------------------------------------------------------------------
    // Types (Catalog)
    // -------------------------------------------------------------------------
    @GetMapping("/service-request-types")
    public ResponseEntity<List<ServiceRequestType>> getRequestTypes(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(serviceRequestService.getRequestTypes(category));
    }

    @PostMapping("/service-request-types")
    public ResponseEntity<ServiceRequestType> createRequestType(
            @Valid @RequestBody ServiceRequestTypeCreateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        ServiceRequestType created = serviceRequestService.createRequestType(req, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/service-request-types/{id}")
    public ResponseEntity<ServiceRequestType> updateRequestType(
            @PathVariable String id,
            @RequestBody ServiceRequestTypeUpdateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(serviceRequestService.updateRequestType(id, req, principal));
    }

    // -------------------------------------------------------------------------
    // Service Requests
    // -------------------------------------------------------------------------
    @GetMapping("/service-requests")
    public ResponseEntity<ServiceRequestListResponse> getServiceRequests(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String category,
            @RequestParam(name = "assigned_to", required = false) String assignedTo,
            @RequestParam(name = "requester_id", required = false) String requesterId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @AuthenticationPrincipal UserPrincipal principal) {
        ServiceRequestListResponse res = serviceRequestService.getServiceRequests(status, priority, category, assignedTo, requesterId, search, page, pageSize, principal);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/service-requests")
    public ResponseEntity<ServiceRequestDto> createServiceRequest(
            @Valid @RequestBody ServiceRequestCreateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        ServiceRequestDto sr = serviceRequestService.createServiceRequest(req, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(sr);
    }

    @GetMapping("/service-requests/{id}")
    public ResponseEntity<ServiceRequestDto> getServiceRequest(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ServiceRequestDto sr = serviceRequestService.getServiceRequestById(id, principal);
        return ResponseEntity.ok(sr);
    }

    @PostMapping("/service-requests/{id}/approve")
    public ResponseEntity<ServiceRequestDto> approve(
            @PathVariable String id,
            @RequestBody(required = false) ServiceRequestApproveRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean approved = req == null || req.isApproved();
        String reason = req != null ? req.getReason() : null;
        return ResponseEntity.ok(serviceRequestService.approve(id, approved, reason, principal));
    }

    @PostMapping("/service-requests/{id}/reject")
    public ResponseEntity<ServiceRequestDto> reject(
            @PathVariable String id,
            @Valid @RequestBody ServiceRequestRejectRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(serviceRequestService.reject(id, req.getReason(), principal));
    }

    @PostMapping("/service-requests/{id}/assign")
    public ResponseEntity<ServiceRequestDto> assign(
            @PathVariable String id,
            @RequestBody ServiceRequestAssignRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(serviceRequestService.assign(id, req.getAssignedTo(), req.getAssignedTeam(), principal));
    }

    @PostMapping("/service-requests/{id}/status")
    public ResponseEntity<ServiceRequestDto> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody ServiceRequestStatusRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(serviceRequestService.updateStatus(id, req.getStatus(), req.getReason(), principal));
    }

    @PostMapping("/service-requests/{id}/fulfill")
    public ResponseEntity<ServiceRequestDto> fulfill(
            @PathVariable String id,
            @Valid @RequestBody ServiceRequestFulfillRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(serviceRequestService.fulfill(id, req.getFulfillmentDetails(), req.getResolutionNotes(), principal));
    }

    @PostMapping("/service-requests/{id}/confirm")
    public ResponseEntity<ServiceRequestDto> confirm(
            @PathVariable String id,
            @RequestBody(required = false) ServiceRequestConfirmRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        String feedback = req != null ? req.getFeedbackNotes() : null;
        return ResponseEntity.ok(serviceRequestService.confirm(id, feedback, principal));
    }

    @PostMapping("/service-requests/{id}/report-problem")
    public ResponseEntity<ServiceRequestDto> reportProblem(
            @PathVariable String id,
            @Valid @RequestBody ServiceRequestReportProblemRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(serviceRequestService.reportProblem(id, req.getProblemDescription(), principal));
    }

    @PostMapping("/service-requests/{id}/cancel")
    public ResponseEntity<ServiceRequestDto> cancel(
            @PathVariable String id,
            @RequestBody(required = false) ServiceRequestCancelRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        String reason = req != null ? req.getReason() : null;
        return ResponseEntity.ok(serviceRequestService.cancel(id, reason, principal));
    }

    @PostMapping("/service-requests/{id}/comments")
    public ResponseEntity<ServiceRequestCommentDto> addComment(
            @PathVariable String id,
            @Valid @RequestBody ServiceRequestCommentRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        ServiceRequestCommentDto comment = serviceRequestService.addComment(id, req.getComment(), req.isInternal(), principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @GetMapping("/service-requests/dashboard/stats")
    public ResponseEntity<ServiceRequestDashboardSummaryDto> getDashboardStats() {
        return ResponseEntity.ok(serviceRequestService.getDashboardStats());
    }

    @GetMapping("/service-desk/unified")
    public ResponseEntity<List<UnifiedWorkItemDto>> getUnifiedWork(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String search,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(serviceRequestService.getUnifiedWork(status, priority, search, principal));
    }
}
