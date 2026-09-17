from typing import Any, Dict, List, Optional
from datetime import datetime, date
from pydantic import BaseModel, ConfigDict, EmailStr, Field


# ---------------------------------------------------------------------------
# AUTH & USERS
# ---------------------------------------------------------------------------
class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
    user: "UserOut"


class LoginRequest(BaseModel):
    email: EmailStr
    password: str


class RegisterRequest(BaseModel):
    name: str = Field(..., min_length=2, max_length=100)
    email: EmailStr
    password: str = Field(..., min_length=6)
    department: Optional[str] = "General"
    job_title: Optional[str] = "Staff"
    phone: Optional[str] = None
    location: Optional[str] = "Headquarters"


class UserCreate(BaseModel):
    name: str
    email: EmailStr
    password: str
    role: str = "EMPLOYEE"
    department: str = "General"
    job_title: Optional[str] = None
    phone: Optional[str] = None
    location: Optional[str] = "Headquarters"


class UserUpdate(BaseModel):
    name: Optional[str] = None
    role: Optional[str] = None
    department: Optional[str] = None
    job_title: Optional[str] = None
    phone: Optional[str] = None
    location: Optional[str] = None
    is_active: Optional[bool] = None


class UserOut(BaseModel):
    id: str
    name: str
    email: str
    role: str
    department: str
    job_title: Optional[str] = None
    phone: Optional[str] = None
    location: Optional[str] = None
    is_active: bool
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


# ---------------------------------------------------------------------------
# CATEGORIES
# ---------------------------------------------------------------------------
class CategoryCreate(BaseModel):
    name: str
    description: Optional[str] = None


