import json
from datetime import datetime, timedelta, timezone
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import or_, select
from sqlalchemy.orm import Session, joinedload

from app.db.database import get_db
from app.db.models import (
    Asset,
    Ticket,
    TicketAssignment,
    TicketCategory,
    TicketComment,
    TicketEscalation,
    TicketFeedback,
    TicketPriority,
    TicketStatus,
    TicketStatusHistory,
    User,
    UserRole,
)
from app.dependencies.auth import get_current_user, require_support
from app.schemas import (
    TicketAssign,
    TicketChecklistUpdate,
    TicketCommentCreate,
    TicketCommentOut,
    TicketCreate,
    TicketEscalate,
    TicketEscalationOut,
    TicketFeedbackCreate,
    TicketFeedbackOut,
    TicketListResponse,
    TicketOut,
    TicketPriorityOverride,
    TicketResolve,
    TicketStatusHistoryOut,
    TicketStatusUpdate,
    AssetOut,
)
from app.services.audit_service import log_audit
from app.services.ticket_service import (
    calculate_sla_status,
    calculate_suggested_priority,
    generate_ticket_number,
    get_sla_target_hours,
    update_ticket_status,
)

router = APIRouter(prefix="/tickets", tags=["Tickets"])


def enrich_ticket_out(ticket: Ticket, current_user_role: str) -> TicketOut:
    """Helper to convert Ticket ORM model to TicketOut schema with SLA and filtered comments."""
    sla_status = calculate_sla_status(ticket)

    # Filter internal comments if viewer is standard employee
    visible_comments = []
    if ticket.comments:
        for c in ticket.comments:
            if not c.is_internal or current_user_role in [UserRole.SUPPORT, UserRole.ADMIN]:
                visible_comments.append(
                    TicketCommentOut(
                        id=c.id,
                        ticket_id=c.ticket_id,
                        user_id=c.user_id,
                        user_name=c.user.name if c.user else "Unknown",
                        user_role=c.user.role if c.user else "EMPLOYEE",
                        comment=c.comment,
                        is_internal=c.is_internal,
                        created_at=c.created_at,
                    )
                )

    status_hist = []
    if ticket.status_history:
        for h in ticket.status_history:
            status_hist.append(
                TicketStatusHistoryOut(
                    id=h.id,
                    old_status=h.old_status,
                    new_status=h.new_status,
                    changed_by_name=h.ticket.creator.name if h.ticket and h.ticket.creator else "System",
                    changed_at=h.changed_at,
                    reason=h.reason,
                )
            )

    escalations_out = []
    if ticket.escalations:
        for e in ticket.escalations:
            escalations_out.append(
                TicketEscalationOut(
                    id=e.id,
                    from_engineer_name="Support Engineer",
                    to_engineer_name="Senior Support",
                    reason=e.reason,
                    escalation_level=e.escalation_level,
                    created_at=e.created_at,
                    resolved_at=e.resolved_at,
                )
            )

    asset_out = None
    if ticket.asset:
        asset_out = AssetOut(
            id=ticket.asset.id,
            asset_tag=ticket.asset.asset_tag,
            device_type=ticket.asset.device_type,
            manufacturer=ticket.asset.manufacturer,
            model=ticket.asset.model,
            serial_number=ticket.asset.serial_number,
            status=ticket.asset.status,
            assigned_user_id=ticket.asset.assigned_user_id,
            assigned_user_name=ticket.asset.assigned_user.name if ticket.asset.assigned_user else None,
            purchase_date=ticket.asset.purchase_date,
            warranty_expiry=ticket.asset.warranty_expiry,
            notes=ticket.asset.notes,
            created_at=ticket.asset.created_at,
        )

    feedback_out = None
    if ticket.feedback:
        feedback_out = TicketFeedbackOut(
            id=ticket.feedback.id,
            rating=ticket.feedback.rating,
            comments=ticket.feedback.comments,
            created_at=ticket.feedback.created_at,
        )

    return TicketOut(
        id=ticket.id,
        ticket_number=ticket.ticket_number,
        title=ticket.title,
        description=ticket.description,
        category_id=ticket.category_id,
        category_name=ticket.category.name if ticket.category else None,
        priority=ticket.priority,
        status=ticket.status,
        impact=ticket.impact,
        urgency=ticket.urgency,
        priority_override_reason=ticket.priority_override_reason,
        asset_id=ticket.asset_id,
        asset=asset_out,
        device_type=ticket.device_type,
        os_name=ticket.os_name,
        application_name=ticket.application_name,
        location=ticket.location,
        created_by=ticket.created_by,
        creator_name=ticket.creator.name if ticket.creator else None,
        creator_email=ticket.creator.email if ticket.creator else None,
        creator_department=ticket.creator.department if ticket.creator else None,
        assigned_to=ticket.assigned_to,
        assignee_name=ticket.assignee.name if ticket.assignee else None,
        created_at=ticket.created_at,
        updated_at=ticket.updated_at,
        due_at=ticket.due_at,
        first_response_at=ticket.first_response_at,
        resolved_at=ticket.resolved_at,
        closed_at=ticket.closed_at,
        resolution_notes=ticket.resolution_notes,
        is_escalated=ticket.is_escalated,
        escalation_level=ticket.escalation_level,
        checklist_state=ticket.checklist_state,
        sla_status=sla_status,
        feedback=feedback_out,
        comments=visible_comments,
        status_history=status_hist,
        escalations=escalations_out,
    )


