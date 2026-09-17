from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session, joinedload

from app.db.database import get_db
from app.db.models import Asset, AssetAssignment, AssetStatus, User
from app.dependencies.auth import get_current_user, require_support
from app.schemas import AssetCreate, AssetOut, AssetUpdate
from app.services.audit_service import log_audit

router = APIRouter(prefix="/assets", tags=["Assets & Hardware"])


def format_asset_out(asset: Asset) -> AssetOut:
    return AssetOut(
        id=asset.id,
        asset_tag=asset.asset_tag,
        device_type=asset.device_type,
        manufacturer=asset.manufacturer,
        model=asset.model,
        serial_number=asset.serial_number,
        status=asset.status,
        assigned_user_id=asset.assigned_user_id,
        assigned_user_name=asset.assigned_user.name if asset.assigned_user else None,
        purchase_date=asset.purchase_date,
        warranty_expiry=asset.warranty_expiry,
        notes=asset.notes,
        created_at=asset.created_at,
    )


@router.get("", response_model=List[AssetOut])
def list_assets(
    device_type: Optional[str] = None,
    status: Optional[str] = None,
    assigned_user_id: Optional[str] = None,
    search: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Lists enterprise hardware inventory. Filterable by type, status, and assigned user."""
    query = db.query(Asset).options(joinedload(Asset.assigned_user))

    if device_type:
        query = query.filter(Asset.device_type == device_type)
    if status:
        query = query.filter(Asset.status == status)
    if assigned_user_id:
        query = query.filter(Asset.assigned_user_id == assigned_user_id)
    if search:
        pattern = f"%{search.strip()}%"
        query = query.filter(
            (Asset.asset_tag.ilike(pattern))
            | (Asset.model.ilike(pattern))
            | (Asset.serial_number.ilike(pattern))
            | (Asset.manufacturer.ilike(pattern))
        )

    assets = query.order_by(Asset.asset_tag).all()
    return [format_asset_out(a) for a in assets]


@router.get("/my-devices", response_model=List[AssetOut])
def get_my_devices(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Returns hardware assets currently allocated to the authenticated user."""
    assets = (
        db.query(Asset)
        .options(joinedload(Asset.assigned_user))
        .filter(Asset.assigned_user_id == current_user.id)
        .all()
    )
    return [format_asset_out(a) for a in assets]


@router.get("/{asset_id}", response_model=AssetOut)
def get_asset(
    asset_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Retrieves single device hardware details."""
    asset = (
        db.query(Asset)
        .options(joinedload(Asset.assigned_user))
        .filter(Asset.id == asset_id)
        .first()
    )
    if not asset:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Asset not found.")
    return format_asset_out(asset)


@router.post("", response_model=AssetOut, status_code=status.HTTP_201_CREATED)
def create_asset(
    payload: AssetCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_support),
):
    """Registers a new hardware asset into inventory."""
    existing = db.query(Asset).filter(Asset.asset_tag == payload.asset_tag).first()
    if existing:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Asset tag already registered in system.",
        )

    asset = Asset(
        asset_tag=payload.asset_tag,
        device_type=payload.device_type,
        manufacturer=payload.manufacturer,
        model=payload.model,
        serial_number=payload.serial_number,
        status=payload.status or AssetStatus.IN_USE,
        assigned_user_id=payload.assigned_user_id,
        purchase_date=payload.purchase_date,
        warranty_expiry=payload.warranty_expiry,
        notes=payload.notes,
    )
    db.add(asset)
    db.flush()

    if payload.assigned_user_id:
        assignment = AssetAssignment(
            asset_id=asset.id,
            user_id=payload.assigned_user_id,
            assigned_by=current_user.id,
        )
        db.add(assignment)

    log_audit(
        db,
        action="ASSET_CREATED",
        entity_type="Asset",
        entity_id=asset.id,
        user_id=current_user.id,
        details={"asset_tag": asset.asset_tag, "model": asset.model},
    )

    db.commit()
    db.refresh(asset)
    return format_asset_out(asset)


@router.patch("/{asset_id}", response_model=AssetOut)
def update_asset(
    asset_id: str,
    payload: AssetUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_support),
):
    """Updates device specifications, warranty, or reassigns user."""
    asset = db.query(Asset).filter(Asset.id == asset_id).first()
    if not asset:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Asset not found.")

    update_data = payload.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(asset, field, value)

    log_audit(
        db,
        action="ASSET_UPDATED",
        entity_type="Asset",
        entity_id=asset.id,
        user_id=current_user.id,
        details=update_data,
    )

    db.commit()
    db.refresh(asset)
    return format_asset_out(asset)
