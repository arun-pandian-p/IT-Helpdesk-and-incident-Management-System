from typing import List, Optional
from datetime import datetime, date, timezone
from sqlalchemy import Boolean, Date, DateTime, ForeignKey, Integer, String, Text, Index
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.db.base import Base, TimestampMixin, generate_uuid, get_utc_now


class UserRole:
    EMPLOYEE = "EMPLOYEE"
    SUPPORT = "SUPPORT"
    ADMIN = "ADMIN"


class TicketPriority:
    LOW = "Low"
    MEDIUM = "Medium"
    HIGH = "High"
    CRITICAL = "Critical"


class TicketStatus:
    OPEN = "Open"
    IN_PROGRESS = "In Progress"
    WAITING_FOR_USER = "Waiting for User"
    WAITING_FOR_VENDOR = "Waiting for Vendor"
    ESCALATED = "Escalated"
    RESOLVED = "Resolved"
    CLOSED = "Closed"


class ImpactLevel:
    INDIVIDUAL = "Individual"
    TEAM = "Team"
    DEPARTMENT = "Department"
    MULTIPLE_DEPARTMENTS = "Multiple Departments"
    COMPANY_WIDE = "Company-wide"


class UrgencyLevel:
    LOW = "Low"
    MEDIUM = "Medium"
    HIGH = "High"
    CRITICAL = "Critical"


class AssetStatus:
    IN_USE = "In Use"
    IN_STOCK = "In Stock"
    IN_REPAIR = "In Repair"
    RETIRED = "Retired"


class OnboardingTaskStatus:
    NOT_STARTED = "Not Started"
    IN_PROGRESS = "In Progress"
    BLOCKED = "Blocked"
    COMPLETED = "Completed"


class AccessRequestStatus:
    REQUESTED = "Requested"
    PENDING_APPROVAL = "Pending Approval"
    APPROVED = "Approved"
    IN_PROGRESS = "In Progress"
    COMPLETED = "Completed"
    REJECTED = "Rejected"


class ImprovementStatus:
    PROPOSED = "Proposed"
    UNDER_REVIEW = "Under Review"
    APPROVED = "Approved"
    IN_PROGRESS = "In Progress"
    IMPLEMENTED = "Implemented"
    DEFERRED = "Deferred"


class ServiceRequestStatus:
    SUBMITTED = "Submitted"
    PENDING_APPROVAL = "Pending Approval"
    APPROVED = "Approved"
    REJECTED = "Rejected"
    ASSIGNED = "Assigned"
    IN_PROGRESS = "In Progress"
    WAITING_FOR_USER = "Waiting for User"
    FULFILLED = "Fulfilled"
    CLOSED = "Closed"
    CANCELLED = "Cancelled"


class ServiceRequestApprovalStatus:
    NONE = "None"
    PENDING = "Pending"
    APPROVED = "Approved"
    REJECTED = "Rejected"


class SupportTeam:
    SERVICE_DESK = "Service Desk"
    ENDPOINT_SUPPORT = "Endpoint Support"
    NETWORK_SUPPORT = "Network Support"
    APPLICATION_SUPPORT = "Application Support"
    M365_SUPPORT = "Microsoft 365 Support"
    GOOGLE_WORKSPACE_SUPPORT = "Google Workspace Support"
    HARDWARE_SUPPORT = "Hardware Support"
    SECURITY = "Security"


class ServiceRequestCategory:
    HARDWARE = "Hardware"
    SOFTWARE = "Software"
    ACCESS = "Access"
    MICROSOFT_365 = "Microsoft 365"
    GOOGLE_WORKSPACE = "Google Workspace"
    NETWORK = "Network"
    MOBILE = "Mobile"
    ACCOUNT = "Account"
    ONBOARDING = "Onboarding"
    GENERAL_IT = "General IT"


