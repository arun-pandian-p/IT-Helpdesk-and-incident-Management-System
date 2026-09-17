from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.core.security import create_access_token, get_password_hash, verify_password
from app.db.database import get_db
from app.db.models import User, UserRole
from app.dependencies.auth import get_current_user
from app.schemas import LoginRequest, RegisterRequest, TokenResponse, UserOut
from app.services.audit_service import log_audit

router = APIRouter(prefix="/auth", tags=["Authentication"])


@router.post("/register", response_model=TokenResponse, status_code=status.HTTP_201_CREATED)
def register(request: RegisterRequest, db: Session = Depends(get_db)):
    """Registers a new internal employee account."""
    existing_user = db.query(User).filter(User.email == request.email.lower()).first()
    if existing_user:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="A user with this email address already exists.",
        )

    user = User(
        name=request.name,
        email=request.email.lower(),
        password_hash=get_password_hash(request.password),
        role=UserRole.EMPLOYEE,
        department=request.department or "General",
        job_title=request.job_title or "Staff",
        phone=request.phone,
        location=request.location or "Headquarters",
        is_active=True,
    )
    db.add(user)
    db.commit()
    db.refresh(user)

    log_audit(
        db,
        action="USER_REGISTERED",
        entity_type="User",
        entity_id=user.id,
        user_id=user.id,
        details={"name": user.name, "email": user.email, "role": user.role},
    )
    db.commit()

    token = create_access_token(
        subject=user.id,
        role=user.role,
        email=user.email,
        name=user.name,
    )
    return TokenResponse(access_token=token, user=UserOut.model_validate(user))


@router.post("/login", response_model=TokenResponse)
def login(request: LoginRequest, db: Session = Depends(get_db)):
    """Authenticates credentials and returns signed JWT access token."""
    user = db.query(User).filter(User.email == request.email.lower()).first()
    if not user or not verify_password(request.password, user.password_hash):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid email or password.",
            headers={"WWW-Authenticate": "Bearer"},
        )

    if not user.is_active:
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="User account is deactivated. Contact IT support.",
        )

    token = create_access_token(
        subject=user.id,
        role=user.role,
        email=user.email,
        name=user.name,
    )

    log_audit(
        db,
        action="USER_LOGIN",
        entity_type="User",
        entity_id=user.id,
        user_id=user.id,
        details={"email": user.email, "role": user.role},
    )
    db.commit()

    return TokenResponse(access_token=token, user=UserOut.model_validate(user))


@router.get("/me", response_model=UserOut)
def get_me(current_user: User = Depends(get_current_user)):
    """Retrieves profile of currently authenticated user."""
    return UserOut.model_validate(current_user)
