package com.example.resort.controller;

import com.example.resort.dto.response.ApiResponse;
import com.example.resort.entity.AuditLog;
import com.example.resort.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/audit-log")
@RequiredArgsConstructor
public class AuditLogController {
    private final AuditLogRepository auditLogRepository;

    // Lấy tất cả audit log (ADMIN only — cấu hình trong SecurityConfig)
    @GetMapping
    ApiResponse<List<AuditLog>> getAllLogs()
    {
        return ApiResponse.<List<AuditLog>> builder()
                .result(auditLogRepository.findAll())
                .build();
    }

    // Lấy theo username
    @GetMapping("/user/{username}")
    ApiResponse<List<AuditLog>> getByUsername(@PathVariable String username)
    {
        return ApiResponse.<List<AuditLog>> builder()
                .result(auditLogRepository.findByUsername(username))
                .build();
    }

    // Lấy theo entity (Room / Booking / Customer)
    @GetMapping("/entity/{entityName}")
    ApiResponse<List<AuditLog>> getByEntity(@PathVariable String entityName)
    {
        return ApiResponse.<List<AuditLog>> builder()
                .result(auditLogRepository.findByEntityName(entityName))
                .build();
    }

    //  ấy theo khoản thời gian
    @GetMapping("/time")
    ApiResponse<List<AuditLog>> getByTimeRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to )
    {
        return ApiResponse.<List<AuditLog>> builder()
                .result(auditLogRepository.findByCreatedAtBetween(from, to))
                .build();
    }

    // laays theo action
    @GetMapping("/action/{action}")
    ApiResponse<List<AuditLog>> getByAction(@PathVariable String action)
    {
        return ApiResponse.<List<AuditLog>> builder()
                .result(auditLogRepository.findByAction(action))
                .build();
    }
}
