package com.servicedesk.service;

import com.servicedesk.model.AuditLog;
import com.servicedesk.repository.AuditRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditRepository auditRepository;

    public AuditService(AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    public void logAudit(String userId, String action, String entityType, String entityId, String details) {
        AuditLog log = new AuditLog();
        log.setId(UUID.randomUUID().toString());
        log.setUserId(userId);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setDetails(details);
        log.setCreatedAt(Instant.now());
        auditRepository.save(log);
    }
}
