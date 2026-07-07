package com.example.resort.repository;

import com.example.resort.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByUsername(String username);

    List<AuditLog> findByEntityName(String entityName);

    List<AuditLog> findByEntityNameAndEntityId(String entityName, String entityId);

    // FIX: đổi findByCreatedAtBetween (field trong entity là createdAt, không phải createAt)
    List<AuditLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    List<AuditLog> findByAction(String action);
}