from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session, joinedload

from app.db.database import get_db
from app.db.models import OnboardingRequest, OnboardingTask, User, UserRole
from app.dependencies.auth import get_current_user, require_support
from app.schemas import (
    OnboardingRequestCreate,
    OnboardingRequestOut,
    OnboardingTaskOut,
    OnboardingTaskUpdate,
)
from app.services.audit_service import log_audit
from app.services.onboarding_service import (
    create_default_tasks_for_request,
    update_onboarding_progress,
)

router = APIRouter(prefix="/onboarding", tags=["IT Onboarding"])


def format_request_out(req: OnboardingRequest) -> OnboardingRequestOut:
    tasks_out = []
    if req.tasks:
        for t in req.tasks:
            tasks_out.append(
                OnboardingTaskOut(
                    id=t.id,
                    request_id=t.request_id,
                    task_name=t.task_name,
                    owner_id=t.owner_id,
                    owner_name=None,
                    status=t.status,
                    due_date=t.due_date,
                    completed_at=t.completed_at,
                    notes=t.notes,
                )
            )

    return OnboardingRequestOut(
        id=req.id,
        employee_name=req.employee_name,
        department=req.department,
        job_title=req.job_title,
        manager_name=req.manager_name,
        start_date=req.start_date,
        location=req.location,
        device_requirement=req.device_requirement,
        software_requirement=req.software_requirement,
        access_requirement=req.access_requirement,
        status=req.status,
        progress_percent=req.progress_percent,
        created_by=req.created_by,
        created_at=req.created_at,
        tasks=tasks_out,
    )


@router.get("", response_model=List[OnboardingRequestOut])
def list_onboarding_requests(
    department: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Lists all active employee IT onboarding requests with progress bars."""
    query = db.query(OnboardingRequest).options(joinedload(OnboardingRequest.tasks))
    if department:
        query = query.filter(OnboardingRequest.department == department)

    requests = query.order_by(OnboardingRequest.start_date.desc()).all()
    return [format_request_out(r) for r in requests]


@router.post("", response_model=OnboardingRequestOut, status_code=status.HTTP_201_CREATED)
def create_onboarding_request(
    payload: OnboardingRequestCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """
    Submits a new hire IT onboarding request.
    Automatically generates standardized enterprise checklist tasks (Account, Hardware, MDM, M365, VPN).
    """
    request = OnboardingRequest(
        employee_name=payload.employee_name,
        department=payload.department,
        job_title=payload.job_title,
        manager_name=payload.manager_name,
        start_date=payload.start_date,
        location=payload.location or "Headquarters",
        device_requirement=payload.device_requirement or "Standard Laptop",
        software_requirement=payload.software_requirement,
        access_requirement=payload.access_requirement,
        status="In Progress",
        progress_percent=0,
        created_by=current_user.id,
    )
    db.add(request)
    db.flush()

    # Generate standard checklist tasks
    create_default_tasks_for_request(db, request)

    log_audit(
        db,
        action="ONBOARDING_REQUESTED",
        entity_type="OnboardingRequest",
        entity_id=request.id,
        user_id=current_user.id,
        details={"employee_name": request.employee_name, "start_date": request.start_date.isoformat()},
    )

    db.commit()
    db.refresh(request)
    return format_request_out(request)


@router.get("/{request_id}", response_model=OnboardingRequestOut)
def get_onboarding_request(
    request_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Gets details and task progress of an onboarding request."""
    request = (
        db.query(OnboardingRequest)
        .options(joinedload(OnboardingRequest.tasks))
        .filter(OnboardingRequest.id == request_id)
        .first()
    )
    if not request:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Onboarding request not found.")
    return format_request_out(request)


@router.patch("/tasks/{task_id}", response_model=OnboardingTaskOut)
def update_task(
    task_id: str,
    payload: OnboardingTaskUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_support),
):
    """Updates status or notes of an onboarding task and recalculates request progress (0-100%)."""
    task = db.query(OnboardingTask).filter(OnboardingTask.id == task_id).first()
    if not task:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Task not found.")

    if payload.status:
        task.status = payload.status
    if payload.notes is not None:
        task.notes = payload.notes
    if payload.owner_id:
        task.owner_id = payload.owner_id

    db.flush()
    # Recalculate parent request progress
    update_onboarding_progress(db, task.request_id)

    db.commit()
    db.refresh(task)

    return OnboardingTaskOut(
        id=task.id,
        request_id=task.request_id,
        task_name=task.task_name,
        owner_id=task.owner_id,
        owner_name=None,
        status=task.status,
        due_date=task.due_date,
        completed_at=task.completed_at,
        notes=task.notes,
    )
