import logging
from contextlib import asynccontextmanager
from fastapi import FastAPI, Request, status
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from app.core.config import settings
from app.db.base import Base
from app.db.database import engine
from app.routers import (
    access_requests,
    assets,
    auth,
    categories,
    dashboard,
    improvements,
    knowledge,
    onboarding,
    tickets,
    users,
)

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger("helpdesk_app")


@asynccontextmanager
async def lifespan(app: FastAPI):
    """Initializes database tables and seeds demo dataset if empty."""
    logger.info("Initializing IT Helpdesk application database models...")
    Base.metadata.create_all(bind=engine)
    logger.info("Database initialized successfully.")
    
    # Check if database needs initial seeding
    from app.db.database import SessionLocal
    from app.db.models import User
    from app.services.seed_service import seed_database

    db = SessionLocal()
    try:
        admin_user = db.query(User).filter(User.email == "admin@example.com").first()
        if not admin_user:
            logger.info("Demo users not found. Automatically populating enterprise demo data...")
            seed_database(db)
            logger.info("Demo database seeded successfully.")
        else:
            user_count = db.query(User).count()
            logger.info(f"Database already populated ({user_count} users).")
    except Exception as e:
        logger.error(f"Error during startup database check/seed: {e}", exc_info=True)
    finally:
        db.close()

    yield
    logger.info("Shutting down IT Helpdesk application...")


app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    description=(
        "Production-grade, ITIL-inspired IT Helpdesk & End-User Support Management API. "
        "Provides end-user technical support, incident management, SLA tracking, "
        "hardware/OS diagnostics, Microsoft 365 / Google Workspace support, "
        "IT onboarding, knowledge management, and operational analytics."
    ),
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
)

# Configure CORS
origins = settings.CORS_ORIGINS
if isinstance(origins, str):
    origins = [o.strip() for o in origins.split(",") if o.strip()]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins if origins else ["http://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Global Exception Handler
@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logger.error(f"Unhandled error during request {request.method} {request.url.path}: {exc}", exc_info=True)
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={"detail": "An internal server error occurred. Please contact IT Helpdesk administration."},
    )


# Root & Health Check
@app.get("/", tags=["System"])
def root():
    """Welcome endpoint for root health and documentation discovery."""
    return {
        "service": settings.PROJECT_NAME,
        "status": "online",
        "docs_url": "/docs",
        "health_url": "/health",
        "version": settings.VERSION,
    }


@app.get("/favicon.ico", include_in_schema=False)
def favicon():
    """Avoid 404 logs for browser favicon requests."""
    return JSONResponse(status_code=status.HTTP_204_NO_CONTENT, content=None)


@app.get("/health", tags=["System"])
def health_check():
    """Health check endpoint for container orchestrators and monitoring."""
    return {"status": "ok", "version": settings.VERSION}


# Include Routers
app.include_router(auth.router, prefix=settings.API_V1_STR)
app.include_router(tickets.router, prefix=settings.API_V1_STR)
app.include_router(assets.router, prefix=settings.API_V1_STR)
app.include_router(onboarding.router, prefix=settings.API_V1_STR)
app.include_router(access_requests.router, prefix=settings.API_V1_STR)
app.include_router(knowledge.router, prefix=settings.API_V1_STR)
app.include_router(improvements.router, prefix=settings.API_V1_STR)
app.include_router(categories.router, prefix=settings.API_V1_STR)
app.include_router(users.router, prefix=settings.API_V1_STR)
app.include_router(dashboard.router, prefix=settings.API_V1_STR)
