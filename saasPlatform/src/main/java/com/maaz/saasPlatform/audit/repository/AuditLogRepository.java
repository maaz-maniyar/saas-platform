package com.maaz.saasPlatform.audit.repository;

import com.maaz.saasPlatform.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