class CategoryUpdate(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    is_active: Optional[bool] = None


class CategoryOut(BaseModel):
    id: str
    name: str
    description: Optional[str] = None
    is_active: bool
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


# ---------------------------------------------------------------------------
# ASSETS / HARDWARE
# ---------------------------------------------------------------------------
class AssetCreate(BaseModel):
    asset_tag: str
    device_type: str
    manufacturer: str
    model: str
    serial_number: str
    status: Optional[str] = "In Use"
    assigned_user_id: Optional[str] = None
    purchase_date: Optional[date] = None
    warranty_expiry: Optional[date] = None
    notes: Optional[str] = None


class AssetUpdate(BaseModel):
    asset_tag: Optional[str] = None
    device_type: Optional[str] = None
    manufacturer: Optional[str] = None
    model: Optional[str] = None
    serial_number: Optional[str] = None
    status: Optional[str] = None
    assigned_user_id: Optional[str] = None
    purchase_date: Optional[date] = None
    warranty_expiry: Optional[date] = None
    notes: Optional[str] = None


class AssetOut(BaseModel):
    id: str
    asset_tag: str
    device_type: str
    manufacturer: str
    model: str
    serial_number: str
    status: str
    assigned_user_id: Optional[str] = None
    assigned_user_name: Optional[str] = None
    purchase_date: Optional[date] = None
    warranty_expiry: Optional[date] = None
    notes: Optional[str] = None
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


# ---------------------------------------------------------------------------
# TICKETS & INCIDENTS
# ---------------------------------------------------------------------------
class TicketCreate(BaseModel):
    title: str = Field(..., min_length=5, max_length=255)
    description: str = Field(..., min_length=10)
    category_id: str
    impact: str = "Individual"
    urgency: str = "Medium"
    priority: Optional[str] = None  # Auto-calculated if not provided
    asset_id: Optional[str] = None
    device_type: Optional[str] = None
    os_name: Optional[str] = None
    application_name: Optional[str] = None
    location: Optional[str] = None


class TicketAssign(BaseModel):
    assigned_to: str


class TicketStatusUpdate(BaseModel):
    status: str
    reason: Optional[str] = None


class TicketPriorityOverride(BaseModel):
    priority: str
    reason: str


class TicketEscalate(BaseModel):
    to_engineer: Optional[str] = None
    reason: str
    escalation_level: int = 2


class TicketResolve(BaseModel):
    resolution_notes: str = Field(..., min_length=5)


class TicketCommentCreate(BaseModel):
    comment: str = Field(..., min_length=1)
    is_internal: bool = False


class TicketChecklistUpdate(BaseModel):
    checklist_state: str  # JSON string of checked items e.g. '{"charger": true}'


class TicketFeedbackCreate(BaseModel):
    rating: int = Field(..., ge=1, le=5)
    comments: Optional[str] = None


class TicketCommentOut(BaseModel):
    id: str
    ticket_id: str
    user_id: str
    user_name: Optional[str] = None
    user_role: Optional[str] = None
    comment: str
    is_internal: bool
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class TicketStatusHistoryOut(BaseModel):
    id: str
    old_status: str
    new_status: str
    changed_by_name: Optional[str] = None
    changed_at: datetime
    reason: Optional[str] = None

    model_config = ConfigDict(from_attributes=True)


class TicketEscalationOut(BaseModel):
    id: str
    from_engineer_name: Optional[str] = None
    to_engineer_name: Optional[str] = None
    reason: str
    escalation_level: int
    created_at: datetime
    resolved_at: Optional[datetime] = None

    model_config = ConfigDict(from_attributes=True)


class TicketFeedbackOut(BaseModel):
    id: str
    rating: int
    comments: Optional[str] = None
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class TicketOut(BaseModel):
    id: str
    ticket_number: str
    title: str
    description: str
    category_id: str
    category_name: Optional[str] = None
    priority: str
    status: str
    impact: str
    urgency: str
    priority_override_reason: Optional[str] = None
    asset_id: Optional[str] = None
    asset: Optional[AssetOut] = None
    device_type: Optional[str] = None
    os_name: Optional[str] = None
    application_name: Optional[str] = None
    location: Optional[str] = None
    created_by: str
    creator_name: Optional[str] = None
    creator_email: Optional[str] = None
    creator_department: Optional[str] = None
    assigned_to: Optional[str] = None
    assignee_name: Optional[str] = None
    created_at: datetime
    updated_at: datetime
    due_at: datetime
    first_response_at: Optional[datetime] = None
    resolved_at: Optional[datetime] = None
    closed_at: Optional[datetime] = None
    resolution_notes: Optional[str] = None
    is_escalated: bool
    escalation_level: int
    checklist_state: Optional[str] = None
    sla_status: Optional[str] = None  # "Within SLA", "At Risk", "Breached"
    feedback: Optional[TicketFeedbackOut] = None
    comments: Optional[List[TicketCommentOut]] = []
    status_history: Optional[List[TicketStatusHistoryOut]] = []
    escalations: Optional[List[TicketEscalationOut]] = []

    model_config = ConfigDict(from_attributes=True)


class TicketListResponse(BaseModel):
    items: List[TicketOut]
    total: int
    page: int
    page_size: int
    total_pages: int


# ---------------------------------------------------------------------------
# KNOWLEDGE BASE
# ---------------------------------------------------------------------------
class KnowledgeArticleCreate(BaseModel):
    title: str
    category: str
    os_target: Optional[str] = "All"
    content: str
    tags: Optional[str] = None


class KnowledgeArticleUpdate(BaseModel):
    title: Optional[str] = None
    category: Optional[str] = None
    os_target: Optional[str] = None
    content: Optional[str] = None
    tags: Optional[str] = None


class KnowledgeArticleOut(BaseModel):
    id: str
    title: str
    category: str
    os_target: Optional[str] = None
    content: str
    tags: Optional[str] = None
    view_count: int
    helpful_count: int
    not_helpful_count: int
    created_by: str
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


# ---------------------------------------------------------------------------
# IT ONBOARDING
# ---------------------------------------------------------------------------
class OnboardingRequestCreate(BaseModel):
    employee_name: str
    department: str
    job_title: str
    manager_name: str
    start_date: date
    location: Optional[str] = "Headquarters"
    device_requirement: Optional[str] = "Standard Laptop"
    software_requirement: Optional[str] = "Microsoft 365, Slack, VPN"
    access_requirement: Optional[str] = "Email, Shared Drives, HR Portal"


class OnboardingTaskUpdate(BaseModel):
    status: Optional[str] = None
    notes: Optional[str] = None
    owner_id: Optional[str] = None


class OnboardingTaskOut(BaseModel):
    id: str
    request_id: str
    task_name: str
    owner_id: Optional[str] = None
    owner_name: Optional[str] = None
    status: str
    due_date: Optional[date] = None
    completed_at: Optional[datetime] = None
    notes: Optional[str] = None

    model_config = ConfigDict(from_attributes=True)


class OnboardingRequestOut(BaseModel):
    id: str
    employee_name: str
    department: str
    job_title: str
    manager_name: str
    start_date: date
    location: str
    device_requirement: str
    software_requirement: Optional[str] = None
    access_requirement: Optional[str] = None
    status: str
    progress_percent: int
    created_by: str
    created_at: datetime
    tasks: List[OnboardingTaskOut] = []

    model_config = ConfigDict(from_attributes=True)


# ---------------------------------------------------------------------------
# ACCESS REQUESTS
# ---------------------------------------------------------------------------
class AccessRequestCreate(BaseModel):
    request_type: str
    requested_system: str
    access_level: Optional[str] = "Read/Write"
    reason: str


class AccessRequestStatusUpdate(BaseModel):
    status: str
    approved_by: Optional[str] = None


class AccessRequestOut(BaseModel):
    id: str
    request_type: str
    requested_system: str
    access_level: str
    reason: str
    requested_by: str
    requester_name: Optional[str] = None
    approved_by: Optional[str] = None
    approver_name: Optional[str] = None
    status: str
    completed_at: Optional[datetime] = None
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


# ---------------------------------------------------------------------------
# PROCESS & TECHNOLOGY IMPROVEMENTS
# ---------------------------------------------------------------------------
class ImprovementCreate(BaseModel):
    title: str
    category: str
    description: str
    business_benefit: str
    priority: Optional[str] = "Medium"


class ImprovementUpdate(BaseModel):
    status: Optional[str] = None
    priority: Optional[str] = None
    assigned_to: Optional[str] = None


class ImprovementOut(BaseModel):
    id: str
    title: str
    category: str
    description: str
    business_benefit: str
    status: str
    priority: str
    submitted_by: str
    submitter_name: Optional[str] = None
    assigned_to: Optional[str] = None
    assignee_name: Optional[str] = None
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


# ---------------------------------------------------------------------------
# AUDIT LOGS
# ---------------------------------------------------------------------------
class AuditLogOut(BaseModel):
    id: str
    user_id: Optional[str] = None
    user_name: Optional[str] = None
    action: str
    entity_type: str
    entity_id: str
    details: Optional[str] = None
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


# ---------------------------------------------------------------------------
# DASHBOARDS & OPERATIONAL ANALYTICS
# ---------------------------------------------------------------------------
class DashboardSummaryOut(BaseModel):
    total_tickets: int
    open_tickets: int
    in_progress_tickets: int
    waiting_tickets: int
    resolved_tickets: int
    closed_tickets: int
    critical_tickets: int
    sla_breached: int
    sla_at_risk: int
    average_resolution_hours: float
    average_first_response_hours: float
    csat_average_rating: float
    total_assets: int
    active_onboardings: int
    pending_access_requests: int
    status_distribution: Dict[str, int]
    priority_distribution: Dict[str, int]
    category_distribution: Dict[str, int]
    os_distribution: Dict[str, int]
    daily_trend: List[Dict[str, Any]]


# ---------------------------------------------------------------------------
# SERVICE REQUEST SCHEMAS
# ---------------------------------------------------------------------------
class ServiceRequestTypeBase(BaseModel):
    name: str = Field(..., min_length=2, max_length=100)
    description: str
    category: str = "General IT"
    default_priority: str = "Medium"
    approval_required: bool = False
    default_sla_hours: int = 24
    is_active: bool = True


class ServiceRequestTypeCreate(ServiceRequestTypeBase):
    pass


class ServiceRequestTypeUpdate(BaseModel):
    name: Optional[str] = None
    description: Optional[str] = None
    category: Optional[str] = None
    default_priority: Optional[str] = None
    approval_required: Optional[bool] = None
    default_sla_hours: Optional[int] = None
    is_active: Optional[bool] = None


class ServiceRequestTypeOut(ServiceRequestTypeBase):
    id: str
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)


