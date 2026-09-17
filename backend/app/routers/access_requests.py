from datetime import datetime, timezone
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session, joinedload

from app.db.database import get_db
from app.db.models import AccessRequest, AccessRequestStatus, User, UserRole
from app.dependencies.auth import get_current_user, require_support
from app.schemas import AccessRequestCreate, AccessRequestOut, AccessRequestStatusUpdate
from app.services.audit_service import log_audit

router = APIRouter(prefix="/access-requests", tags=["Access & Account Management"])


def format_access_out(req: AccessRequest, db: Session) -> AccessRequestOut:
    requester = db.query(User).filter(User.id == req.requested_by).first()
    approver = db.query(User).filter(User.id == req.approved_by).first() if req.approved_by else None

    return AccessRequestOut(
        id=req.id,
        request_type=req.request_type,
        requested_system=req.requested_system,
        access_level=req.access_level,
        reason=req.reason,
        requested_by=req.requested_by,
        requester_name=requester.name if requester else "Unknown",
        approved_by=req.approved_by,
        approver_name=approver.name if approver else None,
        status=req.status,
        completed_at=req.completed_at,
        created_at=req.created_at,
    )


@router.get("", response_model=List[AccessRequestOut])
def list_access_requests(
    status: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Lists account and access requests. Employees see own; Support/Admin see all."""
    query = db.query(AccessRequest)
    if current_user.role == UserRole.EMPLOYEE:
        query = query.filter(AccessRequest.requested_by == current_user.id)
    if status:
        query = query.filter(AccessRequest.status == status)

    requests = query.order_by(AccessRequest.created_at.desc()).all()
    return [format_access_out(r, db) for r in requests]


@router.post("", response_model=AccessRequestOut, status_code=status.HTTP_201_CREATED)
def create_access_request(
    payload: AccessRequestCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Submits request for application access, password reset, MFA reset, or shared drive permissions."""
    req = AccessRequest(
        request_type=payload.request_type,
        requested_system=payload.requested_system,
        access_level=payload.access_level or "Read/Write",
        reason=payload.reason,
        requested_by=current_user.id,
        status=AccessRequestStatus.REQUESTED,
    )
    db.add(req)
    db.flush()

    log_audit(
        db,
        action="ACCESS_REQUESTED",
        entity_type="AccessRequest",
        entity_id=req.id,
        user_id=current_user.id,
        details={"type": req.request_type, "system": req.requested_system},
    )

    db.commit()
    db.refresh(req)
    return format_access_out(req, db)


@router.patch("/{request_id}/status", response_model=AccessRequestOut)
def update_access_status(
    request_id: str,
    payload: AccessRequestStatusUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_support),
):
    """Approves, rejects, or completes access provision requests."""
    req = db.query(AccessRequest).filter(AccessRequest.id == request_id).first()
    if not req:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Access request not found.")

    old_status = req.status
    req.status = payload.status
    if payload.status == AccessRequestStatus.APPROVED:
        req.approved_by = current_user.id
    elif payload.status == AccessRequestStatus.COMPLETED:
        req.completed_at = datetime.now(timezone.utc)
        if not req.approved_by:
            req.approved_by = current_user.id

    log_audit(
        db,
        action="ACCESS_REQUEST_STATUS_UPDATED",
        entity_type="AccessRequest",
        entity_id=req.id,
        user_id=current_user.id,
        details={"old_status": old_status, "new_status": req.status},
    )

    db.commit()
    db.refresh(req)
    return format_access_out(req, db)
