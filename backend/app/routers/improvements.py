from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.db.database import get_db
from app.db.models import ImprovementRequest, ImprovementStatus, User, UserRole
from app.dependencies.auth import get_current_user, require_admin, require_support
from app.schemas import ImprovementCreate, ImprovementOut, ImprovementUpdate
from app.services.audit_service import log_audit

router = APIRouter(prefix="/improvements", tags=["Continuous Improvement"])


def format_improvement_out(imp: ImprovementRequest, db: Session) -> ImprovementOut:
    submitter = db.query(User).filter(User.id == imp.submitted_by).first()
    assignee = db.query(User).filter(User.id == imp.assigned_to).first() if imp.assigned_to else None

    return ImprovementOut(
        id=imp.id,
        title=imp.title,
        category=imp.category,
        description=imp.description,
        business_benefit=imp.business_benefit,
        status=imp.status,
        priority=imp.priority,
        submitted_by=imp.submitted_by,
        submitter_name=submitter.name if submitter else "Unknown",
        assigned_to=imp.assigned_to,
        assignee_name=assignee.name if assignee else None,
        created_at=imp.created_at,
    )


@router.get("", response_model=List[ImprovementOut])
def list_improvements(
    category: Optional[str] = None,
    status: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Lists continuous process and technology improvement initiatives."""
    query = db.query(ImprovementRequest)
    if category:
        query = query.filter(ImprovementRequest.category == category)
    if status:
        query = query.filter(ImprovementRequest.status == status)

    improvements = query.order_by(ImprovementRequest.created_at.desc()).all()
    return [format_improvement_out(i, db) for i in improvements]


@router.post("", response_model=ImprovementOut, status_code=status.HTTP_201_CREATED)
def create_improvement(
    payload: ImprovementCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Submits a new IT service process or technology improvement proposal."""
    imp = ImprovementRequest(
        title=payload.title,
        category=payload.category,
        description=payload.description,
        business_benefit=payload.business_benefit,
        priority=payload.priority or "Medium",
        status=ImprovementStatus.PROPOSED,
        submitted_by=current_user.id,
    )
    db.add(imp)
    db.flush()

    log_audit(
        db,
        action="IMPROVEMENT_INITIATIVE_SUBMITTED",
        entity_type="ImprovementRequest",
        entity_id=imp.id,
        user_id=current_user.id,
        details={"title": imp.title, "category": imp.category},
    )

    db.commit()
    db.refresh(imp)
    return format_improvement_out(imp, db)


@router.patch("/{improvement_id}", response_model=ImprovementOut)
def update_improvement(
    improvement_id: str,
    payload: ImprovementUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    """Updates status or assignment of an improvement initiative."""
    imp = db.query(ImprovementRequest).filter(ImprovementRequest.id == improvement_id).first()
    if not imp:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Initiative not found.")

    update_data = payload.model_dump(exclude_unset=True)
    for k, v in update_data.items():
        setattr(imp, k, v)

    log_audit(
        db,
        action="IMPROVEMENT_UPDATED",
        entity_type="ImprovementRequest",
        entity_id=imp.id,
        user_id=current_user.id,
        details=update_data,
    )

    db.commit()
    db.refresh(imp)
    return format_improvement_out(imp, db)