class ServiceRequestCommentCreate(BaseModel):
    comment: str = Field(..., min_length=1)
    is_internal: bool = False


class ServiceRequestCommentOut(BaseModel):
    id: str
    request_id: str
    user_id: str
    user_name: Optional[str] = None
    user_role: Optional[str] = None
    comment: str
    is_internal: bool
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class ServiceRequestTimelineOut(BaseModel):
    id: str
    request_id: str
    event_type: str
    title: str
    description: Optional[str] = None
    actor_id: str
    actor_name: Optional[str] = None
    actor_role: Optional[str] = None
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class ServiceRequestCreate(BaseModel):
    request_type_id: str
    title: str = Field(..., min_length=3, max_length=255)
    description: str = Field(..., min_length=5)
    priority: Optional[str] = None  # Will default to request_type default_priority if None
    business_justification: str = Field(..., min_length=5)
    required_date: Optional[date] = None
    asset_id: Optional[str] = None
    application_name: Optional[str] = None
    access_level: Optional[str] = None
    software_name: Optional[str] = None
    software_version: Optional[str] = None
    device_type: Optional[str] = None


class ServiceRequestUpdate(BaseModel):
    title: Optional[str] = None
    description: Optional[str] = None
    priority: Optional[str] = None
    business_justification: Optional[str] = None
    required_date: Optional[date] = None
    application_name: Optional[str] = None
    access_level: Optional[str] = None
    software_name: Optional[str] = None
    software_version: Optional[str] = None
    device_type: Optional[str] = None