# ---------------------------------------------------------------------------
# CREATE TICKET
# ---------------------------------------------------------------------------
@router.post("", response_model=TicketOut, status_code=status.HTTP_201_CREATED)
def create_ticket(
    request: TicketCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Creates a new incident with Impact x Urgency derived priority,
    calculates SLA due_at, sets status = Open, and generates INC-YYYY-XXXXXX.
    """
    category = db.query(TicketCategory).filter(TicketCategory.id == request.category_id).first()
    if not category:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid category ID.")

    # Auto-calculate priority from matrix if not provided
    priority = request.priority or calculate_suggested_priority(request.impact, request.urgency)

    now = datetime.now(timezone.utc)
    target_hours = get_sla_target_hours(priority)
    due_at = now + timedelta(hours=target_hours)

    ticket_number = generate_ticket_number(db)

    ticket = Ticket(
        ticket_number=ticket_number,
        title=request.title,
        description=request.description,
        category_id=request.category_id,
        priority=priority,
        status=TicketStatus.OPEN,
        impact=request.impact,
        urgency=request.urgency,
        asset_id=request.asset_id,
        device_type=request.device_type,
        os_name=request.os_name,
        application_name=request.application_name,
        location=request.location or current_user.location or "Headquarters",
        created_by=current_user.id,
        due_at=due_at,
        created_at=now,
        updated_at=now,
    )
    db.add(ticket)
    db.flush()

    # Initial status history
    initial_history = TicketStatusHistory(
        ticket_id=ticket.id,
        old_status="None",
        new_status=TicketStatus.OPEN,
        changed_by=current_user.id,
        changed_at=now,
        reason="Incident submitted by user.",
    )
    db.add(initial_history)

    # Audit log
    log_audit(
        db,
        action="TICKET_CREATED",
        entity_type="Ticket",
        entity_id=ticket.id,
        user_id=current_user.id,
        details={
            "ticket_number": ticket_number,
            "title": ticket.title,
            "priority": priority,
            "due_at": due_at.isoformat(),
        },
    )

    db.commit()
    db.refresh(ticket)
    return enrich_ticket_out(ticket, current_user.role)


# ---------------------------------------------------------------------------
# LIST TICKETS
# ---------------------------------------------------------------------------
@router.get("", response_model=TicketListResponse)
def list_tickets(
    status: Optional[str] = None,
    priority: Optional[str] = None,
    category_id: Optional[str] = None,
    assigned_to: Optional[str] = None,
    created_by: Optional[str] = None,
    sla_status: Optional[str] = None,
    search: Optional[str] = None,
    only_mine: bool = False,
    page: int = Query(1, ge=1),
    page_size: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Search and filter incidents with pagination.
    Employees see only their own tickets unless authorized.
    """
    query = (
        db.query(Ticket)
        .options(
            joinedload(Ticket.category),
            joinedload(Ticket.creator),
            joinedload(Ticket.assignee),
            joinedload(Ticket.asset),
        )
    )

    # Restrict employees to only their tickets
    if current_user.role == UserRole.EMPLOYEE or only_mine:
        query = query.filter(Ticket.created_by == current_user.id)
    elif created_by:
        query = query.filter(Ticket.created_by == created_by)

    if status:
        query = query.filter(Ticket.status == status)
    if priority:
        query = query.filter(Ticket.priority == priority)
    if category_id:
        query = query.filter(Ticket.category_id == category_id)
    if assigned_to:
        query = query.filter(Ticket.assigned_to == assigned_to)

    if search:
        pattern = f"%{search.strip()}%"
        query = query.join(Ticket.creator).filter(
            or_(
                Ticket.ticket_number.ilike(pattern),
                Ticket.title.ilike(pattern),
                Ticket.description.ilike(pattern),
                User.name.ilike(pattern),
            )
        )

    all_matching = query.order_by(Ticket.created_at.desc()).all()

    # Filter in-memory for derived SLA status if requested
    if sla_status:
        filtered = [t for t in all_matching if calculate_sla_status(t) == sla_status]
    else:
        filtered = all_matching

    total = len(filtered)
    total_pages = max(1, (total + page_size - 1) // page_size)
    offset = (page - 1) * page_size
    paged_items = filtered[offset : offset + page_size]

    return TicketListResponse(
        items=[enrich_ticket_out(t, current_user.role) for t in paged_items],
        total=total,
        page=page,
        page_size=page_size,
        total_pages=total_pages,
    )


# ---------------------------------------------------------------------------
# GET TICKET DETAIL
# ---------------------------------------------------------------------------
@router.get("/{ticket_id}", response_model=TicketOut)
def get_ticket(
    ticket_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Retrieves full incident details with timeline and comments."""
    ticket = (
        db.query(Ticket)
        .options(
            joinedload(Ticket.category),
            joinedload(Ticket.creator),
            joinedload(Ticket.assignee),
            joinedload(Ticket.asset),
            joinedload(Ticket.comments).joinedload(TicketComment.user),
            joinedload(Ticket.status_history),
            joinedload(Ticket.escalations),
            joinedload(Ticket.feedback),
        )
        .filter(Ticket.id == ticket_id)
        .first()
    )
    if not ticket:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Ticket not found.")

    # Permissions: Employee can only see own tickets
    if current_user.role == UserRole.EMPLOYEE and ticket.created_by != current_user.id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied.")

    return enrich_ticket_out(ticket, current_user.role)


# ---------------------------------------------------------------------------
# ASSIGN TICKET
# ---------------------------------------------------------------------------
@router.post("/{ticket_id}/assign", response_model=TicketOut)
def assign_ticket(
    ticket_id: str,
    payload: TicketAssign,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_support),
):
    """Assigns ticket to a support engineer. If Open, transitions to In Progress."""
    ticket = db.query(Ticket).filter(Ticket.id == ticket_id).first()
    if not ticket:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Ticket not found.")

    target_engineer = db.query(User).filter(User.id == payload.assigned_to).first()
    if not target_engineer or target_engineer.role not in [UserRole.SUPPORT, UserRole.ADMIN]:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Assigned user must be a Support Engineer or Admin.",
        )

    now = datetime.now(timezone.utc)
    ticket.assigned_to = target_engineer.id
    if not ticket.first_response_at:
        ticket.first_response_at = now

    # Record assignment event
    assignment_entry = TicketAssignment(
        ticket_id=ticket.id,
        assigned_to=target_engineer.id,
        assigned_by=current_user.id,
        assigned_at=now,
    )
    db.add(assignment_entry)

    # Automatically transition Open -> In Progress
    if ticket.status == TicketStatus.OPEN:
        update_ticket_status(
            db,
            ticket=ticket,
            new_status=TicketStatus.IN_PROGRESS,
            user_id=current_user.id,
            reason=f"Assigned to {target_engineer.name}",
        )

    log_audit(
        db,
        action="TICKET_ASSIGNED",
        entity_type="Ticket",
        entity_id=ticket.id,
        user_id=current_user.id,
        details={"assigned_to": target_engineer.name, "assigned_to_id": target_engineer.id},
    )

    db.commit()
    db.refresh(ticket)
    return enrich_ticket_out(ticket, current_user.role)


# ---------------------------------------------------------------------------
# UPDATE STATUS
# ---------------------------------------------------------------------------
@router.post("/{ticket_id}/status", response_model=TicketOut)
def change_status(
    ticket_id: str,
    payload: TicketStatusUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_support),
):
    """Transitions ticket status with validation and history logging."""
    ticket = db.query(Ticket).filter(Ticket.id == ticket_id).first()
    if not ticket:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Ticket not found.")

    is_admin = current_user.role == UserRole.ADMIN
    success, error = update_ticket_status(
        db,
        ticket=ticket,
        new_status=payload.status,
        user_id=current_user.id,
        reason=payload.reason,
        is_admin=is_admin,
    )
    if not success:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail=error)

    db.commit()
    db.refresh(ticket)
    return enrich_ticket_out(ticket, current_user.role)


# ---------------------------------------------------------------------------
# OVERRIDE PRIORITY
# ---------------------------------------------------------------------------
@router.post("/{ticket_id}/override-priority", response_model=TicketOut)
def override_priority(
    ticket_id: str,
    payload: TicketPriorityOverride,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_support),
):
    """Allows support engineer to override priority with a recorded rationale."""
    ticket = db.query(Ticket).filter(Ticket.id == ticket_id).first()
    if not ticket:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Ticket not found.")

    valid_priorities = [TicketPriority.LOW, TicketPriority.MEDIUM, TicketPriority.HIGH, TicketPriority.CRITICAL]
    if payload.priority not in valid_priorities:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Invalid priority level.")

    old_priority = ticket.priority
    ticket.priority = payload.priority
    ticket.priority_override_reason = payload.reason
    
    # Recalculate due date from new priority SLA
    target_hours = get_sla_target_hours(payload.priority)
    ticket.due_at = ticket.created_at + timedelta(hours=target_hours)

    log_audit(
        db,
        action="PRIORITY_OVERRIDDEN",
        entity_type="Ticket",
        entity_id=ticket.id,
        user_id=current_user.id,
        details={
            "old_priority": old_priority,
            "new_priority": payload.priority,
            "reason": payload.reason,
            "new_due_at": ticket.due_at.isoformat(),
        },
    )

    db.commit()
    db.refresh(ticket)
    return enrich_ticket_out(ticket, current_user.role)


# ---------------------------------------------------------------------------
# ESCALATE TICKET
# ---------------------------------------------------------------------------
@router.post("/{ticket_id}/escalate", response_model=TicketOut)
def escalate_ticket(
    ticket_id: str,
    payload: TicketEscalate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_support),
):
    """Escalates ticket to Tier 2 / Tier 3 / Admin."""
    ticket = db.query(Ticket).filter(Ticket.id == ticket_id).first()
    if not ticket:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Ticket not found.")

    now = datetime.now(timezone.utc)
    ticket.is_escalated = True
    ticket.escalation_level = payload.escalation_level
    ticket.status = TicketStatus.ESCALATED

    if payload.to_engineer:
        ticket.assigned_to = payload.to_engineer

    escalation_entry = TicketEscalation(
        ticket_id=ticket.id,
        from_engineer=current_user.id,
        to_engineer=payload.to_engineer,
        reason=payload.reason,
        escalation_level=payload.escalation_level,
        created_at=now,
    )
    db.add(escalation_entry)

    # History
    history = TicketStatusHistory(
        ticket_id=ticket.id,
        old_status=ticket.status,
        new_status=TicketStatus.ESCALATED,
        changed_by=current_user.id,
        changed_at=now,
        reason=f"Escalated to Tier {payload.escalation_level}: {payload.reason}",
    )
    db.add(history)

    log_audit(
        db,
        action="TICKET_ESCALATED",
        entity_type="Ticket",
        entity_id=ticket.id,
        user_id=current_user.id,
        details={"level": payload.escalation_level, "reason": payload.reason},
    )

    db.commit()
    db.refresh(ticket)
    return enrich_ticket_out(ticket, current_user.role)


# ---------------------------------------------------------------------------
# RESOLVE TICKET
# ---------------------------------------------------------------------------
@router.post("/{ticket_id}/resolve", response_model=TicketOut)
def resolve_ticket(
    ticket_id: str,
    payload: TicketResolve,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_support),
):
    """Resolves incident. Requires documented resolution notes."""
    ticket = db.query(Ticket).filter(Ticket.id == ticket_id).first()
    if not ticket:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Ticket not found.")

    if not payload.resolution_notes or len(payload.resolution_notes.strip()) < 5:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="Resolution notes are mandatory to resolve an incident.",
        )

    now = datetime.now(timezone.utc)
    old_status = ticket.status
    ticket.status = TicketStatus.RESOLVED
    ticket.resolved_at = now
    ticket.resolution_notes = payload.resolution_notes.strip()

    history = TicketStatusHistory(
        ticket_id=ticket.id,
        old_status=old_status,
        new_status=TicketStatus.RESOLVED,
        changed_by=current_user.id,
        changed_at=now,
        reason=f"Resolved: {ticket.resolution_notes[:100]}",
    )
    db.add(history)

    log_audit(
        db,
        action="TICKET_RESOLVED",
        entity_type="Ticket",
        entity_id=ticket.id,
        user_id=current_user.id,
        details={"resolution_notes": ticket.resolution_notes},
    )

    db.commit()
    db.refresh(ticket)
    return enrich_ticket_out(ticket, current_user.role)


# ---------------------------------------------------------------------------
# CLOSE TICKET
# ---------------------------------------------------------------------------
@router.post("/{ticket_id}/close", response_model=TicketOut)
def close_ticket(
    ticket_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Closes resolved ticket permanently."""
    ticket = db.query(Ticket).filter(Ticket.id == ticket_id).first()
    if not ticket:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Ticket not found.")

    if current_user.role == UserRole.EMPLOYEE and ticket.created_by != current_user.id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied.")

    now = datetime.now(timezone.utc)
    old_status = ticket.status
    ticket.status = TicketStatus.CLOSED
    ticket.closed_at = now

    history = TicketStatusHistory(
        ticket_id=ticket.id,
        old_status=old_status,
        new_status=TicketStatus.CLOSED,
        changed_by=current_user.id,
        changed_at=now,
        reason="Incident closed.",
    )
    db.add(history)

    log_audit(
        db,
        action="TICKET_CLOSED",
        entity_type="Ticket",
        entity_id=ticket.id,
        user_id=current_user.id,
    )

    db.commit()
    db.refresh(ticket)
    return enrich_ticket_out(ticket, current_user.role)


# ---------------------------------------------------------------------------
# REOPEN TICKET
# ---------------------------------------------------------------------------
@router.post("/{ticket_id}/reopen", response_model=TicketOut)
def reopen_ticket(
    ticket_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Reopens a resolved ticket back to In Progress if the issue persists."""
    ticket = db.query(Ticket).filter(Ticket.id == ticket_id).first()
    if not ticket:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Ticket not found.")

    if ticket.status != TicketStatus.RESOLVED and current_user.role != UserRole.ADMIN:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Only resolved tickets can be reopened by users.",
        )

    now = datetime.now(timezone.utc)
    old_status = ticket.status
    ticket.status = TicketStatus.IN_PROGRESS
    ticket.resolved_at = None

    history = TicketStatusHistory(
        ticket_id=ticket.id,
        old_status=old_status,
        new_status=TicketStatus.IN_PROGRESS,
        changed_by=current_user.id,
        changed_at=now,
        reason="Ticket reopened by user: issue persists.",
    )
    db.add(history)

    log_audit(
        db,
        action="TICKET_REOPENED",
        entity_type="Ticket",
        entity_id=ticket.id,
        user_id=current_user.id,
    )

    db.commit()
    db.refresh(ticket)
    return enrich_ticket_out(ticket, current_user.role)


# ---------------------------------------------------------------------------
# COMMENTS
# ---------------------------------------------------------------------------
@router.post("/{ticket_id}/comments", response_model=TicketCommentOut)
def add_comment(
    ticket_id: str,
    payload: TicketCommentCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Adds a customer-facing or internal troubleshooting note."""
    ticket = db.query(Ticket).filter(Ticket.id == ticket_id).first()
    if not ticket:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Ticket not found.")

    # Employees can only post customer-facing comments to own tickets
    if current_user.role == UserRole.EMPLOYEE:
        if ticket.created_by != current_user.id:
            raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Access denied.")
        payload.is_internal = False

    now = datetime.now(timezone.utc)
    comment = TicketComment(
        ticket_id=ticket.id,
        user_id=current_user.id,
        comment=payload.comment,
        is_internal=payload.is_internal,
        created_at=now,
    )
    db.add(comment)

    # First response tracking if support engineer replies
    if current_user.role in [UserRole.SUPPORT, UserRole.ADMIN] and not ticket.first_response_at:
        ticket.first_response_at = now

    log_audit(
        db,
        action="COMMENT_ADDED",
        entity_type="Ticket",
        entity_id=ticket.id,
        user_id=current_user.id,
        details={"is_internal": payload.is_internal},
    )

    db.commit()
    db.refresh(comment)

    return TicketCommentOut(
        id=comment.id,
        ticket_id=comment.ticket_id,
        user_id=comment.user_id,
        user_name=current_user.name,
        user_role=current_user.role,
        comment=comment.comment,
        is_internal=comment.is_internal,
        created_at=comment.created_at,
    )


# ---------------------------------------------------------------------------
# TROUBLESHOOTING CHECKLIST STATE
# ---------------------------------------------------------------------------
@router.patch("/{ticket_id}/checklist", response_model=TicketOut)
def update_checklist(
    ticket_id: str,
    payload: TicketChecklistUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_support),
):
    """Saves completed diagnostic steps on the hardware/OS/M365 troubleshooting checklist."""
    ticket = db.query(Ticket).filter(Ticket.id == ticket_id).first()
    if not ticket:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Ticket not found.")

    ticket.checklist_state = payload.checklist_state
    db.commit()
    db.refresh(ticket)
    return enrich_ticket_out(ticket, current_user.role)


# ---------------------------------------------------------------------------
# CSAT FEEDBACK
# ---------------------------------------------------------------------------
@router.post("/{ticket_id}/feedback", response_model=TicketFeedbackOut)
def submit_feedback(
    ticket_id: str,
    payload: TicketFeedbackCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Allows user to submit customer satisfaction (CSAT) rating on resolved/closed ticket."""
    ticket = db.query(Ticket).filter(Ticket.id == ticket_id).first()
    if not ticket:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Ticket not found.")

    if ticket.created_by != current_user.id:
        raise HTTPException(status_code=status.HTTP_403_FORBIDDEN, detail="Can only review your own ticket.")

    existing = db.query(TicketFeedback).filter(TicketFeedback.ticket_id == ticket_id).first()
    if existing:
        existing.rating = payload.rating
        existing.comments = payload.comments
        feedback_obj = existing
    else:
        feedback_obj = TicketFeedback(
            ticket_id=ticket.id,
            user_id=current_user.id,
            rating=payload.rating,
            comments=payload.comments,
        )
        db.add(feedback_obj)

    log_audit(
        db,
        action="CSAT_FEEDBACK_SUBMITTED",
        entity_type="Ticket",
        entity_id=ticket.id,
        user_id=current_user.id,
        details={"rating": payload.rating},
    )

    db.commit()
    db.refresh(feedback_obj)
    return TicketFeedbackOut.model_validate(feedback_obj)
