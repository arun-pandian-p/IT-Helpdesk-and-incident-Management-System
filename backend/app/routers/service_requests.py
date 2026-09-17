import json
from datetime import datetime, timedelta, timezone
from typing import Any, Dict, List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import desc, func, or_
from sqlalchemy.orm import Session

from app.db.base import get_utc_now, ensure_utc
from app.db.database import get_db
from app.db.models import (
    Asset,
    ServiceRequest,
    ServiceRequestApprovalStatus,
    ServiceRequestCategory,
    ServiceRequestComment,
    ServiceRequestStatus,
    ServiceRequestTimeline,
    ServiceRequestType,
    SupportTeam,
    Ticket,
    TicketPriority,
    TicketStatus,
    User,
    UserRole,
)
from app.dependencies.auth import get_current_user, require_admin, require_support_or_admin
from app.schemas import (
    ServiceRequestApproveAction,
    ServiceRequestAssignAction,
    ServiceRequestCancelAction,
    ServiceRequestCommentCreate,
    ServiceRequestCommentOut,
    ServiceRequestConfirmAction,
    ServiceRequestCreate,
    ServiceRequestDashboardOut,
    ServiceRequestFulfillAction,
    ServiceRequestListResponse,
    ServiceRequestOut,
    ServiceRequestRejectAction,
    ServiceRequestReportProblemAction,
    ServiceRequestStatusAction,
    ServiceRequestTimelineOut,
    ServiceRequestTypeCreate,
    ServiceRequestTypeOut,
    ServiceRequestTypeUpdate,
    ServiceRequestUpdate,
    UnifiedWorkItemOut,
)
from app.services.audit_service import log_audit
from app.services.service_request_service import (
    calculate_sr_sla_status,
    confirm_service_request,
    fulfill_service_request,
    generate_service_request_number,
    get_sr_dashboard_metrics,
    record_sr_timeline,
    report_service_request_problem,
    validate_status_transition,
)

router = APIRouter(tags=["Service Requests"])