# ---------------------------------------------------------------------------
# USERS
# ---------------------------------------------------------------------------
class User(Base, TimestampMixin):
    __tablename__ = "users"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    name: Mapped[str] = mapped_column(String(100), nullable=False)
    email: Mapped[str] = mapped_column(String(255), unique=True, index=True, nullable=False)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    role: Mapped[str] = mapped_column(String(20), default=UserRole.EMPLOYEE, nullable=False, index=True)
    department: Mapped[str] = mapped_column(String(100), default="General", nullable=False)
    job_title: Mapped[Optional[str]] = mapped_column(String(100), nullable=True)
    phone: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)
    location: Mapped[Optional[str]] = mapped_column(String(100), default="Headquarters", nullable=True)
    manager_id: Mapped[Optional[str]] = mapped_column(String(36), ForeignKey("users.id"), nullable=True)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)

    # Relationships
    created_tickets: Mapped[List["Ticket"]] = relationship(
        "Ticket", back_populates="creator", foreign_keys="[Ticket.created_by]"
    )
    assigned_tickets: Mapped[List["Ticket"]] = relationship(
        "Ticket", back_populates="assignee", foreign_keys="[Ticket.assigned_to]"
    )
    comments: Mapped[List["TicketComment"]] = relationship("TicketComment", back_populates="user")
    assigned_assets: Mapped[List["Asset"]] = relationship("Asset", back_populates="assigned_user")
    audit_logs: Mapped[List["AuditLog"]] = relationship("AuditLog", back_populates="user")

    # Service Request Relationships
    created_service_requests: Mapped[List["ServiceRequest"]] = relationship(
        "ServiceRequest", back_populates="requester", foreign_keys="[ServiceRequest.requester_id]"
    )
    assigned_service_requests: Mapped[List["ServiceRequest"]] = relationship(
        "ServiceRequest", back_populates="assignee", foreign_keys="[ServiceRequest.assigned_to]"
    )
    approved_service_requests: Mapped[List["ServiceRequest"]] = relationship(
        "ServiceRequest", back_populates="approver", foreign_keys="[ServiceRequest.approver_id]"
    )
    service_request_comments: Mapped[List["ServiceRequestComment"]] = relationship(
        "ServiceRequestComment", back_populates="user"
    )


# ---------------------------------------------------------------------------
# TICKET CATEGORIES
# ---------------------------------------------------------------------------
class TicketCategory(Base):
    __tablename__ = "ticket_categories"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    name: Mapped[str] = mapped_column(String(100), unique=True, nullable=False)
    description: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=get_utc_now, nullable=False
    )

    tickets: Mapped[List["Ticket"]] = relationship("Ticket", back_populates="category")


# ---------------------------------------------------------------------------
# ASSETS / HARDWARE TRACKING
# ---------------------------------------------------------------------------
class Asset(Base, TimestampMixin):
    __tablename__ = "assets"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    asset_tag: Mapped[str] = mapped_column(String(50), unique=True, index=True, nullable=False)
    device_type: Mapped[str] = mapped_column(String(50), nullable=False)  # Laptop, Desktop, Monitor, etc.
    manufacturer: Mapped[str] = mapped_column(String(100), nullable=False)
    model: Mapped[str] = mapped_column(String(100), nullable=False)
    serial_number: Mapped[str] = mapped_column(String(100), unique=True, nullable=False)
    status: Mapped[str] = mapped_column(String(30), default=AssetStatus.IN_USE, nullable=False, index=True)
    assigned_user_id: Mapped[Optional[str]] = mapped_column(
        String(36), ForeignKey("users.id"), nullable=True, index=True
    )
    purchase_date: Mapped[Optional[date]] = mapped_column(Date, nullable=True)
    warranty_expiry: Mapped[Optional[date]] = mapped_column(Date, nullable=True)
    notes: Mapped[Optional[str]] = mapped_column(Text, nullable=True)

    assigned_user: Mapped[Optional["User"]] = relationship("User", back_populates="assigned_assets")
    tickets: Mapped[List["Ticket"]] = relationship("Ticket", back_populates="asset")


class AssetAssignment(Base):
    __tablename__ = "asset_assignments"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    asset_id: Mapped[str] = mapped_column(String(36), ForeignKey("assets.id", ondelete="CASCADE"), nullable=False)
    user_id: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), nullable=False)
    assigned_by: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), nullable=False)
    assigned_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), default=get_utc_now, nullable=False)
    returned_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), nullable=True)


