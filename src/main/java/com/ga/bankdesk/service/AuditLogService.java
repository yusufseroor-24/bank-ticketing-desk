package com.ga.bankdesk.service;

import com.ga.bankdesk.model.AuditLog;
import com.ga.bankdesk.model.User;
import com.ga.bankdesk.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public void log(User user, String action, String entityType, Long entityId){
        AuditLog log = new AuditLog();
        log.setUser(user);
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        auditLogRepository.save(log);
    }
}