# ---------------------------------------------------------------------------
# SERVICE REQUEST TYPES (CONFIGURATION)
# ---------------------------------------------------------------------------
@router.get("/service-request-types", response_model=List[ServiceRequestTypeOut])
def get_request_types(
    category: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Retrieves all active service request catalog types."""
    query = db.query(ServiceRequestType).filter(ServiceRequestType.is_active == True)
    if category:
        query = query.filter(ServiceRequestType.category == category)
    return query.order_by(ServiceRequestType.category, ServiceRequestType.name).all()


@router.post("/service-request-types", response_model=ServiceRequestTypeOut, status_code=status.HTTP_201_CREATED)
def create_request_type(
    req_type: ServiceRequestTypeCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    """Admin endpoint to create a new service request catalog type."""
    existing = db.query(ServiceRequestType).filter(ServiceRequestType.name.ilike(req_type.name)).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Service request type '{req_type.name}' already exists.",
        )

    new_type = ServiceRequestType(
        name=req_type.name,
        description=req_type.description,
        category=req_type.category,
        default_priority=req_type.default_priority,
        approval_required=req_type.approval_required,
        default_sla_hours=req_type.default_sla_hours,
        is_active=req_type.is_active,
    )
    db.add(new_type)
    db.commit()
    db.refresh(new_type)
    return new_type


@router.patch("/service-request-types/{id}", response_model=ServiceRequestTypeOut)
def update_request_type(
    id: str,
    update_data: ServiceRequestTypeUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    """Admin endpoint to configure an existing request type."""
    req_type = db.query(ServiceRequestType).filter(ServiceRequestType.id == id).first()
    if not req_type:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Request type not found.")

    for field, value in update_data.model_dump(exclude_unset=True).items():
        setattr(req_type, field, value)

    db.commit()
    db.refresh(req_type)
    return req_type


# ---------------------------------------------------------------------------
# SERVICE REQUESTS: LIST & SEARCH
# ---------------------------------------------------------------------------
@router.get("/service-requests", response_model=ServiceRequestListResponse)
def list_service_requests(
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    search: Optional[str] = None,
    category: Optional[str] = None,
    request_type_id: Optional[str] = None,
    priority: Optional[str] = None,
    status_filter: Optional[str] = Query(None, alias="status"),
    approval_status: Optional[str] = None,
    assigned_team: Optional[str] = None,
    assigned_to: Optional[str] = None,
    sla_status: Optional[str] = None,
    view: Optional[str] = None,  # 'my', 'approvals', 'assigned', 'all'
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Lists service requests with role-based visibility and comprehensive filtering."""
    query = db.query(ServiceRequest)

    # Role visibility: Employees only see their own unless viewing pending approvals
    if current_user.role == UserRole.EMPLOYEE:
        if view == "approvals":
            query = query.filter(
                ServiceRequest.approval_required == True,
                ServiceRequest.approval_status == ServiceRequestApprovalStatus.PENDING,
                ServiceRequest.requester_id != current_user.id,
            )
        else:
            query = query.filter(ServiceRequest.requester_id == current_user.id)
    else:
        # Support / Admin role views
        if view == "my":
            query = query.filter(ServiceRequest.requester_id == current_user.id)
        elif view == "assigned":
            query = query.filter(ServiceRequest.assigned_to == current_user.id)
        elif view == "approvals":
            query = query.filter(
                ServiceRequest.approval_required == True,
                ServiceRequest.approval_status == ServiceRequestApprovalStatus.PENDING,
            )

    # Applied filters
    if search:
        search_pattern = f"%{search}%"
        query = query.join(ServiceRequest.requester).filter(
            or_(
                ServiceRequest.request_number.ilike(search_pattern),
                ServiceRequest.title.ilike(search_pattern),
                ServiceRequest.description.ilike(search_pattern),
                ServiceRequest.application_name.ilike(search_pattern),
                User.name.ilike(search_pattern),
            )
        )

    if category:
        query = query.filter(ServiceRequest.category == category)
    if request_type_id:
        query = query.filter(ServiceRequest.request_type_id == request_type_id)
    if priority:
        query = query.filter(ServiceRequest.priority == priority)
    if status_filter:
        query = query.filter(ServiceRequest.status == status_filter)
    if approval_status:
        query = query.filter(ServiceRequest.approval_status == approval_status)
    if assigned_team:
        query = query.filter(ServiceRequest.assigned_team == assigned_team)
    if assigned_to:
        query = query.filter(ServiceRequest.assigned_to == assigned_to)

    total = query.count()
    items = query.order_by(desc(ServiceRequest.created_at)).offset((page - 1) * page_size).limit(page_size).all()

    # Map to schema with SLA and names
    now = get_utc_now()
    results = []
    for sr in items:
        status_name, is_breached = calculate_sr_sla_status(sr, now)
        if sla_status and status_name != sla_status:
            continue

        item_out = ServiceRequestOut(
            id=sr.id,
            request_number=sr.request_number,
            title=sr.title,
            description=sr.description,
            category=sr.category,
            priority=sr.priority,
            status=sr.status,
            request_type_id=sr.request_type_id,
            request_type_name=sr.request_type.name if sr.request_type else None,
            requester_id=sr.requester_id,
            requester_name=sr.requester.name if sr.requester else None,
            requester_email=sr.requester.email if sr.requester else None,
            requester_department=sr.requester.department if sr.requester else None,
            approval_required=sr.approval_required,
            approval_status=sr.approval_status,
            approver_id=sr.approver_id,
            approver_name=sr.approver.name if sr.approver else None,
            approval_reason=sr.approval_reason,
            approved_at=sr.approved_at,
            assigned_to=sr.assigned_to,
            assigned_to_name=sr.assignee.name if sr.assignee else None,
            assigned_team=sr.assigned_team,
            business_justification=sr.business_justification,
            required_date=sr.required_date,
            asset_id=sr.asset_id,
            asset_tag=sr.asset.asset_tag if sr.asset else None,
            asset_model=sr.asset.model if sr.asset else None,
            application_name=sr.application_name,
            access_level=sr.access_level,
            software_name=sr.software_name,
            software_version=sr.software_version,
            device_type=sr.device_type,
            fulfillment_details=sr.fulfillment_details,
            resolution_notes=sr.resolution_notes,
            due_at=sr.due_at,
            created_at=sr.created_at,
            updated_at=sr.updated_at,
            fulfilled_at=sr.fulfilled_at,
            closed_at=sr.closed_at,
            sla_status=status_name,
            is_breached=is_breached,
            comments=[],
            timeline=[],
        )
        results.append(item_out)

    pages = (total + page_size - 1) // page_size
    return ServiceRequestListResponse(items=results, total=total, page=page, page_size=page_size, pages=pages)


# ---------------------------------------------------------------------------
# SERVICE REQUESTS: CREATE
# ---------------------------------------------------------------------------
@router.post("/service-requests", response_model=ServiceRequestOut, status_code=status.HTTP_201_CREATED)
def create_service_request(
    request_data: ServiceRequestCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Creates a new service request with automated SR-YYYY-XXXXXX numbering and SLA calculation."""
    req_type = db.query(ServiceRequestType).filter(ServiceRequestType.id == request_data.request_type_id).first()
    if not req_type:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Specified service request type not found.",
        )

    now = get_utc_now()
    sr_number = generate_service_request_number(db)
    priority = request_data.priority or req_type.default_priority

    # Calculate SLA due date
    due_at = now + timedelta(hours=req_type.default_sla_hours)

    # Initial status based on approval requirement
    if req_type.approval_required:
        initial_status = ServiceRequestStatus.PENDING_APPROVAL
        approval_status = ServiceRequestApprovalStatus.PENDING
    else:
        initial_status = ServiceRequestStatus.SUBMITTED
        approval_status = ServiceRequestApprovalStatus.NONE

    sr = ServiceRequest(
        request_number=sr_number,
        title=request_data.title,
        description=request_data.description,
        category=req_type.category,
        priority=priority,
        status=initial_status,
        request_type_id=req_type.id,
        requester_id=current_user.id,
        approval_required=req_type.approval_required,
        approval_status=approval_status,
        assigned_team=SupportTeam.SERVICE_DESK,
        business_justification=request_data.business_justification,
        required_date=request_data.required_date,
        asset_id=request_data.asset_id,
        application_name=request_data.application_name,
        access_level=request_data.access_level,
        software_name=request_data.software_name,
        software_version=request_data.software_version,
        device_type=request_data.device_type,
        due_at=due_at,
    )
    db.add(sr)
    db.flush()

    # Create timeline entry
    record_sr_timeline(
        db,
        request_id=sr.id,
        event_type="Submitted",
        title="Service Request Submitted",
        actor_id=current_user.id,
        description=f"Submitted by {current_user.name} ({current_user.department}).",
    )

    if req_type.approval_required:
        record_sr_timeline(
            db,
            request_id=sr.id,
            event_type="Approval Requested",
            title="Management Approval Pending",
            actor_id=current_user.id,
            description=f"Awaiting approval for {req_type.name}.",
        )

    log_audit(
        db,
        action="SERVICE_REQUEST_CREATED",
        entity_type="ServiceRequest",
        entity_id=sr.id,
        user_id=current_user.id,
        details={
            "request_number": sr.request_number,
            "title": sr.title,
            "type": req_type.name,
            "priority": sr.priority,
            "approval_required": sr.approval_required,
        },
    )

    db.commit()
    db.refresh(sr)

    status_name, is_breached = calculate_sr_sla_status(sr, now)
    return ServiceRequestOut(
        id=sr.id,
        request_number=sr.request_number,
        title=sr.title,
        description=sr.description,
        category=sr.category,
        priority=sr.priority,
        status=sr.status,
        request_type_id=sr.request_type_id,
        request_type_name=req_type.name,
        requester_id=sr.requester_id,
        requester_name=current_user.name,
        requester_email=current_user.email,
        requester_department=current_user.department,
        approval_required=sr.approval_required,
        approval_status=sr.approval_status,
        assigned_team=sr.assigned_team,
        business_justification=sr.business_justification,
        required_date=sr.required_date,
        asset_id=sr.asset_id,
        application_name=sr.application_name,
        access_level=sr.access_level,
        software_name=sr.software_name,
        software_version=sr.software_version,
        device_type=sr.device_type,
        due_at=sr.due_at,
        created_at=sr.created_at,
        updated_at=sr.updated_at,
        sla_status=status_name,
        is_breached=is_breached,
        comments=[],
        timeline=[],
    )


# ---------------------------------------------------------------------------
# SERVICE REQUEST DETAIL & ACTIONS
# ---------------------------------------------------------------------------
@router.get("/service-requests/{id}", response_model=ServiceRequestOut)
def get_service_request_detail(
    id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Retrieves full service request details, comments, and chronological timeline."""
    sr = db.query(ServiceRequest).filter(ServiceRequest.id == id).first()
    if not sr:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Service request not found.")

    # RBAC check: Employee cannot view another employee's request
    if current_user.role == UserRole.EMPLOYEE and sr.requester_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not authorized to view this service request.",
        )

    now = get_utc_now()
    status_name, is_breached = calculate_sr_sla_status(sr, now)

    # Filter internal comments for employees
    comments_out = []
    for c in sr.comments:
        if c.is_internal and current_user.role == UserRole.EMPLOYEE:
            continue
        comments_out.append(
            ServiceRequestCommentOut(
                id=c.id,
                request_id=c.request_id,
                user_id=c.user_id,
                user_name=c.user.name if c.user else "User",
                user_role=c.user.role if c.user else "EMPLOYEE",
                comment=c.comment,
                is_internal=c.is_internal,
                created_at=c.created_at,
            )
        )

    timeline_out = [
        ServiceRequestTimelineOut(
            id=t.id,
            request_id=t.request_id,
            event_type=t.event_type,
            title=t.title,
            description=t.description,
            actor_id=t.actor_id,
            actor_name=t.actor.name if t.actor else "System",
            actor_role=t.actor.role if t.actor else None,
            created_at=t.created_at,
        )
        for t in sr.timeline
    ]

    return ServiceRequestOut(
        id=sr.id,
        request_number=sr.request_number,
        title=sr.title,
        description=sr.description,
        category=sr.category,
        priority=sr.priority,
        status=sr.status,
        request_type_id=sr.request_type_id,
        request_type_name=sr.request_type.name if sr.request_type else None,
        requester_id=sr.requester_id,
        requester_name=sr.requester.name if sr.requester else None,
        requester_email=sr.requester.email if sr.requester else None,
        requester_department=sr.requester.department if sr.requester else None,
        approval_required=sr.approval_required,
        approval_status=sr.approval_status,
        approver_id=sr.approver_id,
        approver_name=sr.approver.name if sr.approver else None,
        approval_reason=sr.approval_reason,
        approved_at=sr.approved_at,
        assigned_to=sr.assigned_to,
        assigned_to_name=sr.assignee.name if sr.assignee else None,
        assigned_team=sr.assigned_team,
        business_justification=sr.business_justification,
        required_date=sr.required_date,
        asset_id=sr.asset_id,
        asset_tag=sr.asset.asset_tag if sr.asset else None,
        asset_model=sr.asset.model if sr.asset else None,
        application_name=sr.application_name,
        access_level=sr.access_level,
        software_name=sr.software_name,
        software_version=sr.software_version,
        device_type=sr.device_type,
        fulfillment_details=sr.fulfillment_details,
        resolution_notes=sr.resolution_notes,
        due_at=sr.due_at,
        created_at=sr.created_at,
        updated_at=sr.updated_at,
        fulfilled_at=sr.fulfilled_at,
        closed_at=sr.closed_at,
        sla_status=status_name,
        is_breached=is_breached,
        comments=comments_out,
        timeline=timeline_out,
    )


@router.post("/service-requests/{id}/approve", response_model=ServiceRequestOut)
def approve_service_request(
    id: str,
    action: ServiceRequestApproveAction,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_support_or_admin),
):
    """Approves a service request. Enforces separation of duties (requester cannot approve own request)."""
    sr = db.query(ServiceRequest).filter(ServiceRequest.id == id).first()
    if not sr:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Service request not found.")

    if not sr.approval_required or sr.approval_status != ServiceRequestApprovalStatus.PENDING:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="This request is not currently pending approval.",
        )

    # Separation of duties: User cannot approve their own request
    if sr.requester_id == current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Separation of Duties violation: You cannot approve your own service request.",
        )

    now = get_utc_now()
    sr.approval_status = ServiceRequestApprovalStatus.APPROVED
    sr.approver_id = current_user.id
    sr.approved_at = now
    sr.approval_reason = action.reason
    sr.status = ServiceRequestStatus.APPROVED

    record_sr_timeline(
        db,
        request_id=sr.id,
        event_type="Approved",
        title="Service Request Approved",
        actor_id=current_user.id,
        description=f"Approved by {current_user.name}. {action.reason or ''}",
    )

    log_audit(
        db,
        action="SERVICE_REQUEST_APPROVED",
        entity_type="ServiceRequest",
        entity_id=sr.id,
        user_id=current_user.id,
        details={"request_number": sr.request_number, "reason": action.reason},
    )

    db.commit()
    db.refresh(sr)
    return get_service_request_detail(id, db, current_user)


