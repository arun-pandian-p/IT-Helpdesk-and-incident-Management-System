from datetime import datetime, timedelta, timezone
from typing import Optional, Tuple
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.db.models import (
    Ticket,
    TicketPriority,
    TicketStatus,
    ImpactLevel,
    UrgencyLevel,
    TicketStatusHistory,
)
from app.services.audit_service import log_audit


# ---------------------------------------------------------------------------
# PRIORITY MATRIX (ITIL-INSPIRED IMPACT x URGENCY)
# ---------------------------------------------------------------------------
PRIORITY_MATRIX = {
    (ImpactLevel.COMPANY_WIDE, UrgencyLevel.CRITICAL): TicketPriority.CRITICAL,
    (ImpactLevel.COMPANY_WIDE, UrgencyLevel.HIGH): TicketPriority.CRITICAL,
    (ImpactLevel.COMPANY_WIDE, UrgencyLevel.MEDIUM): TicketPriority.HIGH,
    (ImpactLevel.COMPANY_WIDE, UrgencyLevel.LOW): TicketPriority.MEDIUM,

    (ImpactLevel.MULTIPLE_DEPARTMENTS, UrgencyLevel.CRITICAL): TicketPriority.CRITICAL,
    (ImpactLevel.MULTIPLE_DEPARTMENTS, UrgencyLevel.HIGH): TicketPriority.HIGH,
    (ImpactLevel.MULTIPLE_DEPARTMENTS, UrgencyLevel.MEDIUM): TicketPriority.MEDIUM,
    (ImpactLevel.MULTIPLE_DEPARTMENTS, UrgencyLevel.LOW): TicketPriority.LOW,

    (ImpactLevel.DEPARTMENT, UrgencyLevel.CRITICAL): TicketPriority.HIGH,
    (ImpactLevel.DEPARTMENT, UrgencyLevel.HIGH): TicketPriority.HIGH,
    (ImpactLevel.DEPARTMENT, UrgencyLevel.MEDIUM): TicketPriority.MEDIUM,
    (ImpactLevel.DEPARTMENT, UrgencyLevel.LOW): TicketPriority.LOW,

    (ImpactLevel.TEAM, UrgencyLevel.CRITICAL): TicketPriority.HIGH,
    (ImpactLevel.TEAM, UrgencyLevel.HIGH): TicketPriority.MEDIUM,
    (ImpactLevel.TEAM, UrgencyLevel.MEDIUM): TicketPriority.MEDIUM,
    (ImpactLevel.TEAM, UrgencyLevel.LOW): TicketPriority.LOW,

    (ImpactLevel.INDIVIDUAL, UrgencyLevel.CRITICAL): TicketPriority.HIGH,
    (ImpactLevel.INDIVIDUAL, UrgencyLevel.HIGH): TicketPriority.MEDIUM,
    (ImpactLevel.INDIVIDUAL, UrgencyLevel.MEDIUM): TicketPriority.MEDIUM,
    (ImpactLevel.INDIVIDUAL, UrgencyLevel.LOW): TicketPriority.LOW,
}


def calculate_suggested_priority(impact: str, urgency: str) -> str:
    """Derives ITIL-inspired suggested priority from Impact and Urgency."""
    return PRIORITY_MATRIX.get((impact, urgency), TicketPriority.MEDIUM)


def get_sla_target_hours(priority: str) -> int:
    """Returns SLA resolution window in hours for given priority."""
    if priority == TicketPriority.CRITICAL:
        return settings.SLA_CRITICAL_HOURS
    elif priority == TicketPriority.HIGH:
        return settings.SLA_HIGH_HOURS
    elif priority == TicketPriority.MEDIUM:
        return settings.SLA_MEDIUM_HOURS
    else:
        return settings.SLA_LOW_HOURS


def calculate_sla_status(ticket: Ticket) -> str:
    """
    Evaluates real-time SLA status:
    - Within SLA
    - At Risk
    - Breached
    """
    now = datetime.now(timezone.utc)
    due_at = ticket.due_at
    if due_at.tzinfo is None:
        due_at = due_at.replace(tzinfo=timezone.utc)

    # If ticket was resolved
    if ticket.resolved_at:
        resolved_at = ticket.resolved_at
        if resolved_at.tzinfo is None:
            resolved_at = resolved_at.replace(tzinfo=timezone.utc)
        if resolved_at <= due_at:
            return "Within SLA"
        else:
            return "Breached"

    # For open/unresolved tickets
    if now > due_at:
        return "Breached"
    
    target_hours = get_sla_target_hours(ticket.priority)
    time_remaining_seconds = (due_at - now).total_seconds()
    at_risk_threshold_seconds = min(4 * 3600, target_hours * 3600 * 0.25)
    
    if time_remaining_seconds <= at_risk_threshold_seconds:
        return "At Risk"

    return "Within SLA"