# ---------------------------------------------------------------------------
# TICKETS / INCIDENTS
# ---------------------------------------------------------------------------
class Ticket(Base, TimestampMixin):
    __tablename__ = "tickets"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    ticket_number: Mapped[str] = mapped_column(String(20), unique=True, index=True, nullable=False)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    description: Mapped[str] = mapped_column(Text, nullable=False)
    category_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("ticket_categories.id"), nullable=False, index=True
    )
    priority: Mapped[str] = mapped_column(
        String(20), default=TicketPriority.MEDIUM, index=True, nullable=False
    )
    status: Mapped[str] = mapped_column(
        String(30), default=TicketStatus.OPEN, index=True, nullable=False
    )
    
    # Impact & Urgency Matrix
    impact: Mapped[str] = mapped_column(String(30), default=ImpactLevel.INDIVIDUAL, nullable=False)
    urgency: Mapped[str] = mapped_column(String(30), default=UrgencyLevel.MEDIUM, nullable=False)
    priority_override_reason: Mapped[Optional[str]] = mapped_column(Text, nullable=True)

    # Device & Environment Context
    asset_id: Mapped[Optional[str]] = mapped_column(
        String(36), ForeignKey("assets.id"), nullable=True, index=True
    )
    device_type: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)
    os_name: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)
    application_name: Mapped[Optional[str]] = mapped_column(String(100), nullable=True)
    location: Mapped[Optional[str]] = mapped_column(String(100), nullable=True)

    # Ownership & Assignments
    created_by: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id"), nullable=False, index=True
    )
    assigned_to: Mapped[Optional[str]] = mapped_column(
        String(36), ForeignKey("users.id"), nullable=True, index=True
    )
    
    # SLA & Timestamps
    due_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, index=True)
    first_response_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), nullable=True)
    resolved_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), nullable=True)
    closed_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), nullable=True)
    resolution_notes: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    is_escalated: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    escalation_level: Mapped[int] = mapped_column(Integer, default=1, nullable=False)

    # Diagnostic Troubleshooting Checklist State (JSON stored as text, e.g. {"step1": true, "step2": false})
    checklist_state: Mapped[Optional[str]] = mapped_column(Text, nullable=True)

    # Relationships
    category: Mapped["TicketCategory"] = relationship("TicketCategory", back_populates="tickets")
    asset: Mapped[Optional["Asset"]] = relationship("Asset", back_populates="tickets")
    creator: Mapped["User"] = relationship(
        "User", back_populates="created_tickets", foreign_keys=[created_by]
    )
    assignee: Mapped[Optional["User"]] = relationship(
        "User", back_populates="assigned_tickets", foreign_keys=[assigned_to]
    )
    comments: Mapped[List["TicketComment"]] = relationship(
        "TicketComment", back_populates="ticket", cascade="all, delete-orphan", order_by="TicketComment.created_at"
    )
    assignments: Mapped[List["TicketAssignment"]] = relationship(
        "TicketAssignment", back_populates="ticket", cascade="all, delete-orphan", order_by="TicketAssignment.assigned_at"
    )
    status_history: Mapped[List["TicketStatusHistory"]] = relationship(
        "TicketStatusHistory", back_populates="ticket", cascade="all, delete-orphan", order_by="TicketStatusHistory.changed_at"
    )
    escalations: Mapped[List["TicketEscalation"]] = relationship(
        "TicketEscalation", back_populates="ticket", cascade="all, delete-orphan", order_by="TicketEscalation.created_at"
    )
    feedback: Mapped[Optional["TicketFeedback"]] = relationship(
        "TicketFeedback", back_populates="ticket", uselist=False, cascade="all, delete-orphan"
    )

    __table_args__ = (
        Index("ix_tickets_created_at", "created_at"),
        Index("ix_tickets_status_priority", "status", "priority"),
    )


# ---------------------------------------------------------------------------
# TICKET COMMENTS
# ---------------------------------------------------------------------------
class TicketComment(Base):
    __tablename__ = "ticket_comments"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    ticket_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("tickets.id", ondelete="CASCADE"), nullable=False, index=True
    )
    user_id: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), nullable=False)
    comment: Mapped[str] = mapped_column(Text, nullable=False)
    is_internal: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=get_utc_now, nullable=False, index=True
    )

    ticket: Mapped["Ticket"] = relationship("Ticket", back_populates="comments")
    user: Mapped["User"] = relationship("User", back_populates="comments")


# ---------------------------------------------------------------------------
# TICKET STATUS HISTORY & ASSIGNMENTS & ESCALATIONS
# ---------------------------------------------------------------------------
class TicketStatusHistory(Base):
    __tablename__ = "ticket_status_history"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    ticket_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("tickets.id", ondelete="CASCADE"), nullable=False, index=True
    )
    old_status: Mapped[str] = mapped_column(String(50), nullable=False)
    new_status: Mapped[str] = mapped_column(String(50), nullable=False)
    changed_by: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), nullable=False)
    changed_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=get_utc_now, nullable=False
    )
    reason: Mapped[Optional[str]] = mapped_column(Text, nullable=True)

    ticket: Mapped["Ticket"] = relationship("Ticket", back_populates="status_history")


