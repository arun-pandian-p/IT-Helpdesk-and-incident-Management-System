package com.servicedesk.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SystemController {

    @GetMapping("/")
    public Map<String, Object> root() {
        return Map.of(
                "service", "IT Helpdesk & Incident Management System",
                "status", "online",
                "docs_url", "/docs",
                "health_url", "/health",
                "version", "1.0.0"
        );
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "ok",
                "version", "1.0.0"
        );
    }

    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }
}
