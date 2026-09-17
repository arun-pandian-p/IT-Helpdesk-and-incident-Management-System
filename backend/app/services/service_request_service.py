import json
from datetime import datetime, date, timedelta, timezone
from typing import Any, Dict, List, Optional, Tuple
from sqlalchemy import func, or_
from sqlalchemy.orm import Session

from app.db.base import get_utc_now, ensure_utc
from app.db.models import (
    Asset,
    AssetAssignment,
    AssetStatus,
    ServiceRequest,
    ServiceRequestApprovalStatus,
    ServiceRequestCategory,
    ServiceRequestComment,
    ServiceRequestStatus,
    ServiceRequestTimeline,
    ServiceRequestType,
    SupportTeam,
    TicketPriority,
    User,
    UserRole,
)
from app.services.audit_service import log_audit


def generate_service_request_number(db: Session) -> str:
    """Generates sequential request identifier: SR-YYYY-XXXXXX."""
    current_year = datetime.now(timezone.utc).year
    prefix = f"SR-{current_year}-"

    latest_sr = (
        db.query(ServiceRequest)
        .filter(ServiceRequest.request_number.like(f"{prefix}%"))
        .order_by(ServiceRequest.request_number.desc())
        .first()
    )

    if not latest_sr:
        return f"{prefix}000001"

    try:
        last_seq = int(latest_sr.request_number.split("-")[-1])
        next_seq = last_seq + 1
        return f"{prefix}{next_seq:06d}"
    except (ValueError, IndexError):
        count = db.query(ServiceRequest).count() + 1
        return f"{prefix}{count:06d}"


def calculate_sr_sla_status(sr: ServiceRequest, now: Optional[datetime] = None) -> Tuple[str, bool]:
    """
    Computes real-time SLA status and breach flag:
    - If fulfilled/closed, evaluates fulfilled_at vs due_at.
    - If active, evaluates remaining time vs due_at.
    """
    if now is None:
        now = get_utc_now()

    now_u = ensure_utc(now)
    due_u = ensure_utc(sr.due_at)
    created_u = ensure_utc(sr.created_at)
    fulfilled_u = ensure_utc(sr.fulfilled_at)

    # If completed, check against fulfilled_at
    if fulfilled_u is not None:
        is_breached = fulfilled_u > due_u
        status = "Breached" if is_breached else "Within SLA"
        return status, is_breached

    if sr.status in [ServiceRequestStatus.CLOSED, ServiceRequestStatus.CANCELLED, ServiceRequestStatus.REJECTED]:
        return "Within SLA", False

    # Active request
    if now_u > due_u:
        return "Breached", True

    total_window = (due_u - created_u).total_seconds()
    remaining = (due_u - now_u).total_seconds()

    if total_window > 0 and (remaining / total_window) < 0.25:
        return "At Risk", False

    return "Within SLA", False


def record_sr_timeline(
    db: Session,
    request_id: str,
    event_type: str,
    title: str,
    actor_id: str,
    description: Optional[str] = None,
) -> ServiceRequestTimeline:
    """Appends an immutable event to the service request timeline."""
    event = ServiceRequestTimeline(
        request_id=request_id,
        event_type=event_type,
        title=title,
        actor_id=actor_id,
        description=description,
    )
    db.add(event)
    return event


def validate_status_transition(current_status: str, new_status: str, approval_required: bool, approval_status: str):
    """Enforces valid lifecycle state transitions."""
    if current_status == new_status:
        return

    # Terminal states
    if current_status in [ServiceRequestStatus.CLOSED, ServiceRequestStatus.CANCELLED]:
        raise ValueError(f"Cannot transition from terminal status '{current_status}'.")

    # Pending Approval must be approved before moving forward
    if current_status == ServiceRequestStatus.PENDING_APPROVAL and new_status not in [
        ServiceRequestStatus.APPROVED,
        ServiceRequestStatus.REJECTED,
        ServiceRequestStatus.CANCELLED,
    ]:
        raise ValueError("Requests pending approval must first be Approved or Rejected.")

    # Rejection moves to Closed or stays Rejected
    if current_status == ServiceRequestStatus.REJECTED and new_status not in [
        ServiceRequestStatus.CLOSED,
        ServiceRequestStatus.CANCELLED,
    ]:
        raise ValueError("Rejected requests cannot proceed to fulfillment.")


