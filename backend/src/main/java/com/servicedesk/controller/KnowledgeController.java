package com.servicedesk.controller;

import com.servicedesk.dto.CommonDtos.KnowledgeCreateRequest;
import com.servicedesk.model.KnowledgeArticle;
import com.servicedesk.security.UserPrincipal;
import com.servicedesk.service.KnowledgeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    public KnowledgeController(KnowledgeService knowledgeService) {
        this.knowledgeService = knowledgeService;
    }

    @GetMapping
    public ResponseEntity<List<KnowledgeArticle>> getArticles(
            @RequestParam(required = false) String category,
            @RequestParam(name = "os_target", required = false) String osTarget,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(knowledgeService.getArticles(category, osTarget, search));
    }

    @GetMapping("/{id}")
    public ResponseEntity<KnowledgeArticle> getArticle(@PathVariable String id) {
        return ResponseEntity.ok(knowledgeService.getArticleById(id));
    }

    @PostMapping
    public ResponseEntity<KnowledgeArticle> createArticle(
            @Valid @RequestBody KnowledgeCreateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        KnowledgeArticle created = knowledgeService.createArticle(req, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{id}/vote")
    public ResponseEntity<KnowledgeArticle> voteArticle(
            @PathVariable String id,
            @RequestParam boolean helpful) {
        return ResponseEntity.ok(knowledgeService.voteArticle(id, helpful));
    }
}
