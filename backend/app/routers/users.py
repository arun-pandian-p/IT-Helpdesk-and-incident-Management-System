from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.core.security import get_password_hash
from app.db.database import get_db
from app.db.models import AuditLog, User, UserRole
from app.dependencies.auth import get_current_user, require_admin, require_support
from app.schemas import AuditLogOut, UserCreate, UserOut, UserUpdate
from app.services.audit_service import log_audit

router = APIRouter(prefix="/users", tags=["User Administration"])


@router.get("", response_model=List[UserOut])
def list_users(
    role: Optional[str] = None,
    department: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Lists enterprise users. Filterable by role and department."""
    query = db.query(User).filter(User.is_active == True)
    if role:
        query = query.filter(User.role == role)
    if department:
        query = query.filter(User.department == department)

    users = query.order_by(User.name).all()
    return [UserOut.model_validate(u) for u in users]


@router.get("/support-staff", response_model=List[UserOut])
def list_support_staff(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Returns all active Support Engineers and Admins available for ticket assignment."""
    users = (
        db.query(User)
        .filter(User.is_active == True, User.role.in_([UserRole.SUPPORT, UserRole.ADMIN]))
        .order_by(User.name)
        .all()
    )
    return [UserOut.model_validate(u) for u in users]


@router.get("/{user_id}", response_model=UserOut)
def get_user(
    user_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Retrieves user profile details."""
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found.")
    return UserOut.model_validate(user)


@router.post("", response_model=UserOut, status_code=status.HTTP_201_CREATED)
def create_user(
    payload: UserCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    """Provisions a new employee, support engineer, or admin account (Admin only)."""
    existing = db.query(User).filter(User.email == payload.email.lower()).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="A user with this email already exists.",
        )

    user = User(
        name=payload.name,
        email=payload.email.lower(),
        password_hash=get_password_hash(payload.password),
        role=payload.role,
        department=payload.department,
        job_title=payload.job_title,
        phone=payload.phone,
        location=payload.location or "Headquarters",
        is_active=True,
    )
    db.add(user)
    db.flush()

    log_audit(
        db,
        action="USER_PROVISIONED_BY_ADMIN",
        entity_type="User",
        entity_id=user.id,
        user_id=current_user.id,
        details={"email": user.email, "role": user.role},
    )

    db.commit()
    db.refresh(user)
    return UserOut.model_validate(user)


@router.patch("/{user_id}", response_model=UserOut)
def update_user(
    user_id: str,
    payload: UserUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    """Updates user role, department, or active status (Admin only)."""
    user = db.query(User).filter(User.id == user_id).first()
    if not user:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="User not found.")

    update_data = payload.model_dump(exclude_unset=True)
    for k, v in update_data.items():
        setattr(user, k, v)

    log_audit(
        db,
        action="USER_UPDATED_BY_ADMIN",
        entity_type="User",
        entity_id=user.id,
        user_id=current_user.id,
        details=update_data,
    )

    db.commit()
    db.refresh(user)
    return UserOut.model_validate(user)


@router.get("/audit/logs", response_model=List[AuditLogOut])
def get_audit_logs(
    limit: int = Query(50, ge=1, le=200),
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    """Retrieves immutable audit trail history for enterprise compliance (Admin only)."""
    logs = (
        db.query(AuditLog)
        .order_by(AuditLog.created_at.desc())
        .limit(limit)
        .all()
    )
    result = []
    for l in logs:
        result.append(
            AuditLogOut(
                id=l.id,
                user_id=l.user_id,
                user_name=l.user.name if l.user else "System",
                action=l.action,
                entity_type=l.entity_type,
                entity_id=l.entity_id,
                details=l.details,
                created_at=l.created_at,
            )
        )
    return result