def fulfill_service_request(
    db: Session,
    sr: ServiceRequest,
    engineer: User,
    fulfillment_details: Dict[str, Any],
    resolution_notes: Optional[str] = None,
) -> ServiceRequest:
    """Fulfills a service request with required technical completion info."""
    if sr.status not in [ServiceRequestStatus.IN_PROGRESS, ServiceRequestStatus.ASSIGNED, ServiceRequestStatus.WAITING_FOR_USER]:
        raise ValueError(f"Cannot fulfill request currently in '{sr.status}' state.")

    # Validate type-specific required completion fields
    req_type_name = sr.request_type.name.lower() if sr.request_type else ""

    if "software" in req_type_name:
        if not fulfillment_details.get("software_version") and not fulfillment_details.get("installation_status"):
            fulfillment_details.setdefault("installation_status", "Installed and Verified")

    elif "laptop" in req_type_name or "hardware" in req_type_name:
        # If asset_id or asset_tag is provided, associate or assign asset to requester
        target_asset_id = fulfillment_details.get("asset_id") or sr.asset_id
        if target_asset_id:
            asset = db.query(Asset).filter(Asset.id == target_asset_id).first()
            if asset:
                asset.status = AssetStatus.IN_USE
                asset.assigned_user_id = sr.requester_id
                db.add(
                    AssetAssignment(
                        asset_id=asset.id,
                        user_id=sr.requester_id,
                        assigned_by=engineer.id,
                    )
                )
                fulfillment_details["asset_tag"] = asset.asset_tag
                fulfillment_details["serial_number"] = asset.serial_number

    elif "password" in req_type_name:
        fulfillment_details.setdefault("mfa_verified", True)
        fulfillment_details.setdefault("temporary_credential_provided", True)

    elif "access" in req_type_name or "license" in req_type_name:
        fulfillment_details.setdefault("access_granted_by", engineer.name)
        fulfillment_details.setdefault("provisioning_result", "Active & Provisioned")

    # Update SR state
    sr.status = ServiceRequestStatus.FULFILLED
    sr.fulfilled_at = get_utc_now()
    sr.fulfillment_details = json.dumps(fulfillment_details)
    sr.resolution_notes = resolution_notes or "Service request successfully fulfilled."

    record_sr_timeline(
        db,
        request_id=sr.id,
        event_type="Fulfilled",
        title="Service Request Fulfilled",
        actor_id=engineer.id,
        description=f"Fulfilled by {engineer.name}. {resolution_notes or ''}",
    )

    log_audit(
        db,
        action="SERVICE_REQUEST_FULFILLED",
        entity_type="ServiceRequest",
        entity_id=sr.id,
        user_id=engineer.id,
        details={"request_number": sr.request_number, "details": fulfillment_details},
    )

    db.commit()
    db.refresh(sr)
    return sr


def confirm_service_request(
    db: Session,
    sr: ServiceRequest,
    user: User,
    feedback_notes: Optional[str] = None,
) -> ServiceRequest:
    """End-user confirms completion, transitioning request from Fulfilled to Closed."""
    if sr.status != ServiceRequestStatus.FULFILLED:
        raise ValueError("Only fulfilled requests can be confirmed.")

    sr.status = ServiceRequestStatus.CLOSED
    sr.closed_at = get_utc_now()

    record_sr_timeline(
        db,
        request_id=sr.id,
        event_type="Confirmed",
        title="User Confirmed Completion",
        actor_id=user.id,
        description=feedback_notes or "Requester confirmed successful service delivery.",
    )

    log_audit(
        db,
        action="SERVICE_REQUEST_CONFIRMED",
        entity_type="ServiceRequest",
        entity_id=sr.id,
        user_id=user.id,
        details={"request_number": sr.request_number},
    )

    db.commit()
    db.refresh(sr)
    return sr


def report_service_request_problem(
    db: Session,
    sr: ServiceRequest,
    user: User,
    problem_description: str,
) -> ServiceRequest:
    """End-user reports problem with fulfillment, returning request to In Progress."""
    if sr.status != ServiceRequestStatus.FULFILLED:
        raise ValueError("Problem reports can only be submitted for fulfilled requests.")

    sr.status = ServiceRequestStatus.IN_PROGRESS
    sr.fulfilled_at = None

    record_sr_timeline(
        db,
        request_id=sr.id,
        event_type="Reported Problem",
        title="Requester Reported Incomplete Fulfillment",
        actor_id=user.id,
        description=problem_description,
    )

    # Post an automatic comment
    db.add(
        ServiceRequestComment(
            request_id=sr.id,
            user_id=user.id,
            comment=f"[RE-OPENED ISSUE]: {problem_description}",
            is_internal=False,
        )
    )

    log_audit(
        db,
        action="SERVICE_REQUEST_PROBLEM_REPORTED",
        entity_type="ServiceRequest",
        entity_id=sr.id,
        user_id=user.id,
        details={"request_number": sr.request_number, "reason": problem_description},
    )

    db.commit()
    db.refresh(sr)
    return sr


