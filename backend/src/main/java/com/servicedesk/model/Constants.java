package com.servicedesk.model;

public final class Constants {

    private Constants() {}

    public static final class UserRole {
        public static final String EMPLOYEE = "EMPLOYEE";
        public static final String SUPPORT = "SUPPORT";
        public static final String ADMIN = "ADMIN";
    }

    public static final class TicketPriority {
        public static final String LOW = "Low";
        public static final String MEDIUM = "Medium";
        public static final String HIGH = "High";
        public static final String CRITICAL = "Critical";
    }

    public static final class TicketStatus {
        public static final String OPEN = "Open";
        public static final String IN_PROGRESS = "In Progress";
        public static final String WAITING_FOR_USER = "Waiting for User";
        public static final String WAITING_FOR_VENDOR = "Waiting for Vendor";
        public static final String ESCALATED = "Escalated";
        public static final String RESOLVED = "Resolved";
        public static final String CLOSED = "Closed";
    }

    public static final class ImpactLevel {
        public static final String INDIVIDUAL = "Individual";
        public static final String TEAM = "Team";
        public static final String DEPARTMENT = "Department";
        public static final String MULTIPLE_DEPARTMENTS = "Multiple Departments";
        public static final String COMPANY_WIDE = "Company-wide";
    }

    public static final class UrgencyLevel {
        public static final String LOW = "Low";
        public static final String MEDIUM = "Medium";
        public static final String HIGH = "High";
        public static final String CRITICAL = "Critical";
    }

    public static final class AssetStatus {
        public static final String IN_USE = "In Use";
        public static final String IN_STOCK = "In Stock";
        public static final String IN_REPAIR = "In Repair";
        public static final String RETIRED = "Retired";
    }

    public static final class OnboardingTaskStatus {
        public static final String NOT_STARTED = "Not Started";
        public static final String IN_PROGRESS = "In Progress";
        public static final String BLOCKED = "Blocked";
        public static final String COMPLETED = "Completed";
    }

    public static final class AccessRequestStatus {
        public static final String REQUESTED = "Requested";
        public static final String PENDING_APPROVAL = "Pending Approval";
        public static final String APPROVED = "Approved";
        public static final String IN_PROGRESS = "In Progress";
        public static final String COMPLETED = "Completed";
        public static final String REJECTED = "Rejected";
    }

    public static final class ImprovementStatus {
        public static final String PROPOSED = "Proposed";
        public static final String UNDER_REVIEW = "Under Review";
        public static final String APPROVED = "Approved";
        public static final String IN_PROGRESS = "In Progress";
        public static final String IMPLEMENTED = "Implemented";
        public static final String DEFERRED = "Deferred";
    }

    public static final class ServiceRequestStatus {
        public static final String SUBMITTED = "Submitted";
        public static final String PENDING_APPROVAL = "Pending Approval";
        public static final String APPROVED = "Approved";
        public static final String REJECTED = "Rejected";
        public static final String ASSIGNED = "Assigned";
        public static final String IN_PROGRESS = "In Progress";
        public static final String WAITING_FOR_USER = "Waiting for User";
        public static final String FULFILLED = "Fulfilled";
        public static final String CLOSED = "Closed";
        public static final String CANCELLED = "Cancelled";
    }

    public static final class ServiceRequestApprovalStatus {
        public static final String NONE = "None";
        public static final String PENDING = "Pending";
        public static final String APPROVED = "Approved";
        public static final String REJECTED = "Rejected";
    }

    public static final class SupportTeam {
        public static final String SERVICE_DESK = "Service Desk";
        public static final String ENDPOINT_SUPPORT = "Endpoint Support";
        public static final String NETWORK_SUPPORT = "Network Support";
        public static final String APPLICATION_SUPPORT = "Application Support";
        public static final String M365_SUPPORT = "Microsoft 365 Support";
        public static final String GOOGLE_WORKSPACE_SUPPORT = "Google Workspace Support";
        public static final String HARDWARE_SUPPORT = "Hardware Support";
        public static final String SECURITY = "Security";
    }

    public static final class ServiceRequestCategory {
        public static final String HARDWARE = "Hardware";
        public static final String SOFTWARE = "Software";
        public static final String ACCESS = "Access";
        public static final String MICROSOFT_365 = "Microsoft 365";
        public static final String GOOGLE_WORKSPACE = "Google Workspace";
        public static final String NETWORK = "Network";
        public static final String MOBILE = "Mobile";
        public static final String ACCOUNT = "Account";
        public static final String ONBOARDING = "Onboarding";
        public static final String GENERAL_IT = "General IT";
    }
}
