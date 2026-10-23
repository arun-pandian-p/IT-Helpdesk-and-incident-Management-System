package com.servicedesk.model;

import java.time.Instant;

public class AccessRequest {
    private String id;
    private String requestType;
    private String requestedSystem;
    private String accessLevel;
    private String reason;
    private String requestedBy;
    private String requestedByName;
    private String approvedBy;
    private String approvedByName;
    private String assignedEngineerId;
    private String assignedEngineerName;
    private String status;
    private Instant completedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public AccessRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getRequestType() { return requestType; }
    public void setRequestType(String requestType) { this.requestType = requestType; }

    public String getRequestedSystem() { return requestedSystem; }
    public void setRequestedSystem(String requestedSystem) { this.requestedSystem = requestedSystem; }

    public String getAccessLevel() { return accessLevel; }
    public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getRequestedBy() { return requestedBy; }
    public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }

    public String getRequestedByName() { return requestedByName; }
    public void setRequestedByName(String requestedByName) { this.requestedByName = requestedByName; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public String getApprovedByName() { return approvedByName; }
    public void setApprovedByName(String approvedByName) { this.approvedByName = approvedByName; }

    public String getAssignedEngineerId() { return assignedEngineerId; }
    public void setAssignedEngineerId(String assignedEngineerId) { this.assignedEngineerId = assignedEngineerId; }

    public String getAssignedEngineerName() { return assignedEngineerName; }
    public void setAssignedEngineerName(String assignedEngineerName) { this.assignedEngineerName = assignedEngineerName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