@router.post("/service-requests/{id}/reject", response_model=ServiceRequestOut)
def reject_service_request(
    id: str,
    action: ServiceRequestRejectAction,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_support_or_admin),
):
    """Rejects a service request. Rejection reason is strictly required."""
    sr = db.query(ServiceRequest).filter(ServiceRequest.id == id).first()
    if not sr:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Service request not found.")

    if sr.approval_status != ServiceRequestApprovalStatus.PENDING:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Only requests in 'Pending Approval' state can be rejected.",
        )

    now = get_utc_now()
    sr.approval_status = ServiceRequestApprovalStatus.REJECTED
    sr.approver_id = current_user.id
    sr.approved_at = now
    sr.approval_reason = action.reason
    sr.status = ServiceRequestStatus.REJECTED

    record_sr_timeline(
        db,
        request_id=sr.id,
        event_type="Rejected",
        title="Service Request Rejected",
        actor_id=current_user.id,
        description=f"Rejected by {current_user.name}. Reason: {action.reason}",
    )

    log_audit(
        db,
        action="SERVICE_REQUEST_REJECTED",
        entity_type="ServiceRequest",
        entity_id=sr.id,
        user_id=current_user.id,
        details={"request_number": sr.request_number, "reason": action.reason},
    )

    db.commit()
    db.refresh(sr)
    return get_service_request_detail(id, db, current_user)


