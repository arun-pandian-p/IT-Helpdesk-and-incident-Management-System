package com.servicedesk.controller;

import com.servicedesk.dto.TicketDtos.*;
import com.servicedesk.security.UserPrincipal;
import com.servicedesk.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public ResponseEntity<TicketListResponse> getTickets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(name = "category_id", required = false) String categoryId,
            @RequestParam(name = "assigned_to", required = false) String assignedTo,
            @RequestParam(name = "created_by", required = false) String createdBy,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(name = "page_size", defaultValue = "20") int pageSize,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketListResponse res = ticketService.getTickets(status, priority, categoryId, assignedTo, createdBy, search, page, pageSize, principal);
        return ResponseEntity.ok(res);
    }

    @PostMapping
    public ResponseEntity<TicketDto> createTicket(
            @Valid @RequestBody TicketCreateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDto ticket = ticketService.createTicket(req, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(ticket);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketDto> getTicket(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDto ticket = ticketService.getTicketById(id, principal);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<TicketDto> assignTicket(
            @PathVariable String id,
            @Valid @RequestBody TicketAssignRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDto ticket = ticketService.assignTicket(id, req.getAssignedTo(), principal);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<TicketDto> updateStatus(
            @PathVariable String id,
            @Valid @RequestBody TicketStatusUpdateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDto ticket = ticketService.updateStatus(id, req.getStatus(), req.getReason(), principal);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/override-priority")
    public ResponseEntity<TicketDto> overridePriority(
            @PathVariable String id,
            @Valid @RequestBody TicketPriorityOverrideRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDto ticket = ticketService.overridePriority(id, req.getPriority(), req.getReason(), principal);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/escalate")
    public ResponseEntity<TicketDto> escalateTicket(
            @PathVariable String id,
            @Valid @RequestBody TicketEscalateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDto ticket = ticketService.escalateTicket(id, req.getEscalationLevel(), req.getReason(), req.getToEngineer(), principal);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<TicketDto> resolveTicket(
            @PathVariable String id,
            @Valid @RequestBody TicketResolveRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDto ticket = ticketService.resolveTicket(id, req.getResolutionNotes(), principal);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<TicketDto> closeTicket(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDto ticket = ticketService.closeTicket(id, principal);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/reopen")
    public ResponseEntity<TicketDto> reopenTicket(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDto ticket = ticketService.reopenTicket(id, principal);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<TicketCommentDto> addComment(
            @PathVariable String id,
            @Valid @RequestBody TicketCommentCreateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketCommentDto comment = ticketService.addComment(id, req.getComment(), req.isInternal(), principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    @PatchMapping("/{id}/checklist")
    public ResponseEntity<TicketDto> updateChecklist(
            @PathVariable String id,
            @RequestBody TicketChecklistUpdateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDto ticket = ticketService.updateChecklist(id, req.getChecklistState(), principal);
        return ResponseEntity.ok(ticket);
    }

    @PostMapping("/{id}/feedback")
    public ResponseEntity<TicketFeedbackDto> submitFeedback(
            @PathVariable String id,
            @RequestBody TicketFeedbackRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketFeedbackDto feedback = ticketService.submitFeedback(id, req.getRating(), req.getComments(), principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(feedback);
    }
}