class TicketAssignment(Base):
    __tablename__ = "ticket_assignments"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    ticket_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("tickets.id", ondelete="CASCADE"), nullable=False, index=True
    )
    assigned_to: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), nullable=False)
    assigned_by: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), nullable=False)
    assigned_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=get_utc_now, nullable=False
    )
    unassigned_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), nullable=True)

    ticket: Mapped["Ticket"] = relationship("Ticket", back_populates="assignments")


class TicketEscalation(Base):
    __tablename__ = "ticket_escalations"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    ticket_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("tickets.id", ondelete="CASCADE"), nullable=False, index=True
    )
    from_engineer: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), nullable=False)
    to_engineer: Mapped[Optional[str]] = mapped_column(
        String(36), ForeignKey("users.id"), nullable=True
    )
    reason: Mapped[str] = mapped_column(Text, nullable=False)
    escalation_level: Mapped[int] = mapped_column(Integer, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=get_utc_now, nullable=False
    )
    resolved_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), nullable=True)

    ticket: Mapped["Ticket"] = relationship("Ticket", back_populates="escalations")


class TicketFeedback(Base):
    __tablename__ = "ticket_feedback"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    ticket_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("tickets.id", ondelete="CASCADE"), unique=True, nullable=False
    )
    user_id: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), nullable=False)
    rating: Mapped[int] = mapped_column(Integer, nullable=False)  # 1 to 5 stars
    comments: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=get_utc_now, nullable=False
    )

    ticket: Mapped["Ticket"] = relationship("Ticket", back_populates="feedback")


# ---------------------------------------------------------------------------
# KNOWLEDGE BASE & OS GUIDES
# ---------------------------------------------------------------------------
class KnowledgeArticle(Base, TimestampMixin):
    __tablename__ = "knowledge_articles"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    title: Mapped[str] = mapped_column(String(255), nullable=False, index=True)
    category: Mapped[str] = mapped_column(String(50), nullable=False, index=True)
    os_target: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)  # Windows, macOS, iOS, Android, All
    content: Mapped[str] = mapped_column(Text, nullable=False)
    tags: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
    view_count: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    helpful_count: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    not_helpful_count: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    created_by: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), nullable=False)


# ---------------------------------------------------------------------------
# IT ONBOARDING MODULE
# ---------------------------------------------------------------------------
class OnboardingRequest(Base, TimestampMixin):
    __tablename__ = "onboarding_requests"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    employee_name: Mapped[str] = mapped_column(String(100), nullable=False)
    department: Mapped[str] = mapped_column(String(100), nullable=False)
    job_title: Mapped[str] = mapped_column(String(100), nullable=False)
    manager_name: Mapped[str] = mapped_column(String(100), nullable=False)
    start_date: Mapped[date] = mapped_column(Date, nullable=False)
    location: Mapped[str] = mapped_column(String(100), default="Headquarters", nullable=False)
    device_requirement: Mapped[str] = mapped_column(String(100), default="Standard Laptop", nullable=False)
    software_requirement: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    access_requirement: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    status: Mapped[str] = mapped_column(String(30), default="In Progress", nullable=False)
    progress_percent: Mapped[int] = mapped_column(Integer, default=0, nullable=False)
    created_by: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), nullable=False)

    tasks: Mapped[List["OnboardingTask"]] = relationship(
        "OnboardingTask", back_populates="request", cascade="all, delete-orphan", order_by="OnboardingTask.id"
    )


class OnboardingTask(Base):
    __tablename__ = "onboarding_tasks"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    request_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("onboarding_requests.id", ondelete="CASCADE"), nullable=False
    )
    task_name: Mapped[str] = mapped_column(String(200), nullable=False)
    owner_id: Mapped[Optional[str]] = mapped_column(String(36), ForeignKey("users.id"), nullable=True)
    status: Mapped[str] = mapped_column(
        String(30), default=OnboardingTaskStatus.NOT_STARTED, nullable=False
    )
    due_date: Mapped[Optional[date]] = mapped_column(Date, nullable=True)
    completed_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), nullable=True)
    notes: Mapped[Optional[str]] = mapped_column(Text, nullable=True)

    request: Mapped["OnboardingRequest"] = relationship("OnboardingRequest", back_populates="tasks")


