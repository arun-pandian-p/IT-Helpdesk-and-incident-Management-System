package com.servicedesk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.servicedesk.model.Asset;
import com.servicedesk.model.Ticket;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.List;

public class TicketDtos {

    public static class TicketCreateRequest {
        @NotBlank(message = "Title is required")
        private String title;

        @NotBlank(message = "Description is required")
        private String description;

        @NotBlank(message = "Category is required")
        @JsonProperty("category_id")
        private String categoryId;

        private String impact = "Individual";
        private String urgency = "Medium";

        @JsonProperty("asset_id")
        private String assetId;

        @JsonProperty("device_type")
        private String deviceType;

        @JsonProperty("os_name")
        private String osName;

        @JsonProperty("application_name")
        private String applicationName;

        private String location;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getCategoryId() { return categoryId; }
        public void setCategoryId(String categoryId) { this.categoryId = categoryId; }

        public String getImpact() { return impact; }
        public void setImpact(String impact) { this.impact = impact; }

        public String getUrgency() { return urgency; }
        public void setUrgency(String urgency) { this.urgency = urgency; }

        public String getAssetId() { return assetId; }
        public void setAssetId(String assetId) { this.assetId = assetId; }

        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

        public String getOsName() { return osName; }
        public void setOsName(String osName) { this.osName = osName; }

        public String getApplicationName() { return applicationName; }
        public void setApplicationName(String applicationName) { this.applicationName = applicationName; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
    }

    public static class TicketAssignRequest {
        @NotBlank(message = "assigned_to is required")
        @JsonProperty("assigned_to")
        private String assignedTo;

        public String getAssignedTo() { return assignedTo; }
        public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    }

    public static class TicketStatusUpdateRequest {
        @NotBlank(message = "status is required")
        private String status;
        private String reason;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class TicketPriorityOverrideRequest {
        @NotBlank(message = "priority is required")
        private String priority;

        @NotBlank(message = "reason is required")
        private String reason;

        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class TicketEscalateRequest {
        @JsonProperty("escalation_level")
        private int escalationLevel = 2;

        @NotBlank(message = "reason is required")
        private String reason;

        @JsonProperty("to_engineer")
        private String toEngineer;

        public int getEscalationLevel() { return escalationLevel; }
        public void setEscalationLevel(int escalationLevel) { this.escalationLevel = escalationLevel; }

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }

        public String getToEngineer() { return toEngineer; }
        public void setToEngineer(String toEngineer) { this.toEngineer = toEngineer; }
    }

    public static class TicketResolveRequest {
        @NotBlank(message = "resolution_notes is required")
        @JsonProperty("resolution_notes")
        private String resolutionNotes;

        public String getResolutionNotes() { return resolutionNotes; }
        public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    }

    public static class TicketCommentCreateRequest {
        @NotBlank(message = "comment is required")
        private String comment;

        @JsonProperty("is_internal")
        private boolean internal = false;

        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }

        public boolean isInternal() { return internal; }
        public void setInternal(boolean internal) { this.internal = internal; }
    }

    public static class TicketChecklistUpdateRequest {
        @JsonProperty("checklist_state")
        private String checklistState;

        public String getChecklistState() { return checklistState; }
        public void setChecklistState(String checklistState) { this.checklistState = checklistState; }
    }

    public static class TicketFeedbackRequest {
        private int rating;
        private String comments;

        public int getRating() { return rating; }
        public void setRating(int rating) { this.rating = rating; }

        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
    }