@router.post("/service-requests/{id}/assign", response_model=ServiceRequestOut)
def assign_service_request(
    id: str,
    action: ServiceRequestAssignAction,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_support_or_admin),
):
    """Assigns service request to a support engineer and/or support team."""
    sr = db.query(ServiceRequest).filter(ServiceRequest.id == id).first()
    if not sr:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Service request not found.")

    if action.assigned_to:
        target_user = db.query(User).filter(User.id == action.assigned_to).first()
        if not target_user:
            raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Assigned engineer not found.")
        sr.assigned_to = target_user.id

    if action.assigned_team:
        sr.assigned_team = action.assigned_team

    # If in Submitted or Approved status, move to Assigned
    if sr.status in [ServiceRequestStatus.SUBMITTED, ServiceRequestStatus.APPROVED]:
        sr.status = ServiceRequestStatus.ASSIGNED

    assigned_engineer_name = sr.assignee.name if sr.assignee else "None"
    record_sr_timeline(
        db,
        request_id=sr.id,
        event_type="Assigned",
        title=f"Assigned to {sr.assigned_team}",
        actor_id=current_user.id,
        description=f"Assigned engineer: {assigned_engineer_name} | Team: {sr.assigned_team}",
    )

    log_audit(
        db,
        action="SERVICE_REQUEST_ASSIGNED",
        entity_type="ServiceRequest",
        entity_id=sr.id,
        user_id=current_user.id,
        details={"engineer": assigned_engineer_name, "team": sr.assigned_team},
    )

    db.commit()
    db.refresh(sr)
    return get_service_request_detail(id, db, current_user)


