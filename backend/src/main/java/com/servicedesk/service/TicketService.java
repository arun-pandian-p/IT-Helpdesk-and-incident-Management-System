package com.servicedesk.service;

import com.servicedesk.dto.TicketDtos.*;
import com.servicedesk.exception.AppException;
import com.servicedesk.model.Asset;
import com.servicedesk.model.Constants.TicketPriority;
import com.servicedesk.model.Constants.TicketStatus;
import com.servicedesk.model.Constants.UserRole;
import com.servicedesk.model.Ticket;
import com.servicedesk.model.TicketComment;
import com.servicedesk.model.TicketEscalation;
import com.servicedesk.model.TicketFeedback;
import com.servicedesk.model.TicketStatusHistory;
import com.servicedesk.model.User;
import com.servicedesk.repository.AssetRepository;
import com.servicedesk.repository.CategoryRepository;
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
public class TicketService {

    private final TicketRepository ticketRepository;
    private final CategoryRepository categoryRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final SlaService slaService;
    private final AuditService auditService;

    private static final Map<String, List<String>> ALLOWED_TRANSITIONS = new HashMap<>();

    static {
        ALLOWED_TRANSITIONS.put(TicketStatus.OPEN, List.of(
                TicketStatus.IN_PROGRESS, TicketStatus.WAITING_FOR_USER, TicketStatus.WAITING_FOR_VENDOR,
                TicketStatus.ESCALATED, TicketStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(TicketStatus.IN_PROGRESS, List.of(
                TicketStatus.WAITING_FOR_USER, TicketStatus.WAITING_FOR_VENDOR, TicketStatus.ESCALATED,
                TicketStatus.RESOLVED, TicketStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(TicketStatus.WAITING_FOR_USER, List.of(
                TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED, TicketStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(TicketStatus.WAITING_FOR_VENDOR, List.of(
                TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED, TicketStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(TicketStatus.ESCALATED, List.of(
                TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED, TicketStatus.CLOSED));
        ALLOWED_TRANSITIONS.put(TicketStatus.RESOLVED, List.of(
                TicketStatus.CLOSED, TicketStatus.IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(TicketStatus.CLOSED, List.of(
                TicketStatus.IN_PROGRESS));
    }

    public TicketService(TicketRepository ticketRepository,
                         CategoryRepository categoryRepository,
                         AssetRepository assetRepository,
                         UserRepository userRepository,
                         SlaService slaService,
                         AuditService auditService) {
        this.ticketRepository = ticketRepository;
        this.categoryRepository = categoryRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.slaService = slaService;
        this.auditService = auditService;
    }

    public synchronized String generateTicketNumber() {
        int currentYear = Year.now().getValue();
        String prefix = String.format("INC-%d-", currentYear);
        String lastNumber = ticketRepository.findHighestTicketNumberForYear(currentYear);

        int nextSeq = 1;
        if (lastNumber != null) {
            Pattern p = Pattern.compile("^INC-\\d{4}-(\\d{6})$");
            Matcher m = p.matcher(lastNumber);
            if (m.find()) {
                try {
                    nextSeq = Integer.parseInt(m.group(1)) + 1;
                } catch (NumberFormatException ignored) {}
            }
        }
        return String.format("%s%06d", prefix, nextSeq);
    }

    @Transactional
    public TicketDto createTicket(TicketCreateRequest req, UserPrincipal principal) {
        categoryRepository.findById(req.getCategoryId())
                .orElseThrow(() -> new AppException("Selected category not found.", HttpStatus.BAD_REQUEST));

        String priority = slaService.calculateSuggestedPriority(req.getImpact(), req.getUrgency());
        int targetHours = slaService.getTargetHours(priority);
        Instant now = Instant.now();
        Instant dueAt = now.plus(targetHours, ChronoUnit.HOURS);

        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID().toString());
        ticket.setTicketNumber(generateTicketNumber());
        ticket.setTitle(req.getTitle());
        ticket.setDescription(req.getDescription());
        ticket.setCategoryId(req.getCategoryId());
        ticket.setPriority(priority);
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setImpact(req.getImpact());
        ticket.setUrgency(req.getUrgency());
        ticket.setAssetId(req.getAssetId());
        ticket.setDeviceType(req.getDeviceType());
        ticket.setOsName(req.getOsName());
        ticket.setApplicationName(req.getApplicationName());
        ticket.setLocation(req.getLocation());
        ticket.setCreatedBy(principal.getId());
        ticket.setDueAt(dueAt);
        ticket.setEscalated(false);
        ticket.setEscalationLevel(1);
        ticket.setCreatedAt(now);
        ticket.setUpdatedAt(now);

        ticketRepository.save(ticket);

        // Record Initial Status History
        TicketStatusHistory hist = new TicketStatusHistory();
        hist.setId(UUID.randomUUID().toString());
        hist.setTicketId(ticket.getId());
        hist.setOldStatus("None");
        hist.setNewStatus(TicketStatus.OPEN);
        hist.setChangedBy(principal.getId());
        hist.setChangedAt(now);
        hist.setReason("Ticket created");
        ticketRepository.saveStatusHistory(hist);

        auditService.logAudit(principal.getId(), "TICKET_CREATED", "Ticket", ticket.getId(),
                "Created incident " + ticket.getTicketNumber() + ": " + ticket.getTitle());

        return getTicketById(ticket.getId(), principal);
    }

    public TicketListResponse getTickets(String status, String priority, String categoryId,
                                        String assignedTo, String createdBy, String search,
                                        int page, int pageSize, UserPrincipal principal) {
        if (page < 1) page = 1;
        if (pageSize < 1) pageSize = 20;

        List<Ticket> tickets = ticketRepository.findAll(status, priority, categoryId, assignedTo, createdBy, search, page, pageSize);
        int total = ticketRepository.count(status, priority, categoryId, assignedTo, createdBy, search);

        List<TicketDto> dtos = tickets.stream().map(t -> {
            t.setSlaStatus(slaService.calculateSlaStatus(t));
            TicketDto dto = TicketDto.fromEntity(t);
            enrichAsset(dto, t.getAssetId());
            return dto;
        }).collect(Collectors.toList());

        return new TicketListResponse(dtos, total, page, pageSize);
    }

    public TicketDto getTicketById(String id, UserPrincipal principal) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new AppException("Ticket not found.", HttpStatus.NOT_FOUND));

        ticket.setSlaStatus(slaService.calculateSlaStatus(ticket));
        TicketDto dto = TicketDto.fromEntity(ticket);
        enrichAsset(dto, ticket.getAssetId());

        // Comments (filter internal if Employee)
        boolean isSupportOrAdmin = principal != null &&
                (UserRole.SUPPORT.equals(principal.getRole()) || UserRole.ADMIN.equals(principal.getRole()));

        List<TicketComment> comments = ticketRepository.findCommentsByTicketId(id);
        List<TicketCommentDto> commentDtos = comments.stream()
                .filter(c -> !c.isInternal() || isSupportOrAdmin)
                .map(c -> {
                    TicketCommentDto cd = new TicketCommentDto();
                    cd.setId(c.getId());
                    cd.setTicketId(c.getTicketId());
                    cd.setUserId(c.getUserId());
                    cd.setUserName(c.getUserName() != null ? c.getUserName() : "Unknown");
                    cd.setUserRole(c.getUserRole() != null ? c.getUserRole() : UserRole.EMPLOYEE);
                    cd.setComment(c.getComment());
                    cd.setInternal(c.isInternal());
                    cd.setCreatedAt(c.getCreatedAt() != null ? c.getCreatedAt().toString() : null);
                    return cd;
                }).collect(Collectors.toList());
        dto.setComments(commentDtos);

        // Status History
        List<TicketStatusHistory> history = ticketRepository.findStatusHistoryByTicketId(id);
        List<TicketStatusHistoryDto> historyDtos = history.stream().map(h -> {
            TicketStatusHistoryDto hd = new TicketStatusHistoryDto();
            hd.setId(h.getId());
            hd.setOldStatus(h.getOldStatus());
            hd.setNewStatus(h.getNewStatus());
            hd.setChangedByName(h.getChangedByName() != null ? h.getChangedByName() : "System");
            hd.setChangedAt(h.getChangedAt() != null ? h.getChangedAt().toString() : null);
            hd.setReason(h.getReason());
            return hd;
        }).collect(Collectors.toList());
        dto.setStatusHistory(historyDtos);

        // Escalations
        List<TicketEscalation> escalations = ticketRepository.findEscalationsByTicketId(id);
        List<TicketEscalationDto> escDtos = escalations.stream().map(e -> {
            TicketEscalationDto ed = new TicketEscalationDto();
            ed.setId(e.getId());
            ed.setFromEngineerName(e.getFromEngineerName() != null ? e.getFromEngineerName() : "Support Engineer");
            ed.setToEngineerName(e.getToEngineerName() != null ? e.getToEngineerName() : "Senior Support");
            ed.setReason(e.getReason());
            ed.setEscalationLevel(e.getEscalationLevel());
            ed.setCreatedAt(e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
            ed.setResolvedAt(e.getResolvedAt() != null ? e.getResolvedAt().toString() : null);
            return ed;
        }).collect(Collectors.toList());
        dto.setEscalations(escDtos);

        // Feedback
        ticketRepository.findFeedbackByTicketId(id).ifPresent(f -> {
            TicketFeedbackDto fd = new TicketFeedbackDto();
            fd.setId(f.getId());
            fd.setRating(f.getRating());
            fd.setComments(f.getComments());
            fd.setCreatedAt(f.getCreatedAt() != null ? f.getCreatedAt().toString() : null);
            dto.setFeedback(fd);
        });

        return dto;
    }

    private void enrichAsset(TicketDto dto, String assetId) {
        if (assetId != null && !assetId.isBlank()) {
            assetRepository.findById(assetId).ifPresent(dto::setAsset);
        }
    }

    @Transactional
    public TicketDto assignTicket(String id, String assignedTo, UserPrincipal principal) {
        requireSupportOrAdmin(principal);

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new AppException("Ticket not found.", HttpStatus.NOT_FOUND));

        User assignee = userRepository.findById(assignedTo)
                .orElseThrow(() -> new AppException("Assignee user not found.", HttpStatus.NOT_FOUND));

        ticket.setAssignedTo(assignee.getId());
        Instant now = Instant.now();

        // If ticket was Open, transition to In Progress
        if (TicketStatus.OPEN.equals(ticket.getStatus())) {
            ticket.setStatus(TicketStatus.IN_PROGRESS);
            if (ticket.getFirstResponseAt() == null) {
                ticket.setFirstResponseAt(now);
            }
            recordStatusHistory(ticket.getId(), TicketStatus.OPEN, TicketStatus.IN_PROGRESS,
                    principal.getId(), "Assigned to " + assignee.getName());
        }

        ticket.setUpdatedAt(now);
        ticketRepository.update(ticket);

        auditService.logAudit(principal.getId(), "TICKET_ASSIGNED", "Ticket", ticket.getId(),
                "Assigned to " + assignee.getName());

        return getTicketById(id, principal);
    }

    @Transactional
    public TicketDto updateStatus(String id, String newStatus, String reason, UserPrincipal principal) {
        requireSupportOrAdmin(principal);

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new AppException("Ticket not found.", HttpStatus.NOT_FOUND));

        validateTransition(ticket.getStatus(), newStatus);

        String oldStatus = ticket.getStatus();
        ticket.setStatus(newStatus);
        Instant now = Instant.now();
        ticket.setUpdatedAt(now);

        if (TicketStatus.IN_PROGRESS.equals(newStatus) && ticket.getFirstResponseAt() == null) {
            ticket.setFirstResponseAt(now);
        }

        ticketRepository.update(ticket);
        recordStatusHistory(ticket.getId(), oldStatus, newStatus, principal.getId(), reason);

        auditService.logAudit(principal.getId(), "TICKET_STATUS_CHANGED", "Ticket", ticket.getId(),
                "Status changed from " + oldStatus + " to " + newStatus);

        return getTicketById(id, principal);
    }

    @Transactional
    public TicketDto overridePriority(String id, String priority, String reason, UserPrincipal principal) {
        requireSupportOrAdmin(principal);

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new AppException("Ticket not found.", HttpStatus.NOT_FOUND));

        ticket.setPriority(priority);
        ticket.setPriorityOverrideReason(reason);

        // Recalculate SLA due date based on new priority
        int targetHours = slaService.getTargetHours(priority);
        ticket.setDueAt(ticket.getCreatedAt().plus(targetHours, ChronoUnit.HOURS));
        ticket.setUpdatedAt(Instant.now());

        ticketRepository.update(ticket);

        auditService.logAudit(principal.getId(), "TICKET_PRIORITY_OVERRIDDEN", "Ticket", ticket.getId(),
                "Priority changed to " + priority + ". Reason: " + reason);

        return getTicketById(id, principal);
    }

    @Transactional
    public TicketDto escalateTicket(String id, int level, String reason, String toEngineer, UserPrincipal principal) {
        requireSupportOrAdmin(principal);

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new AppException("Ticket not found.", HttpStatus.NOT_FOUND));

        Instant now = Instant.now();
        ticket.setEscalated(true);
        ticket.setEscalationLevel(level);
        ticket.setStatus(TicketStatus.ESCALATED);
        if (toEngineer != null && !toEngineer.isBlank()) {
            ticket.setAssignedTo(toEngineer);
        }
        ticket.setUpdatedAt(now);
        ticketRepository.update(ticket);

        TicketEscalation esc = new TicketEscalation();
        esc.setId(UUID.randomUUID().toString());
        esc.setTicketId(ticket.getId());
        esc.setFromEngineer(principal.getId());
        esc.setToEngineer(toEngineer);
        esc.setReason(reason);
        esc.setEscalationLevel(level);
        esc.setCreatedAt(now);
        ticketRepository.saveEscalation(esc);

        recordStatusHistory(ticket.getId(), ticket.getStatus(), TicketStatus.ESCALATED, principal.getId(),
                "Escalated to level " + level + ": " + reason);

        auditService.logAudit(principal.getId(), "TICKET_ESCALATED", "Ticket", ticket.getId(),
                "Escalated to level " + level);

        return getTicketById(id, principal);
    }

    @Transactional
    public TicketDto resolveTicket(String id, String resolutionNotes, UserPrincipal principal) {
        requireSupportOrAdmin(principal);

        if (resolutionNotes == null || resolutionNotes.trim().isEmpty()) {
            throw new AppException("resolution_notes is required to resolve an incident.", HttpStatus.UNPROCESSABLE_ENTITY);
        }

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new AppException("Ticket not found.", HttpStatus.NOT_FOUND));

        validateTransition(ticket.getStatus(), TicketStatus.RESOLVED);

        Instant now = Instant.now();
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(now);
        ticket.setResolutionNotes(resolutionNotes);
        ticket.setUpdatedAt(now);
        ticketRepository.update(ticket);

        recordStatusHistory(ticket.getId(), ticket.getStatus(), TicketStatus.RESOLVED, principal.getId(),
                "Incident resolved: " + resolutionNotes);

        auditService.logAudit(principal.getId(), "TICKET_RESOLVED", "Ticket", ticket.getId(),
                "Resolved incident with notes: " + resolutionNotes);

        return getTicketById(id, principal);
    }

    @Transactional
    public TicketDto closeTicket(String id, UserPrincipal principal) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new AppException("Ticket not found.", HttpStatus.NOT_FOUND));

        Instant now = Instant.now();
        ticket.setStatus(TicketStatus.CLOSED);
        ticket.setClosedAt(now);
        ticket.setUpdatedAt(now);
        ticketRepository.update(ticket);

        recordStatusHistory(ticket.getId(), ticket.getStatus(), TicketStatus.CLOSED, principal.getId(), "Ticket closed");
        auditService.logAudit(principal.getId(), "TICKET_CLOSED", "Ticket", ticket.getId(), "Closed ticket");

        return getTicketById(id, principal);
    }

    @Transactional
    public TicketDto reopenTicket(String id, UserPrincipal principal) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new AppException("Ticket not found.", HttpStatus.NOT_FOUND));

