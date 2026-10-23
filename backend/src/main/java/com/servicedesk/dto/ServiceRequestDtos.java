package com.servicedesk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.servicedesk.model.ServiceRequest;
import com.servicedesk.model.ServiceRequestComment;
import com.servicedesk.model.ServiceRequestTimeline;
import com.servicedesk.model.ServiceRequestType;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceRequestDtos {

    public static class ServiceRequestCreateRequest {
        @NotBlank(message = "request_type_id is required")
        @JsonProperty("request_type_id")
        private String requestTypeId;

        @NotBlank(message = "title is required")
        private String title;

        @NotBlank(message = "description is required")
        private String description;

        @NotBlank(message = "business_justification is required")
        @JsonProperty("business_justification")
        private String businessJustification;

        @JsonProperty("required_date")
        private String requiredDate;

        @JsonProperty("asset_id")
        private String assetId;

        @JsonProperty("application_name")
        private String applicationName;

        @JsonProperty("access_level")
        private String accessLevel;

        @JsonProperty("software_name")
        private String softwareName;

        @JsonProperty("software_version")
        private String softwareVersion;

        @JsonProperty("device_type")
        private String deviceType;

        public String getRequestTypeId() { return requestTypeId; }
        public void setRequestTypeId(String requestTypeId) { this.requestTypeId = requestTypeId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getBusinessJustification() { return businessJustification; }
        public void setBusinessJustification(String businessJustification) { this.businessJustification = businessJustification; }
        public String getRequiredDate() { return requiredDate; }
        public void setRequiredDate(String requiredDate) { this.requiredDate = requiredDate; }
        public String getAssetId() { return assetId; }
        public void setAssetId(String assetId) { this.assetId = assetId; }
        public String getApplicationName() { return applicationName; }
        public void setApplicationName(String applicationName) { this.applicationName = applicationName; }
        public String getAccessLevel() { return accessLevel; }
        public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }
        public String getSoftwareName() { return softwareName; }
        public void setSoftwareName(String softwareName) { this.softwareName = softwareName; }
        public String getSoftwareVersion() { return softwareVersion; }
        public void setSoftwareVersion(String softwareVersion) { this.softwareVersion = softwareVersion; }
        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
    }

    public static class ServiceRequestTypeCreateRequest {
        @NotBlank(message = "name is required")
        private String name;
        @NotBlank(message = "description is required")
        private String description;
        private String category = "General IT";
        @JsonProperty("default_priority")
        private String defaultPriority = "Medium";
        @JsonProperty("approval_required")
        private boolean approvalRequired = false;
        @JsonProperty("default_sla_hours")
        private int defaultSlaHours = 24;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDefaultPriority() { return defaultPriority; }
        public void setDefaultPriority(String defaultPriority) { this.defaultPriority = defaultPriority; }
        public boolean isApprovalRequired() { return approvalRequired; }
        public void setApprovalRequired(boolean approvalRequired) { this.approvalRequired = approvalRequired; }
        public int getDefaultSlaHours() { return defaultSlaHours; }
        public void setDefaultSlaHours(int defaultSlaHours) { this.defaultSlaHours = defaultSlaHours; }
    }

    public static class ServiceRequestTypeUpdateRequest {
        private String name;
        private String description;
        private String category;
        @JsonProperty("default_priority")
        private String defaultPriority;
        @JsonProperty("approval_required")
        private boolean approvalRequired;
        @JsonProperty("default_sla_hours")
        private int defaultSlaHours;
        @JsonProperty("is_active")
        private boolean active = true;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDefaultPriority() { return defaultPriority; }
        public void setDefaultPriority(String defaultPriority) { this.defaultPriority = defaultPriority; }
        public boolean isApprovalRequired() { return approvalRequired; }
        public void setApprovalRequired(boolean approvalRequired) { this.approvalRequired = approvalRequired; }
        public int getDefaultSlaHours() { return defaultSlaHours; }
        public void setDefaultSlaHours(int defaultSlaHours) { this.defaultSlaHours = defaultSlaHours; }
        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }
    }

    public static class ServiceRequestApproveRequest {
        private boolean approved = true;
        private String reason;
        public boolean isApproved() { return approved; }
        public void setApproved(boolean approved) { this.approved = approved; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class ServiceRequestRejectRequest {
        @NotBlank(message = "reason is required")
        private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class ServiceRequestAssignRequest {
        @JsonProperty("assigned_to")
        private String assignedTo;
        @JsonProperty("assigned_team")
        private String assignedTeam;
        public String getAssignedTo() { return assignedTo; }
        public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
        public String getAssignedTeam() { return assignedTeam; }
        public void setAssignedTeam(String assignedTeam) { this.assignedTeam = assignedTeam; }
    }

    public static class ServiceRequestStatusRequest {
        @NotBlank(message = "status is required")
        private String status;
        private String reason;
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class ServiceRequestFulfillRequest {
        @JsonProperty("fulfillment_details")
        private Object fulfillmentDetails;
        @NotBlank(message = "resolution_notes is required")
        @JsonProperty("resolution_notes")
        private String resolutionNotes;
        public Object getFulfillmentDetails() { return fulfillmentDetails; }
        public void setFulfillmentDetails(Object fulfillmentDetails) { this.fulfillmentDetails = fulfillmentDetails; }
        public String getResolutionNotes() { return resolutionNotes; }
        public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }
    }

    public static class ServiceRequestConfirmRequest {
        @JsonProperty("feedback_notes")
        private String feedbackNotes;
        public String getFeedbackNotes() { return feedbackNotes; }
        public void setFeedbackNotes(String feedbackNotes) { this.feedbackNotes = feedbackNotes; }
    }

    public static class ServiceRequestReportProblemRequest {
        @NotBlank(message = "problem_description is required")
        @JsonProperty("problem_description")
        private String problemDescription;
        public String getProblemDescription() { return problemDescription; }
        public void setProblemDescription(String problemDescription) { this.problemDescription = problemDescription; }
    }

    public static class ServiceRequestCancelRequest {
        private String reason;
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class ServiceRequestCommentRequest {
        @NotBlank(message = "comment is required")
        private String comment;
        @JsonProperty("is_internal")
        private boolean internal = false;
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public boolean isInternal() { return internal; }
        public void setInternal(boolean internal) { this.internal = internal; }
    }

    public static class ServiceRequestCommentDto {
        private String id;
        @JsonProperty("request_id")
        private String requestId;
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

        public static ServiceRequestCommentDto fromEntity(ServiceRequestComment c) {
            if (c == null) return null;
            ServiceRequestCommentDto d = new ServiceRequestCommentDto();
            d.id = c.getId();
            d.requestId = c.getRequestId();
            d.userId = c.getUserId();
            d.userName = c.getUserName();
            d.userRole = c.getUserRole();
            d.comment = c.getComment();
            d.internal = c.isInternal();
            d.createdAt = c.getCreatedAt() != null ? c.getCreatedAt().toString() : null;
            return d;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getRequestId() { return requestId; }
        public void setRequestId(String requestId) { this.requestId = requestId; }
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

    public static class ServiceRequestTimelineDto {
        private String id;
        @JsonProperty("request_id")
        private String requestId;
        @JsonProperty("event_type")
        private String eventType;
        private String title;
        private String description;
        @JsonProperty("actor_id")
        private String actorId;
        @JsonProperty("actor_name")
        private String actorName;
        @JsonProperty("created_at")
        private String createdAt;

        public static ServiceRequestTimelineDto fromEntity(ServiceRequestTimeline t) {
            if (t == null) return null;
            ServiceRequestTimelineDto d = new ServiceRequestTimelineDto();
            d.id = t.getId();
            d.requestId = t.getRequestId();
            d.eventType = t.getEventType();
            d.title = t.getTitle();
            d.description = t.getDescription();
            d.actorId = t.getActorId();
            d.actorName = t.getActorName();
            d.createdAt = t.getCreatedAt() != null ? t.getCreatedAt().toString() : null;
            return d;
        }

        public String getId() { return id; }
        public String getRequestId() { return requestId; }
        public String getEventType() { return eventType; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getActorId() { return actorId; }
        public String getActorName() { return actorName; }
        public String getCreatedAt() { return createdAt; }
    }

    public static class ServiceRequestDto {
        private String id;
        @JsonProperty("request_number")
        private String requestNumber;
        private String title;
        private String description;
        private String category;
        private String priority;
        private String status;
        @JsonProperty("request_type_id")
        private String requestTypeId;
        @JsonProperty("request_type_name")
        private String requestTypeName;
        @JsonProperty("requester_id")
        private String requesterId;
        @JsonProperty("requester_name")
        private String requesterName;
        @JsonProperty("requester_email")
        private String requesterEmail;
        @JsonProperty("requester_department")
        private String requesterDepartment;
        @JsonProperty("approval_required")
        private boolean approvalRequired;
        @JsonProperty("approval_status")
        private String approvalStatus;
        @JsonProperty("approver_id")
        private String approverId;
        @JsonProperty("approver_name")
        private String approverName;
        @JsonProperty("approval_reason")
        private String approvalReason;
        @JsonProperty("approved_at")
        private String approvedAt;
        @JsonProperty("assigned_to")
        private String assignedTo;
        @JsonProperty("assignee_name")
        private String assigneeName;
        @JsonProperty("assigned_team")
        private String assignedTeam;
        @JsonProperty("business_justification")
        private String businessJustification;
        @JsonProperty("required_date")
        private String requiredDate;
        @JsonProperty("asset_id")
        private String assetId;
        @JsonProperty("application_name")
        private String applicationName;
        @JsonProperty("access_level")
        private String accessLevel;
        @JsonProperty("software_name")
        private String softwareName;
        @JsonProperty("software_version")
        private String softwareVersion;
        @JsonProperty("device_type")
        private String deviceType;
        @JsonProperty("fulfillment_details")
        private String fulfillmentDetails;
        @JsonProperty("resolution_notes")
        private String resolutionNotes;
        @JsonProperty("due_at")
        private String dueAt;
        @JsonProperty("fulfilled_at")
        private String fulfilledAt;
        @JsonProperty("closed_at")
        private String closedAt;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("updated_at")
        private String updatedAt;
        @JsonProperty("sla_status")
        private String slaStatus;

        private List<ServiceRequestCommentDto> comments = new ArrayList<>();
        private List<ServiceRequestTimelineDto> timeline = new ArrayList<>();

        public static ServiceRequestDto fromEntity(ServiceRequest s) {
            if (s == null) return null;
            ServiceRequestDto d = new ServiceRequestDto();
            d.id = s.getId();
            d.requestNumber = s.getRequestNumber();
            d.title = s.getTitle();
            d.description = s.getDescription();
            d.category = s.getCategory();
            d.priority = s.getPriority();
            d.status = s.getStatus();
            d.requestTypeId = s.getRequestTypeId();
            d.requestTypeName = s.getRequestTypeName();
            d.requesterId = s.getRequesterId();
            d.requesterName = s.getRequesterName();
            d.requesterEmail = s.getRequesterEmail();
            d.requesterDepartment = s.getRequesterDepartment();
            d.approvalRequired = s.isApprovalRequired();
            d.approvalStatus = s.getApprovalStatus();
            d.approverId = s.getApproverId();
            d.approverName = s.getApproverName();
            d.approvalReason = s.getApprovalReason();
            d.approvedAt = s.getApprovedAt() != null ? s.getApprovedAt().toString() : null;
            d.assignedTo = s.getAssignedTo();
            d.assigneeName = s.getAssigneeName();
            d.assignedTeam = s.getAssignedTeam();
            d.businessJustification = s.getBusinessJustification();
            d.requiredDate = s.getRequiredDate();
            d.assetId = s.getAssetId();
            d.applicationName = s.getApplicationName();
            d.accessLevel = s.getAccessLevel();
            d.softwareName = s.getSoftwareName();
            d.softwareVersion = s.getSoftwareVersion();
            d.deviceType = s.getDeviceType();
            d.fulfillmentDetails = s.getFulfillmentDetails();
            d.resolutionNotes = s.getResolutionNotes();
            d.dueAt = s.getDueAt() != null ? s.getDueAt().toString() : null;
            d.fulfilledAt = s.getFulfilledAt() != null ? s.getFulfilledAt().toString() : null;
            d.closedAt = s.getClosedAt() != null ? s.getClosedAt().toString() : null;
            d.createdAt = s.getCreatedAt() != null ? s.getCreatedAt().toString() : null;
            d.updatedAt = s.getUpdatedAt() != null ? s.getUpdatedAt().toString() : null;
            d.slaStatus = s.getSlaStatus();
            return d;
        }

        public String getId() { return id; }
        public String getRequestNumber() { return requestNumber; }
        public String getTitle() { return title; }
        public String getDescription() { return description; }
        public String getCategory() { return category; }
        public String getPriority() { return priority; }
        public String getStatus() { return status; }
        public String getRequestTypeId() { return requestTypeId; }
        public String getRequestTypeName() { return requestTypeName; }
        public String getRequesterId() { return requesterId; }
        public String getRequesterName() { return requesterName; }
        public String getRequesterEmail() { return requesterEmail; }
        public String getRequesterDepartment() { return requesterDepartment; }
        public boolean isApprovalRequired() { return approvalRequired; }
        public String getApprovalStatus() { return approvalStatus; }
        public String getApproverId() { return approverId; }
        public String getApproverName() { return approverName; }
        public String getApprovalReason() { return approvalReason; }
        public String getApprovedAt() { return approvedAt; }
        public String getAssignedTo() { return assignedTo; }
        public String getAssigneeName() { return assigneeName; }
        public String getAssignedTeam() { return assignedTeam; }
        public String getBusinessJustification() { return businessJustification; }
        public String getRequiredDate() { return requiredDate; }
        public String getAssetId() { return assetId; }
        public String getApplicationName() { return applicationName; }
        public String getAccessLevel() { return accessLevel; }
        public String getSoftwareName() { return softwareName; }
        public String getSoftwareVersion() { return softwareVersion; }
        public String getDeviceType() { return deviceType; }
        public String getFulfillmentDetails() { return fulfillmentDetails; }
        public String getResolutionNotes() { return resolutionNotes; }
        public String getDueAt() { return dueAt; }
        public String getFulfilledAt() { return fulfilledAt; }
        public String getClosedAt() { return closedAt; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
        public String getSlaStatus() { return slaStatus; }
        public List<ServiceRequestCommentDto> getComments() { return comments; }
        public void setComments(List<ServiceRequestCommentDto> comments) { this.comments = comments; }
        public List<ServiceRequestTimelineDto> getTimeline() { return timeline; }
        public void setTimeline(List<ServiceRequestTimelineDto> timeline) { this.timeline = timeline; }
    }

    public static class ServiceRequestListResponse {
        private List<ServiceRequestDto> items;
        private int total;
        private int page;
        @JsonProperty("page_size")
        private int pageSize;
        @JsonProperty("total_pages")
        private int totalPages;

        public ServiceRequestListResponse(List<ServiceRequestDto> items, int total, int page, int pageSize) {
            this.items = items;
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
            this.totalPages = (int) Math.ceil((double) total / (pageSize > 0 ? pageSize : 1));
        }

        public List<ServiceRequestDto> getItems() { return items; }
        public int getTotal() { return total; }
        public int getPage() { return page; }
        public int getPageSize() { return pageSize; }
        public int getTotalPages() { return totalPages; }
    }

    public static class ServiceRequestDashboardSummaryDto {
        @JsonProperty("total_requests")
        private int totalRequests;
        @JsonProperty("pending_approval")
        private int pendingApproval;
        private int unassigned;
        @JsonProperty("in_progress")
        private int inProgress;
        private int fulfilled;
        private int closed;
        @JsonProperty("sla_breached")
        private int slaBreached;
        @JsonProperty("sla_at_risk")
        private int slaAtRisk;
        @JsonProperty("sla_compliance_rate")
        private double slaComplianceRate = 100.0;
        @JsonProperty("average_fulfillment_hours")
        private double averageFulfillmentHours = 0.0;
        @JsonProperty("by_type")
        private Map<String, Integer> byType = new HashMap<>();
        @JsonProperty("by_category")
        private Map<String, Integer> byCategory = new HashMap<>();
        @JsonProperty("by_status")
        private Map<String, Integer> byStatus = new HashMap<>();
        @JsonProperty("by_team")
        private Map<String, Integer> byTeam = new HashMap<>();
        @JsonProperty("daily_trend")
        private List<Map<String, Object>> dailyTrend = new ArrayList<>();

        public ServiceRequestDashboardSummaryDto() {}

        public ServiceRequestDashboardSummaryDto(int total, int pending, int inProgress, int fulfilled, int closed, int breached) {
            this.totalRequests = total;
            this.pendingApproval = pending;
            this.inProgress = inProgress;
            this.fulfilled = fulfilled;
            this.closed = closed;
            this.slaBreached = breached;
        }

        public int getTotalRequests() { return totalRequests; }
        public void setTotalRequests(int totalRequests) { this.totalRequests = totalRequests; }
        public int getPendingApproval() { return pendingApproval; }
        public void setPendingApproval(int pendingApproval) { this.pendingApproval = pendingApproval; }
        public int getUnassigned() { return unassigned; }
        public void setUnassigned(int unassigned) { this.unassigned = unassigned; }
        public int getInProgress() { return inProgress; }
        public void setInProgress(int inProgress) { this.inProgress = inProgress; }
        public int getFulfilled() { return fulfilled; }
        public void setFulfilled(int fulfilled) { this.fulfilled = fulfilled; }
        public int getClosed() { return closed; }
        public void setClosed(int closed) { this.closed = closed; }
        public int getSlaBreached() { return slaBreached; }
        public void setSlaBreached(int slaBreached) { this.slaBreached = slaBreached; }
        public int getSlaAtRisk() { return slaAtRisk; }
        public void setSlaAtRisk(int slaAtRisk) { this.slaAtRisk = slaAtRisk; }
        public double getSlaComplianceRate() { return slaComplianceRate; }
        public void setSlaComplianceRate(double slaComplianceRate) { this.slaComplianceRate = slaComplianceRate; }
        public double getAverageFulfillmentHours() { return averageFulfillmentHours; }
        public void setAverageFulfillmentHours(double averageFulfillmentHours) { this.averageFulfillmentHours = averageFulfillmentHours; }
        public Map<String, Integer> getByType() { return byType; }
        public void setByType(Map<String, Integer> byType) { this.byType = byType; }
        public Map<String, Integer> getByCategory() { return byCategory; }
        public void setByCategory(Map<String, Integer> byCategory) { this.byCategory = byCategory; }
        public Map<String, Integer> getByStatus() { return byStatus; }
        public void setByStatus(Map<String, Integer> byStatus) { this.byStatus = byStatus; }
        public Map<String, Integer> getByTeam() { return byTeam; }
        public void setByTeam(Map<String, Integer> byTeam) { this.byTeam = byTeam; }
        public List<Map<String, Object>> getDailyTrend() { return dailyTrend; }
        public void setDailyTrend(List<Map<String, Object>> dailyTrend) { this.dailyTrend = dailyTrend; }
    }

    public static class UnifiedWorkItemDto {
        private String id;
        private String type; // "Incident" or "Service Request"
        @JsonProperty("item_number")
        private String itemNumber;
        private String title;
        private String status;
        private String priority;
        private String category;
        @JsonProperty("requester_name")
        private String requesterName;
        @JsonProperty("assignee_name")
        private String assigneeName;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("due_at")
        private String dueAt;
        @JsonProperty("sla_status")
        private String slaStatus;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getItemNumber() { return itemNumber; }
        public void setItemNumber(String itemNumber) { this.itemNumber = itemNumber; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getRequesterName() { return requesterName; }
        public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
        public String getAssigneeName() { return assigneeName; }
        public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
        public String getDueAt() { return dueAt; }
        public void setDueAt(String dueAt) { this.dueAt = dueAt; }
        public String getSlaStatus() { return slaStatus; }
        public void setSlaStatus(String slaStatus) { this.slaStatus = slaStatus; }
    }
}