@router.post("/service-requests/{id}/status", response_model=ServiceRequestOut)
def update_service_request_status(
    id: str,
    action: ServiceRequestStatusAction,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_support_or_admin),
):
    """Updates service request status with transition verification."""
    sr = db.query(ServiceRequest).filter(ServiceRequest.id == id).first()
    if not sr:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Service request not found.")

    try:
        validate_status_transition(sr.status, action.status, sr.approval_required, sr.approval_status)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))

    old_status = sr.status
    sr.status = action.status

    record_sr_timeline(
        db,
        request_id=sr.id,
        event_type="Status Changed",
        title=f"Status: {action.status}",
        actor_id=current_user.id,
        description=f"Status changed from {old_status} to {action.status}. {action.reason or ''}",
    )

    log_audit(
        db,
        action="SERVICE_REQUEST_STATUS_UPDATED",
        entity_type="ServiceRequest",
        entity_id=sr.id,
        user_id=current_user.id,
        details={"old_status": old_status, "new_status": action.status, "reason": action.reason},
    )

    db.commit()
    db.refresh(sr)
    return get_service_request_detail(id, db, current_user)


@router.post("/service-requests/{id}/fulfill", response_model=ServiceRequestOut)
def fulfill_request_endpoint(
    id: str,
    action: ServiceRequestFulfillAction,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_support_or_admin),
):
    """Completes request fulfillment with mandatory verified technical details."""
    sr = db.query(ServiceRequest).filter(ServiceRequest.id == id).first()
    if not sr:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Service request not found.")

    try:
        fulfill_service_request(
            db,
            sr=sr,
            engineer=current_user,
            fulfillment_details=action.fulfillment_details,
            resolution_notes=action.resolution_notes,
        )
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))

    return get_service_request_detail(id, db, current_user)