# ---------------------------------------------------------------------------
# ACCESS & SERVICE REQUESTS
# ---------------------------------------------------------------------------
class AccessRequest(Base, TimestampMixin):
    __tablename__ = "access_requests"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    request_type: Mapped[str] = mapped_column(String(50), nullable=False)  # Password Reset, MFA Reset, VPN, Shared Drive, App Access
    requested_system: Mapped[str] = mapped_column(String(100), nullable=False)
    access_level: Mapped[str] = mapped_column(String(50), default="Read/Write", nullable=False)
    reason: Mapped[str] = mapped_column(Text, nullable=False)
    requested_by: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), nullable=False)
    approved_by: Mapped[Optional[str]] = mapped_column(String(36), ForeignKey("users.id"), nullable=True)
    assigned_engineer_id: Mapped[Optional[str]] = mapped_column(String(36), ForeignKey("users.id"), nullable=True)
    status: Mapped[str] = mapped_column(String(30), default=AccessRequestStatus.REQUESTED, nullable=False)
    completed_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), nullable=True)


# ---------------------------------------------------------------------------
# PROCESS & TECHNOLOGY IMPROVEMENT
# ---------------------------------------------------------------------------
class ImprovementRequest(Base, TimestampMixin):
    __tablename__ = "improvement_requests"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    title: Mapped[str] = mapped_column(String(200), nullable=False)
    category: Mapped[str] = mapped_column(String(50), nullable=False)  # Process, Technology, Documentation, Training
    description: Mapped[str] = mapped_column(Text, nullable=False)
    business_benefit: Mapped[str] = mapped_column(Text, nullable=False)
    status: Mapped[str] = mapped_column(String(30), default=ImprovementStatus.PROPOSED, nullable=False)
    priority: Mapped[str] = mapped_column(String(20), default=TicketPriority.MEDIUM, nullable=False)
    submitted_by: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), nullable=False)
    assigned_to: Mapped[Optional[str]] = mapped_column(String(36), ForeignKey("users.id"), nullable=True)


# ---------------------------------------------------------------------------
# AUDIT LOGS
# ---------------------------------------------------------------------------
class AuditLog(Base):
    __tablename__ = "audit_logs"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    user_id: Mapped[Optional[str]] = mapped_column(
        String(36), ForeignKey("users.id"), nullable=True, index=True
    )
    action: Mapped[str] = mapped_column(String(100), nullable=False, index=True)
    entity_type: Mapped[str] = mapped_column(String(50), nullable=False)
    entity_id: Mapped[str] = mapped_column(String(100), nullable=False)
    details: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=get_utc_now, nullable=False, index=True
    )

    user: Mapped[Optional["User"]] = relationship("User", back_populates="audit_logs")


# ---------------------------------------------------------------------------
# SERVICE REQUEST MANAGEMENT MODULE
# ---------------------------------------------------------------------------
class ServiceRequestType(Base, TimestampMixin):
    __tablename__ = "service_request_types"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    name: Mapped[str] = mapped_column(String(100), unique=True, nullable=False, index=True)
    description: Mapped[str] = mapped_column(Text, nullable=False)
    category: Mapped[str] = mapped_column(String(50), default=ServiceRequestCategory.GENERAL_IT, nullable=False, index=True)
    default_priority: Mapped[str] = mapped_column(String(20), default=TicketPriority.MEDIUM, nullable=False)
    approval_required: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    default_sla_hours: Mapped[int] = mapped_column(Integer, default=24, nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)

    service_requests: Mapped[List["ServiceRequest"]] = relationship(
        "ServiceRequest", back_populates="request_type"
    )


