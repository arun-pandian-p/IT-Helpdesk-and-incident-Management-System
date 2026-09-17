from datetime import datetime, timedelta, timezone
from typing import Any, Dict, List
from fastapi import APIRouter, Depends
from sqlalchemy import func
from sqlalchemy.orm import Session

from app.db.database import get_db
from app.db.models import (
    AccessRequest,
    AccessRequestStatus,
    Asset,
    OnboardingRequest,
    Ticket,
    TicketCategory,
    TicketFeedback,
    TicketPriority,
    TicketStatus,
    User,
)
from app.dependencies.auth import get_current_user
from app.schemas import DashboardSummaryOut
from app.services.ticket_service import calculate_sla_status

router = APIRouter(prefix="/dashboard", tags=["Operational Analytics"])


@router.get("/summary", response_model=DashboardSummaryOut)
def get_dashboard_summary(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Computes real-time operational IT service desk analytics:
    KPI cards, SLA breaches, CSAT satisfaction, hardware volume, and trend charts.
    """
    all_tickets = db.query(Ticket).all()
    total_tickets = len(all_tickets)

    open_tickets = sum(1 for t in all_tickets if t.status == TicketStatus.OPEN)
    in_progress = sum(1 for t in all_tickets if t.status == TicketStatus.IN_PROGRESS)
    waiting_tickets = sum(
        1 for t in all_tickets if t.status in [TicketStatus.WAITING_FOR_USER, TicketStatus.WAITING_FOR_VENDOR]
    )
    resolved_tickets = sum(1 for t in all_tickets if t.status == TicketStatus.RESOLVED)
    closed_tickets = sum(1 for t in all_tickets if t.status == TicketStatus.CLOSED)
    critical_tickets = sum(
        1 for t in all_tickets if t.priority == TicketPriority.CRITICAL and t.status not in [TicketStatus.RESOLVED, TicketStatus.CLOSED]
    )

    # SLA calculations
    sla_breached = sum(1 for t in all_tickets if calculate_sla_status(t) == "Breached")
    sla_at_risk = sum(1 for t in all_tickets if calculate_sla_status(t) == "At Risk")

    # Average resolution hours
    resolved_with_time = [
        (t.resolved_at - t.created_at).total_seconds() / 3600.0
        for t in all_tickets
        if t.resolved_at and t.created_at
    ]
    avg_resolution_hours = (
        round(sum(resolved_with_time) / len(resolved_with_time), 1) if resolved_with_time else 0.0
    )

    # Average first response hours
    first_response_with_time = [
        (t.first_response_at - t.created_at).total_seconds() / 3600.0
        for t in all_tickets
        if t.first_response_at and t.created_at
    ]
    avg_first_response_hours = (
        round(sum(first_response_with_time) / len(first_response_with_time), 1)
        if first_response_with_time
        else 0.0
    )

    # CSAT Average Rating
    feedback_ratings = [f.rating for f in db.query(TicketFeedback.rating).all()]
    avg_csat = round(sum(feedback_ratings) / len(feedback_ratings), 2) if feedback_ratings else 4.8

    # Asset counts
    total_assets = db.query(Asset).count()

    # Active onboardings
    active_onboardings = db.query(OnboardingRequest).filter(OnboardingRequest.status != "Completed").count()

    # Pending access requests
    pending_access = (
        db.query(AccessRequest)
        .filter(AccessRequest.status.in_([AccessRequestStatus.REQUESTED, AccessRequestStatus.PENDING_APPROVAL]))
        .count()
    )

    # Status Distribution
    status_dist = {
        "Open": open_tickets,
        "In Progress": in_progress,
        "Waiting": waiting_tickets,
        "Resolved": resolved_tickets,
        "Closed": closed_tickets,
    }

    # Priority Distribution
    priority_dist = {
        "Critical": sum(1 for t in all_tickets if t.priority == TicketPriority.CRITICAL),
        "High": sum(1 for t in all_tickets if t.priority == TicketPriority.HIGH),
        "Medium": sum(1 for t in all_tickets if t.priority == TicketPriority.MEDIUM),
        "Low": sum(1 for t in all_tickets if t.priority == TicketPriority.LOW),
    }

    # Category Distribution
    categories = db.query(TicketCategory).all()
    cat_map = {c.id: c.name for c in categories}
    cat_dist: Dict[str, int] = {}
    for t in all_tickets:
        cname = cat_map.get(t.category_id, "Other")
        cat_dist[cname] = cat_dist.get(cname, 0) + 1

    # OS Distribution
    os_dist: Dict[str, int] = {}
    for t in all_tickets:
        os_name = t.os_name or "Windows 11"
        os_dist[os_name] = os_dist.get(os_name, 0) + 1

    # Daily Trend (last 14 days)
    now = datetime.now(timezone.utc)
    daily_trend: List[Dict[str, Any]] = []
    for i in range(13, -1, -1):
        day_date = (now - timedelta(days=i)).date()
        day_str = day_date.strftime("%b %d")
        
        created_count = sum(
            1 for t in all_tickets if t.created_at and t.created_at.date() == day_date
        )
        resolved_count = sum(
            1 for t in all_tickets if t.resolved_at and t.resolved_at.date() == day_date
        )
        daily_trend.append(
            {
                "date": day_str,
                "created": created_count,
                "resolved": resolved_count,
            }
        )

    return DashboardSummaryOut(
        total_tickets=total_tickets,
        open_tickets=open_tickets,
        in_progress_tickets=in_progress,
        waiting_tickets=waiting_tickets,
        resolved_tickets=resolved_tickets,
        closed_tickets=closed_tickets,
        critical_tickets=critical_tickets,
        sla_breached=sla_breached,
        sla_at_risk=sla_at_risk,
        average_resolution_hours=avg_resolution_hours,
        average_first_response_hours=avg_first_response_hours,
        csat_average_rating=avg_csat,
        total_assets=total_assets,
        active_onboardings=active_onboardings,
        pending_access_requests=pending_access,
        status_distribution=status_dist,
        priority_distribution=priority_dist,
        category_distribution=cat_dist,
        os_distribution=os_dist,
        daily_trend=daily_trend,
    )
