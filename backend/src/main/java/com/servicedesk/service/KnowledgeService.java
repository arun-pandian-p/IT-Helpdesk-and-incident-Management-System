package com.servicedesk.service;

import com.servicedesk.dto.CommonDtos.KnowledgeCreateRequest;
import com.servicedesk.exception.AppException;
import com.servicedesk.model.KnowledgeArticle;
import com.servicedesk.repository.KnowledgeRepository;
import com.servicedesk.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class KnowledgeService {

    private final KnowledgeRepository knowledgeRepository;
    private final AuditService auditService;

    public KnowledgeService(KnowledgeRepository knowledgeRepository, AuditService auditService) {
        this.knowledgeRepository = knowledgeRepository;
        this.auditService = auditService;
    }

    public List<KnowledgeArticle> getArticles(String category, String osTarget, String search) {
        return knowledgeRepository.findAll(category, osTarget, search);
    }

    public KnowledgeArticle getArticleById(String id) {
        KnowledgeArticle a = knowledgeRepository.findById(id)
                .orElseThrow(() -> new AppException("Article not found.", HttpStatus.NOT_FOUND));
        knowledgeRepository.incrementViewCount(id);
        a.setViewCount(a.getViewCount() + 1);
        return a;
    }

    @Transactional
    public KnowledgeArticle createArticle(KnowledgeCreateRequest req, UserPrincipal principal) {
        Instant now = Instant.now();
        KnowledgeArticle a = new KnowledgeArticle();
        a.setId(UUID.randomUUID().toString());
        a.setTitle(req.getTitle());
        a.setCategory(req.getCategory());
        a.setOsTarget(req.getOsTarget() != null ? req.getOsTarget() : "All");
        a.setContent(req.getContent());
        a.setTags(req.getTags());
        a.setViewCount(0);
        a.setHelpfulCount(0);
        a.setNotHelpfulCount(0);
        a.setCreatedBy(principal.getId());
        a.setCreatedAt(now);
        a.setUpdatedAt(now);

        knowledgeRepository.save(a);
        auditService.logAudit(principal.getId(), "KB_ARTICLE_CREATED", "KnowledgeArticle", a.getId(),
                "Created article: " + a.getTitle());

        return knowledgeRepository.findById(a.getId()).orElse(a);
    }

    @Transactional
    public KnowledgeArticle voteArticle(String id, boolean helpful) {
        knowledgeRepository.findById(id)
                .orElseThrow(() -> new AppException("Article not found.", HttpStatus.NOT_FOUND));

        knowledgeRepository.recordVote(id, helpful);
        return knowledgeRepository.findById(id).orElse(null);
    }
}
