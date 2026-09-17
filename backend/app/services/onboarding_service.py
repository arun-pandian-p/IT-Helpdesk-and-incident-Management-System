from datetime import date, timedelta
from typing import List
from sqlalchemy.orm import Session
from app.db.models import OnboardingRequest, OnboardingTask, OnboardingTaskStatus


DEFAULT_ONBOARDING_TASKS = [
    "Provision corporate user account & email in Microsoft Entra ID / Workspace",
    "Allocate and image laptop asset from IT Inventory",
    "Enroll device in Intune / Jamf MDM and apply corporate compliance policies",
    "Install standard productivity software (M365 Apps, Slack, Zoom, Browsers)",
    "Configure Multi-Factor Authentication (MFA) and Security Keys",
    "Deploy corporate VPN profile and install root security certificates",
    "Grant departmental security groups, shared drives, and cloud permissions",
    "Provide equipment delivery and schedule Day 1 IT Orientation session",
]


def create_default_tasks_for_request(db: Session, request: OnboardingRequest) -> List[OnboardingTask]:
    """Generates standard enterprise IT onboarding tasks for a new hire."""
    tasks = []
    base_date = request.start_date
    for i, task_name in enumerate(DEFAULT_ONBOARDING_TASKS):
        due_date = base_date - timedelta(days=max(0, 5 - i))
        task = OnboardingTask(
            request_id=request.id,
            task_name=task_name,
            status=OnboardingTaskStatus.NOT_STARTED,
            due_date=due_date,
            notes=f"Preparation task for {request.employee_name} ({request.department})",
        )
        db.add(task)
        tasks.append(task)

    db.flush()
    return tasks


def update_onboarding_progress(db: Session, request_id: str) -> int:
    """Recalculates completion percentage (0-100%) for an onboarding request."""
    request = db.query(OnboardingRequest).filter(OnboardingRequest.id == request_id).first()
    if not request:
        return 0

    total_tasks = len(request.tasks)
    if total_tasks == 0:
        request.progress_percent = 0
    else:
        completed = sum(1 for t in request.tasks if t.status == OnboardingTaskStatus.COMPLETED)
        progress = int((completed / total_tasks) * 100)
        request.progress_percent = progress
        if progress == 100:
            request.status = "Completed"
        elif any(t.status == OnboardingTaskStatus.IN_PROGRESS for t in request.tasks):
            request.status = "In Progress"

    db.flush()
    return request.progress_percent
