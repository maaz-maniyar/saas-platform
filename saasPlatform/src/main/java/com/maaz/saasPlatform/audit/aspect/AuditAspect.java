package com.maaz.saasPlatform.audit.aspect;

import com.maaz.saasPlatform.audit.annotation.Auditable;
import com.maaz.saasPlatform.audit.entity.AuditLog;
import com.maaz.saasPlatform.audit.repository.AuditLogRepository;
import com.maaz.saasPlatform.tenant.context.TenantContext;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    public AuditAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @AfterReturning("@annotation(auditable)")
    public void logAudit(JoinPoint jp, Auditable auditable) {
        AuditLog log = new AuditLog();
        log.setTenantId(TenantContext.getTenant());
        log.setAction(auditable.action());
        log.setResource(auditable.resource());

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            log.setUserEmail(auth.getName());
        }

        auditLogRepository.save(log);
    }
}