def create_onboarding_service_requests(
    db: Session,
    onboarding_id: str,
    employee_name: str,
    department: str,
    creator_id: str,
) -> List[ServiceRequest]:
    """Generates standard onboarding service requests linked to a new hire."""
    # Find request types
    req_types = {rt.name: rt for rt in db.query(ServiceRequestType).all()}
    created_srs = []

    standard_tasks = [
        ("New Employee Account", f"Provision corporate Active Directory & email for {employee_name}", "Account"),
        ("Microsoft 365 License", f"Assign M365 Business Standard license to {employee_name}", "Microsoft 365"),
        ("New Laptop", f"Prepare and configure standard corporate laptop for {employee_name} ({department})", "Hardware"),
        ("VPN Access", f"Provision secure remote VPN access profile for {employee_name}", "Network"),
    ]

    for type_name, desc, category in standard_tasks:
        rt = req_types.get(type_name)
        if not rt:
            continue

        now = get_utc_now()
        sr = ServiceRequest(
            request_number=generate_service_request_number(db),
            title=f"{type_name} — {employee_name}",
            description=desc,
            category=category,
            priority=TicketPriority.HIGH if "Account" in type_name else TicketPriority.MEDIUM,
            status=ServiceRequestStatus.SUBMITTED,
            request_type_id=rt.id,
            requester_id=creator_id,
            approval_required=False,  # Auto-approved via onboarding approval
            approval_status=ServiceRequestApprovalStatus.APPROVED,
            approved_at=now,
            assigned_team=SupportTeam.SERVICE_DESK,
            business_justification=f"Mandatory IT provisioning for new hire {employee_name} ({department}).",
            due_at=now + timedelta(hours=rt.default_sla_hours),
        )
        db.add(sr)
        db.flush()

        record_sr_timeline(
            db,
            request_id=sr.id,
            event_type="Submitted",
            title="Auto-Generated via IT Onboarding",
            actor_id=creator_id,
            description=f"Generated for employee onboarding {employee_name}.",
        )
        created_srs.append(sr)

    db.commit()
    return created_srs


def get_sr_dashboard_metrics(db: Session) -> Dict[str, Any]:
    """Aggregates all real-time operational metrics for Service Requests."""
    now = get_utc_now()
    all_srs = db.query(ServiceRequest).all()

    total_requests = len(all_srs)
    pending_approval = sum(1 for sr in all_srs if sr.status == ServiceRequestStatus.PENDING_APPROVAL)
    unassigned = sum(1 for sr in all_srs if sr.assigned_to is None and sr.status not in [ServiceRequestStatus.CLOSED, ServiceRequestStatus.CANCELLED])
    in_progress = sum(1 for sr in all_srs if sr.status in [ServiceRequestStatus.IN_PROGRESS, ServiceRequestStatus.ASSIGNED, ServiceRequestStatus.WAITING_FOR_USER])
    fulfilled = sum(1 for sr in all_srs if sr.status == ServiceRequestStatus.FULFILLED)
    closed = sum(1 for sr in all_srs if sr.status == ServiceRequestStatus.CLOSED)

    sla_breached = 0
    sla_at_risk = 0
    fulfilled_within_sla = 0
    total_completed = 0
    fulfillment_hours_list = []

    by_type: Dict[str, int] = {}
    by_category: Dict[str, int] = {}
    by_status: Dict[str, int] = {}
    by_team: Dict[str, int] = {}

    for sr in all_srs:
        # Category / Status / Team breakdown
        by_category[sr.category] = by_category.get(sr.category, 0) + 1
        by_status[sr.status] = by_status.get(sr.status, 0) + 1
        team = sr.assigned_team or "Unassigned"
        by_team[team] = by_team.get(team, 0) + 1

        if sr.request_type:
            type_name = sr.request_type.name
            by_type[type_name] = by_type.get(type_name, 0) + 1

        # SLA evaluation
        status, is_breached = calculate_sr_sla_status(sr, now)
        if is_breached:
            sla_breached += 1
        elif status == "At Risk":
            sla_at_risk += 1

        if sr.fulfilled_at is not None:
            total_completed += 1
            if not is_breached:
                fulfilled_within_sla += 1
            f_dt = ensure_utc(sr.fulfilled_at)
            c_dt = ensure_utc(sr.created_at)
            if f_dt and c_dt:
                duration = (f_dt - c_dt).total_seconds() / 3600.0
                fulfillment_hours_list.append(duration)

    sla_compliance_rate = (
        round((fulfilled_within_sla / total_completed) * 100, 1)
        if total_completed > 0
        else (round(((total_requests - sla_breached) / total_requests) * 100, 1) if total_requests > 0 else 100.0)
    )

    avg_fulfillment = (
        round(sum(fulfillment_hours_list) / len(fulfillment_hours_list), 1)
        if fulfillment_hours_list
        else 0.0
    )

    # 14-day daily trend
    daily_trend = []
    for i in range(13, -1, -1):
        target_day = (now - timedelta(days=i)).date()
        day_str = target_day.strftime("%b %d")
        count = sum(1 for sr in all_srs if ensure_utc(sr.created_at).date() == target_day)
        fulfilled_count = sum(
            1 for sr in all_srs if sr.fulfilled_at and ensure_utc(sr.fulfilled_at).date() == target_day
        )
        daily_trend.append({"date": day_str, "created": count, "fulfilled": fulfilled_count})

    return {
        "total_requests": total_requests,
        "pending_approval": pending_approval,
        "unassigned": unassigned,
        "in_progress": in_progress,
        "fulfilled": fulfilled,
        "closed": closed,
        "sla_breached": sla_breached,
        "sla_at_risk": sla_at_risk,
        "sla_compliance_rate": sla_compliance_rate,
        "average_fulfillment_hours": avg_fulfillment,
        "by_type": by_type,
        "by_category": by_category,
        "by_status": by_status,
        "by_team": by_team,
        "daily_trend": daily_trend,
    }
