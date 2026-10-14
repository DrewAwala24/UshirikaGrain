package com.zuriagro.service;

import com.zuriagro.domain.entity.AuditLog;
import com.zuriagro.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void logAction(String entityName, Long entityId, String action, String performedByUsername, String details) {
        AuditLog auditLog = new AuditLog(entityName, entityId, action, performedByUsername, details);
        auditLogRepository.save(auditLog);
    }
}