class ServiceRequest(Base, TimestampMixin):
    __tablename__ = "service_requests"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    request_number: Mapped[str] = mapped_column(String(20), unique=True, index=True, nullable=False)
    title: Mapped[str] = mapped_column(String(255), nullable=False)
    description: Mapped[str] = mapped_column(Text, nullable=False)
    category: Mapped[str] = mapped_column(String(50), nullable=False, index=True)
    priority: Mapped[str] = mapped_column(String(20), default=TicketPriority.MEDIUM, nullable=False, index=True)
    status: Mapped[str] = mapped_column(String(30), default=ServiceRequestStatus.SUBMITTED, nullable=False, index=True)

    # Type & Requester
    request_type_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("service_request_types.id"), nullable=False, index=True
    )
    requester_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("users.id"), nullable=False, index=True
    )

    # Approval Workflow
    approval_required: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    approval_status: Mapped[str] = mapped_column(
        String(30), default=ServiceRequestApprovalStatus.NONE, nullable=False
    )
    approver_id: Mapped[Optional[str]] = mapped_column(
        String(36), ForeignKey("users.id"), nullable=True
    )
    approval_reason: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    approved_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), nullable=True)

    # Assignment
    assigned_to: Mapped[Optional[str]] = mapped_column(
        String(36), ForeignKey("users.id"), nullable=True, index=True
    )
    assigned_team: Mapped[Optional[str]] = mapped_column(
        String(50), default=SupportTeam.SERVICE_DESK, nullable=True, index=True
    )

    # Request Context & Parameters
    business_justification: Mapped[str] = mapped_column(Text, nullable=False)
    required_date: Mapped[Optional[date]] = mapped_column(Date, nullable=True)
    asset_id: Mapped[Optional[str]] = mapped_column(
        String(36), ForeignKey("assets.id"), nullable=True, index=True
    )
    application_name: Mapped[Optional[str]] = mapped_column(String(100), nullable=True)
    access_level: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)
    software_name: Mapped[Optional[str]] = mapped_column(String(100), nullable=True)
    software_version: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)
    device_type: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)

    # Fulfillment & Resolution
    fulfillment_details: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    resolution_notes: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    due_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), nullable=False, index=True)
    fulfilled_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), nullable=True)
    closed_at: Mapped[Optional[datetime]] = mapped_column(DateTime(timezone=True), nullable=True)

    # Relationships
    request_type: Mapped["ServiceRequestType"] = relationship(
        "ServiceRequestType", back_populates="service_requests"
    )
    requester: Mapped["User"] = relationship(
        "User", back_populates="created_service_requests", foreign_keys=[requester_id]
    )
    approver: Mapped[Optional["User"]] = relationship(
        "User", back_populates="approved_service_requests", foreign_keys=[approver_id]
    )
    assignee: Mapped[Optional["User"]] = relationship(
        "User", back_populates="assigned_service_requests", foreign_keys=[assigned_to]
    )
    asset: Mapped[Optional["Asset"]] = relationship("Asset")
    comments: Mapped[List["ServiceRequestComment"]] = relationship(
        "ServiceRequestComment", back_populates="service_request", cascade="all, delete-orphan", order_by="ServiceRequestComment.created_at"
    )
    timeline: Mapped[List["ServiceRequestTimeline"]] = relationship(
        "ServiceRequestTimeline", back_populates="service_request", cascade="all, delete-orphan", order_by="ServiceRequestTimeline.created_at"
    )

    __table_args__ = (
        Index("ix_service_requests_created_at", "created_at"),
        Index("ix_service_requests_status_priority", "status", "priority"),
    )


class ServiceRequestComment(Base):
    __tablename__ = "service_request_comments"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    request_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("service_requests.id", ondelete="CASCADE"), nullable=False, index=True
    )
    user_id: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), nullable=False)
    comment: Mapped[str] = mapped_column(Text, nullable=False)
    is_internal: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=get_utc_now, nullable=False, index=True
    )

    service_request: Mapped["ServiceRequest"] = relationship("ServiceRequest", back_populates="comments")
    user: Mapped["User"] = relationship("User", back_populates="service_request_comments")


class ServiceRequestTimeline(Base):
    __tablename__ = "service_request_timeline"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=generate_uuid)
    request_id: Mapped[str] = mapped_column(
        String(36), ForeignKey("service_requests.id", ondelete="CASCADE"), nullable=False, index=True
    )
    event_type: Mapped[str] = mapped_column(String(50), nullable=False)
    title: Mapped[str] = mapped_column(String(150), nullable=False)
    description: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
    actor_id: Mapped[str] = mapped_column(String(36), ForeignKey("users.id"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True), default=get_utc_now, nullable=False, index=True
    )

    service_request: Mapped["ServiceRequest"] = relationship("ServiceRequest", back_populates="timeline")
    actor: Mapped["User"] = relationship("User")
