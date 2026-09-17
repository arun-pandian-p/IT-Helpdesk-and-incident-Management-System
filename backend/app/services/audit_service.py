import json
from typing import Any, Optional
from sqlalchemy.orm import Session
from app.db.models import AuditLog


def log_audit(
    db: Session,
    action: str,
    entity_type: str,
    entity_id: str,
    user_id: Optional[str] = None,
    details: Optional[Any] = None,
) -> AuditLog:
    """Record an audit trail event for compliance and traceability."""
    details_str = None
    if details is not None:
        if isinstance(details, (dict, list)):
            details_str = json.dumps(details)
        else:
            details_str = str(details)

    log_entry = AuditLog(
        user_id=user_id,
        action=action,
        entity_type=entity_type,
        entity_id=str(entity_id),
        details=details_str,
    )
    db.add(log_entry)
    db.flush()
    return log_entry