class ServiceRequestApproveAction(BaseModel):
    approved: bool
    reason: Optional[str] = None


class ServiceRequestRejectAction(BaseModel):
    reason: str = Field(..., min_length=3, description="Rejection reason is mandatory")


class ServiceRequestAssignAction(BaseModel):
    assigned_to: Optional[str] = None
    assigned_team: Optional[str] = None


class ServiceRequestStatusAction(BaseModel):
    status: str
    reason: Optional[str] = None


class ServiceRequestFulfillAction(BaseModel):
    fulfillment_details: Dict[str, Any] = Field(..., description="Type-specific verified completion info")
    resolution_notes: Optional[str] = None


class ServiceRequestConfirmAction(BaseModel):
    confirmed: bool = True
    feedback_notes: Optional[str] = None


class ServiceRequestReportProblemAction(BaseModel):
    problem_description: str = Field(..., min_length=5)


class ServiceRequestCancelAction(BaseModel):
    reason: Optional[str] = None


class ServiceRequestOut(BaseModel):
    id: str
    request_number: str
    title: str
    description: str
    category: str
    priority: str
    status: str
    request_type_id: str
    request_type_name: Optional[str] = None
    requester_id: str
    requester_name: Optional[str] = None
    requester_email: Optional[str] = None
    requester_department: Optional[str] = None
    approval_required: bool
    approval_status: str
    approver_id: Optional[str] = None
    approver_name: Optional[str] = None
    approval_reason: Optional[str] = None
    approved_at: Optional[datetime] = None
    assigned_to: Optional[str] = None
    assigned_to_name: Optional[str] = None
    assigned_team: Optional[str] = None
    business_justification: str
    required_date: Optional[date] = None
    asset_id: Optional[str] = None
    asset_tag: Optional[str] = None
    asset_model: Optional[str] = None
    application_name: Optional[str] = None
    access_level: Optional[str] = None
    software_name: Optional[str] = None
    software_version: Optional[str] = None
    device_type: Optional[str] = None
    fulfillment_details: Optional[str] = None
    resolution_notes: Optional[str] = None
    due_at: datetime
    created_at: datetime
    updated_at: datetime
    fulfilled_at: Optional[datetime] = None
    closed_at: Optional[datetime] = None
    sla_status: Optional[str] = "Within SLA"
    is_breached: Optional[bool] = False
    comments: Optional[List[ServiceRequestCommentOut]] = []
    timeline: Optional[List[ServiceRequestTimelineOut]] = []

    model_config = ConfigDict(from_attributes=True)


class ServiceRequestListResponse(BaseModel):
    items: List[ServiceRequestOut]
    total: int
    page: int
    page_size: int
    pages: int


class UnifiedWorkItemOut(BaseModel):
    id: str
    item_type: str  # "Incident" | "Service Request"
    reference_number: str  # "INC-2026-XXXXXX" | "SR-2026-XXXXXX"
    title: str
    category: str
    requester_id: str
    requester_name: str
    requester_department: str
    priority: str
    status: str
    assigned_to_id: Optional[str] = None
    assigned_to_name: Optional[str] = None
    assigned_team: Optional[str] = None
    sla_status: str
    is_breached: bool
    due_at: datetime
    created_at: datetime
    updated_at: datetime


class ServiceRequestDashboardOut(BaseModel):
    total_requests: int
    pending_approval: int
    unassigned: int
    in_progress: int
    fulfilled: int
    closed: int
    sla_breached: int
    sla_at_risk: int
    sla_compliance_rate: float
    average_fulfillment_hours: float
    by_type: Dict[str, int]
    by_category: Dict[str, int]
    by_status: Dict[str, int]
    by_team: Dict[str, int]
    daily_trend: List[Dict[str, Any]]

