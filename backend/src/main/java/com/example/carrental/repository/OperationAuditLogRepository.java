package com.example.carrental.repository;

import com.example.carrental.domain.OperationAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationAuditLogRepository extends JpaRepository<OperationAuditLog, Long> {
}
