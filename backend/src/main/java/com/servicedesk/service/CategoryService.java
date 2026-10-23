package com.servicedesk.service;

import com.servicedesk.dto.CommonDtos.CategoryCreateRequest;
import com.servicedesk.exception.AppException;
import com.servicedesk.model.Constants.UserRole;
import com.servicedesk.model.TicketCategory;
import com.servicedesk.repository.CategoryRepository;
import com.servicedesk.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<TicketCategory> getCategories() {
        return categoryRepository.findAll();
    }

    @Transactional
    public TicketCategory createCategory(CategoryCreateRequest req, UserPrincipal principal) {
        if (principal == null || (!UserRole.SUPPORT.equals(principal.getRole()) && !UserRole.ADMIN.equals(principal.getRole()))) {
            throw new AppException("Access forbidden: requires SUPPORT or ADMIN role.", HttpStatus.FORBIDDEN);
        }

        if (categoryRepository.findByName(req.getName()).isPresent()) {
            throw new AppException("Category with name '" + req.getName() + "' already exists.", HttpStatus.BAD_REQUEST);
        }

        TicketCategory cat = new TicketCategory();
        cat.setId(UUID.randomUUID().toString());
        cat.setName(req.getName());
        cat.setDescription(req.getDescription());
        cat.setActive(true);
        cat.setCreatedAt(Instant.now());

        categoryRepository.save(cat);
        return cat;
    }
}
