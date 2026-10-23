package com.servicedesk.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class OnboardingRequest {
    private String id;
    private String employeeName;
    private String department;
    private String jobTitle;
    private String managerName;
    private String startDate;
    private String location;
    private String deviceRequirement;
    private String softwareRequirement;
    private String accessRequirement;
    private String status;
    private int progressPercent;
    private String createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private List<OnboardingTask> tasks = new ArrayList<>();

    public OnboardingRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmployeeName() { return employeeName; }
    public void setEmployeeName(String employeeName) { this.employeeName = employeeName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

    public String getManagerName() { return managerName; }
    public void setManagerName(String managerName) { this.managerName = managerName; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getDeviceRequirement() { return deviceRequirement; }
    public void setDeviceRequirement(String deviceRequirement) { this.deviceRequirement = deviceRequirement; }

    public String getSoftwareRequirement() { return softwareRequirement; }
    public void setSoftwareRequirement(String softwareRequirement) { this.softwareRequirement = softwareRequirement; }

    public String getAccessRequirement() { return accessRequirement; }
    public void setAccessRequirement(String accessRequirement) { this.accessRequirement = accessRequirement; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public List<OnboardingTask> getTasks() { return tasks; }
    public void setTasks(List<OnboardingTask> tasks) { this.tasks = tasks; }
}
