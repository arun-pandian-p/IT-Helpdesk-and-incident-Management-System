from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy import or_
from sqlalchemy.orm import Session

from app.db.database import get_db
from app.db.models import KnowledgeArticle, User
from app.dependencies.auth import get_current_user, require_support
from app.schemas import (
    KnowledgeArticleCreate,
    KnowledgeArticleOut,
    KnowledgeArticleUpdate,
)
from app.services.audit_service import log_audit

router = APIRouter(prefix="/knowledge", tags=["Knowledge Base & User Guidance"])


@router.get("", response_model=List[KnowledgeArticleOut])
def list_articles(
    category: Optional[str] = None,
    os_target: Optional[str] = None,
    search: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Searches self-service IT support articles and OS setup guides."""
    query = db.query(KnowledgeArticle)

    if category:
        query = query.filter(KnowledgeArticle.category == category)
    if os_target and os_target != "All":
        query = query.filter(
            or_(
                KnowledgeArticle.os_target == os_target,
                KnowledgeArticle.os_target == "All",
            )
        )
    if search:
        pattern = f"%{search.strip()}%"
        query = query.filter(
            or_(
                KnowledgeArticle.title.ilike(pattern),
                KnowledgeArticle.content.ilike(pattern),
                KnowledgeArticle.tags.ilike(pattern),
            )
        )

    articles = query.order_by(KnowledgeArticle.view_count.desc()).all()
    return [KnowledgeArticleOut.model_validate(a) for a in articles]


@router.get("/{article_id}", response_model=KnowledgeArticleOut)
def get_article(
    article_id: str,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Retrieves article content and increments view count."""
    article = db.query(KnowledgeArticle).filter(KnowledgeArticle.id == article_id).first()
    if not article:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Article not found.")

    article.view_count += 1
    db.commit()
    db.refresh(article)
    return KnowledgeArticleOut.model_validate(article)


@router.post("", response_model=KnowledgeArticleOut, status_code=status.HTTP_201_CREATED)
def create_article(
    payload: KnowledgeArticleCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(require_support),
):
    """Publishes a new IT knowledge base troubleshooting guide."""
    article = KnowledgeArticle(
        title=payload.title,
        category=payload.category,
        os_target=payload.os_target or "All",
        content=payload.content,
        tags=payload.tags,
        created_by=current_user.id,
    )
    db.add(article)
    db.flush()

    log_audit(
        db,
        action="KNOWLEDGE_ARTICLE_CREATED",
        entity_type="KnowledgeArticle",
        entity_id=article.id,
        user_id=current_user.id,
        details={"title": article.title, "category": article.category},
    )

    db.commit()
    db.refresh(article)
    return KnowledgeArticleOut.model_validate(article)


@router.post("/{article_id}/vote", response_model=KnowledgeArticleOut)
def vote_article(
    article_id: str,
    helpful: bool = Query(..., description="True if article solved issue, False otherwise"),
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user),
):
    """Records user feedback on article helpfulness."""
    article = db.query(KnowledgeArticle).filter(KnowledgeArticle.id == article_id).first()
    if not article:
        raise HTTPException(status_code=status.HTTP_404_NOT_FOUND, detail="Article not found.")

    if helpful:
        article.helpful_count += 1
    else:
        article.not_helpful_count += 1

    db.commit()
    db.refresh(article)
    return KnowledgeArticleOut.model_validate(article)