@router.post("/service-requests/{id}/confirm", response_model=ServiceRequestOut)
def confirm_request_endpoint(
    id: str,
    action: ServiceRequestConfirmAction,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Requester confirms fulfilled service, closing the request."""
    sr = db.query(ServiceRequest).filter(ServiceRequest.id == id).first()
    if not sr:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Service request not found.")

    # Only requester or admin can confirm
    if current_user.role == UserRole.EMPLOYEE and sr.requester_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only the requester can confirm service completion.",
        )

    try:
        confirm_service_request(db, sr=sr, user=current_user, feedback_notes=action.feedback_notes)
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))

    return get_service_request_detail(id, db, current_user)


@router.post("/service-requests/{id}/report-problem", response_model=ServiceRequestOut)
def report_problem_endpoint(
    id: str,
    action: ServiceRequestReportProblemAction,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Requester reports problem with fulfilled request, returning it to In Progress."""
    sr = db.query(ServiceRequest).filter(ServiceRequest.id == id).first()
    if not sr:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Service request not found.")

    if current_user.role == UserRole.EMPLOYEE and sr.requester_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="Only the requester can report problems on this request.",
        )

    try:
        report_service_request_problem(
            db, sr=sr, user=current_user, problem_description=action.problem_description
        )
    except ValueError as e:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=str(e))

    return get_service_request_detail(id, db, current_user)