        if (!TicketStatus.RESOLVED.equals(ticket.getStatus()) && !TicketStatus.CLOSED.equals(ticket.getStatus())) {
            throw new AppException("Only resolved or closed tickets can be reopened.", HttpStatus.BAD_REQUEST);
        }

        Instant now = Instant.now();
        String oldStatus = ticket.getStatus();
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        ticket.setResolvedAt(null);
        ticket.setClosedAt(null);
        ticket.setUpdatedAt(now);
        ticketRepository.update(ticket);

        recordStatusHistory(ticket.getId(), oldStatus, TicketStatus.IN_PROGRESS, principal.getId(), "Ticket reopened by user");
        auditService.logAudit(principal.getId(), "TICKET_REOPENED", "Ticket", ticket.getId(), "Reopened ticket");

        return getTicketById(id, principal);
    }

    @Transactional
    public TicketCommentDto addComment(String id, String comment, boolean isInternal, UserPrincipal principal) {
        ticketRepository.findById(id)
                .orElseThrow(() -> new AppException("Ticket not found.", HttpStatus.NOT_FOUND));

        // Employee cannot post internal comment
        if (isInternal && !UserRole.SUPPORT.equals(principal.getRole()) && !UserRole.ADMIN.equals(principal.getRole())) {
            isInternal = false;
        }

        Instant now = Instant.now();
        TicketComment c = new TicketComment();
        c.setId(UUID.randomUUID().toString());
        c.setTicketId(id);
        c.setUserId(principal.getId());
        c.setComment(comment);
        c.setInternal(isInternal);
        c.setCreatedAt(now);

        ticketRepository.saveComment(c);

        TicketCommentDto dto = new TicketCommentDto();
        dto.setId(c.getId());
        dto.setTicketId(id);
        dto.setUserId(principal.getId());
        dto.setUserName(principal.getName());
        dto.setUserRole(principal.getRole());
        dto.setComment(comment);
        dto.setInternal(isInternal);
        dto.setCreatedAt(now.toString());
        return dto;
    }

    @Transactional
    public TicketDto updateChecklist(String id, String checklistState, UserPrincipal principal) {
        requireSupportOrAdmin(principal);

        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new AppException("Ticket not found.", HttpStatus.NOT_FOUND));

        ticket.setChecklistState(checklistState);
        ticket.setUpdatedAt(Instant.now());
        ticketRepository.update(ticket);

        return getTicketById(id, principal);
    }

    @Transactional
    public TicketFeedbackDto submitFeedback(String id, int rating, String comments, UserPrincipal principal) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new AppException("Ticket not found.", HttpStatus.NOT_FOUND));

        if (rating < 1 || rating > 5) {
            throw new AppException("Rating must be between 1 and 5 stars.", HttpStatus.BAD_REQUEST);
        }

        if (ticketRepository.findFeedbackByTicketId(id).isPresent()) {
            throw new AppException("Feedback has already been submitted for this incident.", HttpStatus.BAD_REQUEST);
        }

        Instant now = Instant.now();
        TicketFeedback f = new TicketFeedback();
        f.setId(UUID.randomUUID().toString());
        f.setTicketId(ticket.getId());
        f.setUserId(principal.getId());
        f.setRating(rating);
        f.setComments(comments);
        f.setCreatedAt(now);

        ticketRepository.saveFeedback(f);

        TicketFeedbackDto dto = new TicketFeedbackDto();
        dto.setId(f.getId());
        dto.setRating(rating);
        dto.setComments(comments);
        dto.setCreatedAt(now.toString());
        return dto;
    }

    private void validateTransition(String currentStatus, String targetStatus) {
        if (currentStatus.equals(targetStatus)) return;
        List<String> allowed = ALLOWED_TRANSITIONS.get(currentStatus);
        if (allowed == null || !allowed.contains(targetStatus)) {
            throw new AppException(
                    String.format("Invalid status transition from '%s' to '%s'.", currentStatus, targetStatus),
                    HttpStatus.BAD_REQUEST);
        }
    }

    private void recordStatusHistory(String ticketId, String oldStatus, String newStatus, String changedBy, String reason) {
        TicketStatusHistory hist = new TicketStatusHistory();
        hist.setId(UUID.randomUUID().toString());
        hist.setTicketId(ticketId);
        hist.setOldStatus(oldStatus);
        hist.setNewStatus(newStatus);
        hist.setChangedBy(changedBy);
        hist.setChangedAt(Instant.now());
        hist.setReason(reason);
        ticketRepository.saveStatusHistory(hist);
    }

    private void requireSupportOrAdmin(UserPrincipal principal) {
        if (principal == null || (!UserRole.SUPPORT.equals(principal.getRole()) && !UserRole.ADMIN.equals(principal.getRole()))) {
            throw new AppException("Access forbidden: requires SUPPORT or ADMIN role.", HttpStatus.FORBIDDEN);
        }
    }
}
