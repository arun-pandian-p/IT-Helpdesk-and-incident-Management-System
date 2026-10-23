package com.servicedesk.model;

import java.time.Instant;

public class ServiceRequestType {
    private String id;
    private String name;
    private String description;
    private String category;
    private String defaultPriority;
    private boolean approvalRequired;
    private int defaultSlaHours;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    public ServiceRequestType() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

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

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