@router.post("/service-requests/{id}/cancel", response_model=ServiceRequestOut)
def cancel_service_request(
    id: str,
    action: ServiceRequestCancelAction,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Cancels a service request prior to fulfillment."""
    sr = db.query(ServiceRequest).filter(ServiceRequest.id == id).first()
    if not sr:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Service request not found.")

    if current_user.role == UserRole.EMPLOYEE and sr.requester_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You are not authorized to cancel this service request.",
        )

    if sr.status in [ServiceRequestStatus.FULFILLED, ServiceRequestStatus.CLOSED]:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Cannot cancel an already fulfilled or closed request.",
        )

    sr.status = ServiceRequestStatus.CANCELLED

    record_sr_timeline(
        db,
        request_id=sr.id,
        event_type="Cancelled",
        title="Service Request Cancelled",
        actor_id=current_user.id,
        description=f"Cancelled by {current_user.name}. Reason: {action.reason or 'User requested cancellation.'}",
    )

    log_audit(
        db,
        action="SERVICE_REQUEST_CANCELLED",
        entity_type="ServiceRequest",
        entity_id=sr.id,
        user_id=current_user.id,
        details={"request_number": sr.request_number, "reason": action.reason},
    )

    db.commit()
    db.refresh(sr)
    return get_service_request_detail(id, db, current_user)


@router.post("/service-requests/{id}/comments", response_model=ServiceRequestCommentOut)
def add_service_request_comment(
    id: str,
    comment_data: ServiceRequestCommentCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Adds a customer-visible public comment or an internal support note."""
    sr = db.query(ServiceRequest).filter(ServiceRequest.id == id).first()
    if not sr:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Service request not found.")

    if current_user.role == UserRole.EMPLOYEE and sr.requester_id != current_user.id:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="You cannot comment on another employee's service request.",
        )

    # Employees can never post internal notes
    is_internal = comment_data.is_internal if current_user.role != UserRole.EMPLOYEE else False

    comment = ServiceRequestComment(
        request_id=sr.id,
        user_id=current_user.id,
        comment=comment_data.comment,
        is_internal=is_internal,
    )
    db.add(comment)

    event_title = "Internal Support Note Added" if is_internal else "Comment Added"
    record_sr_timeline(
        db,
        request_id=sr.id,
        event_type="Comment",
        title=event_title,
        actor_id=current_user.id,
        description=comment_data.comment[:100] + ("..." if len(comment_data.comment) > 100 else ""),
    )

    db.commit()
    db.refresh(comment)

    return ServiceRequestCommentOut(
        id=comment.id,
        request_id=comment.request_id,
        user_id=comment.user_id,
        user_name=current_user.name,
        user_role=current_user.role,
        comment=comment.comment,
        is_internal=comment.is_internal,
        created_at=comment.created_at,
    )


