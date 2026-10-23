package com.servicedesk.controller;

import com.servicedesk.dto.CommonDtos.CategoryCreateRequest;
import com.servicedesk.model.TicketCategory;
import com.servicedesk.security.UserPrincipal;
import com.servicedesk.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<TicketCategory>> getCategories() {
        return ResponseEntity.ok(categoryService.getCategories());
    }

    @PostMapping
    public ResponseEntity<TicketCategory> createCategory(
            @Valid @RequestBody CategoryCreateRequest req,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketCategory cat = categoryService.createCategory(req, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(cat);
    }
}
