package com.servicedesk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class CommonDtos {

    public static class CategoryCreateRequest {
        @NotBlank(message = "Category name is required")
        private String name;
        private String description;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    public static class AssetCreateRequest {
        @NotBlank(message = "asset_tag is required")
        @JsonProperty("asset_tag")
        private String assetTag;

        @NotBlank(message = "device_type is required")
        @JsonProperty("device_type")
        private String deviceType;

        @NotBlank(message = "manufacturer is required")
        private String manufacturer;

        @NotBlank(message = "model is required")
        private String model;

        @NotBlank(message = "serial_number is required")
        @JsonProperty("serial_number")
        private String serialNumber;

        private String status = "In Use";

        @JsonProperty("assigned_user_id")
        private String assignedUserId;

        @JsonProperty("purchase_date")
        private String purchaseDate;

        @JsonProperty("warranty_expiry")
        private String warrantyExpiry;

        private String notes;

        public String getAssetTag() { return assetTag; }
        public void setAssetTag(String assetTag) { this.assetTag = assetTag; }
        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
        public String getManufacturer() { return manufacturer; }
        public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getSerialNumber() { return serialNumber; }
        public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getAssignedUserId() { return assignedUserId; }
        public void setAssignedUserId(String assignedUserId) { this.assignedUserId = assignedUserId; }
        public String getPurchaseDate() { return purchaseDate; }
        public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }
        public String getWarrantyExpiry() { return warrantyExpiry; }
        public void setWarrantyExpiry(String warrantyExpiry) { this.warrantyExpiry = warrantyExpiry; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class AssetUpdateRequest {
        @JsonProperty("device_type")
        private String deviceType;
        private String manufacturer;
        private String model;
        private String status;
        @JsonProperty("assigned_user_id")
        private String assignedUserId;
        @JsonProperty("purchase_date")
        private String purchaseDate;
        @JsonProperty("warranty_expiry")
        private String warrantyExpiry;
        private String notes;

        public String getDeviceType() { return deviceType; }
        public void setDeviceType(String deviceType) { this.deviceType = deviceType; }
        public String getManufacturer() { return manufacturer; }
        public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getAssignedUserId() { return assignedUserId; }
        public void setAssignedUserId(String assignedUserId) { this.assignedUserId = assignedUserId; }
        public String getPurchaseDate() { return purchaseDate; }
        public void setPurchaseDate(String purchaseDate) { this.purchaseDate = purchaseDate; }
        public String getWarrantyExpiry() { return warrantyExpiry; }
        public void setWarrantyExpiry(String warrantyExpiry) { this.warrantyExpiry = warrantyExpiry; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class OnboardingCreateRequest {
        @NotBlank(message = "employee_name is required")
        @JsonProperty("employee_name")
        private String employeeName;

        @NotBlank(message = "department is required")
        private String department;

        @NotBlank(message = "job_title is required")
        @JsonProperty("job_title")
        private String jobTitle;

        @NotBlank(message = "manager_name is required")
        @JsonProperty("manager_name")
        private String managerName;

        @NotBlank(message = "start_date is required")
        @JsonProperty("start_date")
        private String startDate;

        private String location = "Headquarters";

        @JsonProperty("device_requirement")
        private String deviceRequirement = "Standard Laptop";

        @JsonProperty("software_requirement")
        private String softwareRequirement;

        @JsonProperty("access_requirement")
        private String accessRequirement;

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
    }

    public static class OnboardingTaskUpdateRequest {
        private String status;
        private String notes;
        @JsonProperty("owner_id")
        private String ownerId;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public String getOwnerId() { return ownerId; }
        public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    }

    public static class AccessRequestCreateRequest {
        @NotBlank(message = "request_type is required")
        @JsonProperty("request_type")
        private String requestType;

        @NotBlank(message = "requested_system is required")
        @JsonProperty("requested_system")
        private String requestedSystem;

        @JsonProperty("access_level")
        private String accessLevel = "Read/Write";

        @NotBlank(message = "reason is required")
        private String reason;

        public String getRequestType() { return requestType; }
        public void setRequestType(String requestType) { this.requestType = requestType; }
        public String getRequestedSystem() { return requestedSystem; }
        public void setRequestedSystem(String requestedSystem) { this.requestedSystem = requestedSystem; }
        public String getAccessLevel() { return accessLevel; }
        public void setAccessLevel(String accessLevel) { this.accessLevel = accessLevel; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    public static class AccessRequestStatusUpdateRequest {
        @NotBlank(message = "status is required")
        private String status;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class KnowledgeCreateRequest {
        @NotBlank(message = "title is required")
        private String title;

        @NotBlank(message = "category is required")
        private String category;

        @JsonProperty("os_target")
        private String osTarget = "All";

        @NotBlank(message = "content is required")
        private String content;

        private String tags;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getOsTarget() { return osTarget; }
        public void setOsTarget(String osTarget) { this.osTarget = osTarget; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
    }

    public static class ImprovementCreateRequest {
        @NotBlank(message = "title is required")
        private String title;
        @NotBlank(message = "category is required")
        private String category;
        @NotBlank(message = "description is required")
        private String description;
        @NotBlank(message = "business_benefit is required")
        @JsonProperty("business_benefit")
        private String businessBenefit;
        private String priority = "Medium";

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getBusinessBenefit() { return businessBenefit; }
        public void setBusinessBenefit(String businessBenefit) { this.businessBenefit = businessBenefit; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
    }

    public static class ImprovementUpdateRequest {
        private String status;
        private String priority;
        @JsonProperty("assigned_to")
        private String assignedTo;

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getPriority() { return priority; }
        public void setPriority(String priority) { this.priority = priority; }
        public String getAssignedTo() { return assignedTo; }
        public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    }
}