# ---------------------------------------------------------------------------
# DASHBOARD & OPERATIONAL ANALYTICS FOR SERVICE REQUESTS
# ---------------------------------------------------------------------------
@router.get("/service-requests/dashboard/stats", response_model=ServiceRequestDashboardOut)
def get_service_request_dashboard_stats(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Returns aggregated real-time operational metrics and Recharts chart datasets for Service Requests."""
    return get_sr_dashboard_metrics(db)


# ---------------------------------------------------------------------------
# UNIFIED SERVICE DESK VIEW (INCIDENTS + SERVICE REQUESTS)
# ---------------------------------------------------------------------------
@router.get("/service-desk/unified", response_model=List[UnifiedWorkItemOut])
def get_unified_service_desk_work(
    page: int = Query(1, ge=1),
    page_size: int = Query(25, ge=1, le=100),
    search: Optional[str] = None,
    item_type: Optional[str] = None,  # "Incident" | "Service Request"
    status_filter: Optional[str] = Query(None, alias="status"),
    priority: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Unified feed of Incidents and Service Requests with click-through navigation."""
    now = get_utc_now()
    unified_items: List[UnifiedWorkItemOut] = []

    # 1. Fetch Incidents (if item_type is None or Incident)
    if not item_type or item_type == "Incident":
        t_query = db.query(Ticket)
        if current_user.role == UserRole.EMPLOYEE:
            t_query = t_query.filter(Ticket.created_by == current_user.id)
        if status_filter:
            t_query = t_query.filter(Ticket.status == status_filter)
        if priority:
            t_query = t_query.filter(Ticket.priority == priority)

        tickets = t_query.order_by(desc(Ticket.created_at)).limit(100).all()
        for t in tickets:
            if search and (search.lower() not in t.ticket_number.lower() and search.lower() not in t.title.lower()):
                continue

            # SLA calculation
            now_u = ensure_utc(now)
            t_due_u = ensure_utc(t.due_at)
            t_res_u = ensure_utc(t.resolved_at)
            is_breached = (t_res_u > t_due_u) if t_res_u else (now_u > t_due_u if t.status not in [TicketStatus.RESOLVED, TicketStatus.CLOSED] else False)
            sla_status = "Breached" if is_breached else "Within SLA"

            unified_items.append(
                UnifiedWorkItemOut(
                    id=t.id,
                    item_type="Incident",
                    reference_number=t.ticket_number,
                    title=t.title,
                    category=t.category.name if t.category else "Incident",
                    requester_id=t.created_by,
                    requester_name=t.creator.name if t.creator else "User",
                    requester_department=t.creator.department if t.creator else "Staff",
                    priority=t.priority,
                    status=t.status,
                    assigned_to_id=t.assigned_to,
                    assigned_to_name=t.assignee.name if t.assignee else None,
                    assigned_team="Service Desk",
                    sla_status=sla_status,
                    is_breached=is_breached,
                    due_at=t.due_at,
                    created_at=t.created_at,
                    updated_at=t.updated_at,
                )
            )

    # 2. Fetch Service Requests (if item_type is None or Service Request)
    if not item_type or item_type == "Service Request":
        sr_query = db.query(ServiceRequest)
        if current_user.role == UserRole.EMPLOYEE:
            sr_query = sr_query.filter(ServiceRequest.requester_id == current_user.id)
        if status_filter:
            sr_query = sr_query.filter(ServiceRequest.status == status_filter)
        if priority:
            sr_query = sr_query.filter(ServiceRequest.priority == priority)

        srs = sr_query.order_by(desc(ServiceRequest.created_at)).limit(100).all()
        for sr in srs:
            if search and (search.lower() not in sr.request_number.lower() and search.lower() not in sr.title.lower()):
                continue

            status_name, is_breached = calculate_sr_sla_status(sr, now)
            unified_items.append(
                UnifiedWorkItemOut(
                    id=sr.id,
                    item_type="Service Request",
                    reference_number=sr.request_number,
                    title=sr.title,
                    category=sr.category,
                    requester_id=sr.requester_id,
                    requester_name=sr.requester.name if sr.requester else "User",
                    requester_department=sr.requester.department if sr.requester else "Staff",
                    priority=sr.priority,
                    status=sr.status,
                    assigned_to_id=sr.assigned_to,
                    assigned_to_name=sr.assignee.name if sr.assignee else None,
                    assigned_team=sr.assigned_team or "Service Desk",
                    sla_status=status_name,
                    is_breached=is_breached,
                    due_at=sr.due_at,
                    created_at=sr.created_at,
                    updated_at=sr.updated_at,
                )
            )

    # Sort unified items by created_at desc
    unified_items.sort(key=lambda x: ensure_utc(x.created_at), reverse=True)

    # Pagination slice
    start = (page - 1) * page_size
    return unified_items[start : start + page_size]