    public static class TicketCommentDto {
        private String id;
        @JsonProperty("ticket_id")
        private String ticketId;
        @JsonProperty("user_id")
        private String userId;
        @JsonProperty("user_name")
        private String userName;
        @JsonProperty("user_role")
        private String userRole;
        private String comment;
        @JsonProperty("is_internal")
        private boolean internal;
        @JsonProperty("created_at")
        private String createdAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTicketId() { return ticketId; }
        public void setTicketId(String ticketId) { this.ticketId = ticketId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public String getUserRole() { return userRole; }
        public void setUserRole(String userRole) { this.userRole = userRole; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public boolean isInternal() { return internal; }
        public void setInternal(boolean internal) { this.internal = internal; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    public static class TicketStatusHistoryDto {
        private String id;
        @JsonProperty("old_status")
        private String oldStatus;
        @JsonProperty("new_status")
        private String newStatus;
        @JsonProperty("changed_by_name")
        private String changedByName;
        @JsonProperty("changed_at")
        private String changedAt;
        private String reason;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getOldStatus() { return oldStatus; }
        public void setOldStatus(String oldStatus) { this.oldStatus = oldStatus; }
        public String getNewStatus() { return newStatus; }
        public void setNewStatus(String newStatus) { this.newStatus = newStatus; }
        public String getChangedByName() { return changedByName; }
        public void setChangedByName(String changedByName) { this.changedByName = changedByName; }
        public String getChangedAt() { return changedAt; }
        public void setChangedAt(String changedAt) { this.changedAt = changedAt; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class TicketEscalationDto {
        private String id;
        @JsonProperty("from_engineer_name")
        private String fromEngineerName;
        @JsonProperty("to_engineer_name")
        private String toEngineerName;
        private String reason;
        @JsonProperty("escalation_level")
        private int escalationLevel;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("resolved_at")
        private String resolvedAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getFromEngineerName() { return fromEngineerName; }
        public void setFromEngineerName(String fromEngineerName) { this.fromEngineerName = fromEngineerName; }
        public String getToEngineerName() { return toEngineerName; }
        public void setToEngineerName(String toEngineerName) { this.toEngineerName = toEngineerName; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        public int getEscalationLevel() { return escalationLevel; }
        public void setEscalationLevel(int escalationLevel) { this.escalationLevel = escalationLevel; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getResolvedAt() { return resolvedAt; }
        public void setResolvedAt(String resolvedAt) { this.resolvedAt = resolvedAt; }
    }

    public static class TicketFeedbackDto {
        private String id;
        private int rating;
        private String comments;
        @JsonProperty("created_at")
        private String createdAt;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public int getRating() { return rating; }
        public void setRating(int rating) { this.rating = rating; }
        public String getComments() { return comments; }
        public void setComments(String comments) { this.comments = comments; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    public static class TicketDto {
        private String id;
        @JsonProperty("ticket_number")
        private String ticketNumber;
        private String title;
        private String description;
        @JsonProperty("category_id")
        private String categoryId;
        @JsonProperty("category_name")
        private String categoryName;
        private String priority;
        private String status;
        private String impact;
        private String urgency;
        @JsonProperty("priority_override_reason")
        private String priorityOverrideReason;
        @JsonProperty("asset_id")
        private String assetId;
        private Asset asset;
        @JsonProperty("device_type")
        private String deviceType;
        @JsonProperty("os_name")
        private String osName;
        @JsonProperty("application_name")
        private String applicationName;
        private String location;
        @JsonProperty("created_by")
        private String createdBy;
        @JsonProperty("creator_name")
        private String creatorName;
        @JsonProperty("creator_email")
        private String creatorEmail;
        @JsonProperty("creator_department")
        private String creatorDepartment;
        @JsonProperty("assigned_to")
        private String assignedTo;
        @JsonProperty("assignee_name")
        private String assigneeName;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("updated_at")
        private String updatedAt;
        @JsonProperty("due_at")
        private String dueAt;
        @JsonProperty("first_response_at")
        private String firstResponseAt;
        @JsonProperty("resolved_at")
        private String resolvedAt;
        @JsonProperty("closed_at")
        private String closedAt;
        @JsonProperty("resolution_notes")
        private String resolutionNotes;
        @JsonProperty("is_escalated")
        private boolean escalated;
        @JsonProperty("escalation_level")
        private int escalationLevel;
        @JsonProperty("checklist_state")
        private String checklistState;
        @JsonProperty("sla_status")
        private String slaStatus;

        private List<TicketCommentDto> comments = new ArrayList<>();
        @JsonProperty("status_history")
        private List<TicketStatusHistoryDto> statusHistory = new ArrayList<>();
        private List<TicketEscalationDto> escalations = new ArrayList<>();
        private TicketFeedbackDto feedback;

        public static TicketDto fromEntity(Ticket t) {
            if (t == null) return null;
            TicketDto d = new TicketDto();
            d.id = t.getId();
            d.ticketNumber = t.getTicketNumber();
            d.title = t.getTitle();
            d.description = t.getDescription();
            d.categoryId = t.getCategoryId();
            d.categoryName = t.getCategoryName();
            d.priority = t.getPriority();
            d.status = t.getStatus();
            d.impact = t.getImpact();
            d.urgency = t.getUrgency();
            d.priorityOverrideReason = t.getPriorityOverrideReason();
            d.assetId = t.getAssetId();
            d.asset = t.getAsset();
            d.deviceType = t.getDeviceType();
            d.osName = t.getOsName();
            d.applicationName = t.getApplicationName();
            d.location = t.getLocation();
            d.createdBy = t.getCreatedBy();
            d.creatorName = t.getCreatorName();
            d.creatorEmail = t.getCreatorEmail();
            d.creatorDepartment = t.getCreatorDepartment();
            d.assignedTo = t.getAssignedTo();
            d.assigneeName = t.getAssigneeName();
            d.createdAt = t.getCreatedAt() != null ? t.getCreatedAt().toString() : null;
            d.updatedAt = t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : null;
            d.dueAt = t.getDueAt() != null ? t.getDueAt().toString() : null;
            d.firstResponseAt = t.getFirstResponseAt() != null ? t.getFirstResponseAt().toString() : null;
            d.resolvedAt = t.getResolvedAt() != null ? t.getResolvedAt().toString() : null;
            d.closedAt = t.getClosedAt() != null ? t.getClosedAt().toString() : null;
            d.resolutionNotes = t.getResolutionNotes();
            d.escalated = t.isEscalated();
            d.escalationLevel = t.getEscalationLevel();
            d.checklistState = t.getChecklistState();
            d.slaStatus = t.getSlaStatus();
            return d;
        }

        // Getters & Setters
        public String getId() { return id; }
        public String getTicketNumber() { return ticketNumber; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getCategoryId() { return categoryId; }
        public String getCategoryName() { return categoryName; }
        public String getPriority() { return priority; }
        public String getStatus() { return status; }
        public String getImpact() { return impact; }
        public String getUrgency() { return urgency; }
        public String getPriorityOverrideReason() { return priorityOverrideReason; }
        public String getAssetId() { return assetId; }
        public Asset getAsset() { return asset; }
        public void setAsset(Asset asset) { this.asset = asset; }
        public String getDeviceType() { return deviceType; }
        public String getOsName() { return osName; }
        public String getApplicationName() { return applicationName; }
        public String getLocation() { return location; }
        public String getCreatedBy() { return createdBy; }
        public String getCreatorName() { return creatorName; }
        public String getCreatorEmail() { return creatorEmail; }
        public String getCreatorDepartment() { return creatorDepartment; }
        public String getAssignedTo() { return assignedTo; }
        public String getAssigneeName() { return assigneeName; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public String getDueAt() { return dueAt; }
        public String getFirstResponseAt() { return firstResponseAt; }
        public String getResolvedAt() { return resolvedAt; }
        public String getClosedAt() { return closedAt; }
        public String getResolutionNotes() { return resolutionNotes; }
        public boolean isEscalated() { return escalated; }
        public int getEscalationLevel() { return escalationLevel; }
        public String getChecklistState() { return checklistState; }
        public String getSlaStatus() { return slaStatus; }
        public List<TicketCommentDto> getComments() { return comments; }
        public void setComments(List<TicketCommentDto> comments) { this.comments = comments; }
        public List<TicketStatusHistoryDto> getStatusHistory() { return statusHistory; }
        public void setStatusHistory(List<TicketStatusHistoryDto> statusHistory) { this.statusHistory = statusHistory; }
        public List<TicketEscalationDto> getEscalations() { return escalations; }
        public void setEscalations(List<TicketEscalationDto> escalations) { this.escalations = escalations; }
        public TicketFeedbackDto getFeedback() { return feedback; }
        public void setFeedback(TicketFeedbackDto feedback) { this.feedback = feedback; }
    }

    public static class TicketListResponse {
        private List<TicketDto> items;
        private int total;
        private int page;
        @JsonProperty("page_size")
        private int pageSize;
        @JsonProperty("total_pages")
        private int totalPages;

        public TicketListResponse(List<TicketDto> items, int total, int page, int pageSize) {
            this.items = items;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
            this.totalPages = (int) Math.ceil((double) total / (pageSize > 0 ? pageSize : 1));
        }

        public List<TicketDto> getItems() { return items; }
        public int getTotal() { return total; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
        public int getTotalPages() { return totalPages; }
    }
}
