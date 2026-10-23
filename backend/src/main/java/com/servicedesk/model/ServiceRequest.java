package com.servicedesk.model;

import java.time.Instant;

public class ServiceRequest {
    private String id;
    private String requestNumber;
    private String title;
    private String description;
    private String category;
    private String priority;
    private String status;
    private String requestTypeId;
    private String requesterId;
    private boolean approvalRequired;
    private String approvalStatus;
    private String approverId;
    private String approvalReason;
    private Instant approvedAt;
    private String assignedTo;
    private String assignedTeam;
    private String businessJustification;
    private String requiredDate;
    private String assetId;
    private String applicationName;
    private String accessLevel;
    private String softwareName;
    private String softwareVersion;
    private String deviceType;
    private String fulfillmentDetails;
    private String resolutionNotes;
    private Instant dueAt;
    private Instant fulfilledAt;
    private Instant closedAt;
    private Instant createdAt;
    private Instant updatedAt;

    // Joined fields
    private String requestTypeName;
    private String requesterName;
    private String requesterEmail;
    private String requesterDepartment;
    private String approverName;
    private String assigneeName;
    private String slaStatus;

    public ServiceRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequestNumber() { return requestNumber; }
    public void setRequestNumber(String requestNumber) { this.requestNumber = requestNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRequestTypeId() { return requestTypeId; }
    public void setRequestTypeId(String requestTypeId) { this.requestTypeId = requestTypeId; }

    public String getRequesterId() { return requesterId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }

    public boolean isApprovalRequired() { return approvalRequired; }
    public void setApprovalRequired(boolean approvalRequired) { this.approvalRequired = approvalRequired; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    public String getApproverId() { return approverId; }
    public void setApproverId(String approverId) { this.approverId = approverId; }

    public String getApprovalReason() { return approvalReason; }
    public void setApprovalReason(String approvalReason) { this.approvalReason = approvalReason; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }

    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }

    public String getAssignedTeam() { return assignedTeam; }
    public void setAssignedTeam(String assignedTeam) { this.assignedTeam = assignedTeam; }

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

    public String getFulfillmentDetails() { return fulfillmentDetails; }
    public void setFulfillmentDetails(String fulfillmentDetails) { this.fulfillmentDetails = fulfillmentDetails; }

    public String getResolutionNotes() { return resolutionNotes; }
    public void setResolutionNotes(String resolutionNotes) { this.resolutionNotes = resolutionNotes; }

    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant dueAt) { this.dueAt = dueAt; }

    public Instant getFulfilledAt() { return fulfilledAt; }
    public void setFulfilledAt(Instant fulfilledAt) { this.fulfilledAt = fulfilledAt; }

    public Instant getClosedAt() { return closedAt; }
    public void setClosedAt(Instant closedAt) { this.closedAt = closedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public String getRequestTypeName() { return requestTypeName; }
    public void setRequestTypeName(String requestTypeName) { this.requestTypeName = requestTypeName; }

    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }

    public String getRequesterEmail() { return requesterEmail; }
    public void setRequesterEmail(String requesterEmail) { this.requesterEmail = requesterEmail; }

    public String getRequesterDepartment() { return requesterDepartment; }
    public void setRequesterDepartment(String requesterDepartment) { this.requesterDepartment = requesterDepartment; }

    public String getApproverName() { return approverName; }
    public void setApproverName(String approverName) { this.approverName = approverName; }

    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }

    public String getSlaStatus() { return slaStatus; }
    public void setSlaStatus(String slaStatus) { this.slaStatus = slaStatus; }
}