def generate_ticket_number(db: Session) -> str:
    """
    Safely generates unique human-readable ticket number: INC-YYYY-000001
    """
    current_year = datetime.now(timezone.utc).year
    prefix = f"INC-{current_year}-"

    # Query the highest ticket number for the current year
    stmt = (
        select(Ticket.ticket_number)
        .where(Ticket.ticket_number.like(f"{prefix}%"))
        .order_by(Ticket.ticket_number.desc())
        .limit(1)
    )
    last_ticket = db.execute(stmt).scalar_one_or_none()

    if last_ticket:
        try:
            last_seq = int(last_ticket.replace(prefix, ""))
            next_seq = last_seq + 1
        except ValueError:
            next_seq = 1
    else:
        next_seq = 1

    return f"{prefix}{next_seq:06d}"


# ---------------------------------------------------------------------------
# STATUS TRANSITION RULES
# ---------------------------------------------------------------------------
ALLOWED_TRANSITIONS = {
    TicketStatus.OPEN: [
        TicketStatus.IN_PROGRESS,
        TicketStatus.WAITING_FOR_USER,
        TicketStatus.WAITING_FOR_VENDOR,
        TicketStatus.ESCALATED,
        TicketStatus.CLOSED,
    ],
    TicketStatus.IN_PROGRESS: [
        TicketStatus.WAITING_FOR_USER,
        TicketStatus.WAITING_FOR_VENDOR,
        TicketStatus.ESCALATED,
        TicketStatus.RESOLVED,
        TicketStatus.CLOSED,
    ],
    TicketStatus.WAITING_FOR_USER: [
        TicketStatus.IN_PROGRESS,
        TicketStatus.RESOLVED,
        TicketStatus.CLOSED,
    ],
    TicketStatus.WAITING_FOR_VENDOR: [
        TicketStatus.IN_PROGRESS,
        TicketStatus.RESOLVED,
        TicketStatus.CLOSED,
    ],
    TicketStatus.ESCALATED: [
        TicketStatus.IN_PROGRESS,
        TicketStatus.RESOLVED,
        TicketStatus.CLOSED,
    ],
    TicketStatus.RESOLVED: [
        TicketStatus.IN_PROGRESS,  # Reopen
        TicketStatus.CLOSED,
    ],
    TicketStatus.CLOSED: [
        TicketStatus.IN_PROGRESS,  # Admin override reopening only
    ],
}


def update_ticket_status(
    db: Session,
    ticket: Ticket,
    new_status: str,
    user_id: str,
    reason: Optional[str] = None,
    is_admin: bool = False,
) -> Tuple[bool, Optional[str]]:
    """
    Validates and executes a status change, recording status history and audit trail.
    """
    old_status = ticket.status
    if old_status == new_status:
        return True, None

    allowed = ALLOWED_TRANSITIONS.get(old_status, [])
    if new_status not in allowed and not is_admin:
        return False, f"Invalid transition from '{old_status}' to '{new_status}'."

    now = datetime.now(timezone.utc)
    ticket.status = new_status
    ticket.updated_at = now

    # First response tracking
    if not ticket.first_response_at and new_status in [TicketStatus.IN_PROGRESS, TicketStatus.WAITING_FOR_USER]:
        ticket.first_response_at = now

    # Reopen logic
    if old_status == TicketStatus.RESOLVED and new_status == TicketStatus.IN_PROGRESS:
        ticket.resolved_at = None
        ticket.resolution_notes = None

    # Closure
    if new_status == TicketStatus.CLOSED:
        ticket.closed_at = now

    # Add status history
    history = TicketStatusHistory(
        ticket_id=ticket.id,
        old_status=old_status,
        new_status=new_status,
        changed_by=user_id,
        changed_at=now,
        reason=reason,
    )
    db.add(history)

    # Log audit
    log_audit(
        db,
        action="TICKET_STATUS_CHANGED",
        entity_type="Ticket",
        entity_id=ticket.id,
        user_id=user_id,
        details={"old_status": old_status, "new_status": new_status, "reason": reason},
    )

    db.flush()
    return True, None
