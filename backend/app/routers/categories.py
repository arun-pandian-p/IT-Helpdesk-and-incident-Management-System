from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.db.database import get_db
from app.db.models import TicketCategory, User
from app.dependencies.auth import get_current_user, require_admin
from app.schemas import CategoryCreate, CategoryOut, CategoryUpdate
from app.services.audit_service import log_audit

router = APIRouter(prefix="/categories", tags=["Categories"])


@router.get("", response_model=List[CategoryOut])
def list_categories(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Lists all active IT support ticket categories."""
    categories = db.query(TicketCategory).filter(TicketCategory.is_active == True).order_by(TicketCategory.name).all()
    return [CategoryOut.model_validate(c) for c in categories]


@router.post("", response_model=CategoryOut, status_code=status.HTTP_201_CREATED)
def create_category(
    payload: CategoryCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    """Creates a new incident category (Admin only)."""
    existing = db.query(TicketCategory).filter(TicketCategory.name.ilike(payload.name)).first()
    if existing:
        raise HTTPException(status_code=status.HTTP_400_BAD_REQUEST, detail="Category already exists.")

    cat = TicketCategory(name=payload.name, description=payload.description)
    db.add(cat)
    db.flush()

    log_audit(
        db,
        action="CATEGORY_CREATED",
        entity_type="TicketCategory",
        entity_id=cat.id,
        user_id=current_user.id,
        details={"name": cat.name},
    )

    db.commit()
    db.refresh(cat)
    return CategoryOut.model_validate(cat)


@router.patch("/{category_id}", response_model=CategoryOut)
def update_category(
    category_id: str,
    payload: CategoryUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_admin),
):
    """Updates category name, description, or active status (Admin only)."""
    cat = db.query(TicketCategory).filter(TicketCategory.id == category_id).first()
    if not cat:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Category not found.")

    update_data = payload.model_dump(exclude_unset=True)
    for k, v in update_data.items():
        setattr(cat, k, v)

    db.commit()
    db.refresh(cat)
    return CategoryOut.model_validate(cat)
