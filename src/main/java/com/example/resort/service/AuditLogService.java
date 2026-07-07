package com.example.resort.service;

import com.example.resort.entity.AuditLog;
import com.example.resort.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    // @Async để không làm chậm request
    @Async("auditLogExecutor")
    public void log(String action,
                    String entityName,
                    String entityId,
                    String detail,
                    AuditLog.AuditStatus auditStatus)
    {
        try
        {
            String username = getCurrenUsername();
            String ipAddress = getClientIp();

            AuditLog auditLog = AuditLog.builder()
                    .username(username)
                    .action(action)
                    .entityName(entityName)
                    .entityId(entityId)
                    .detail(detail)
                    .ipAddress(ipAddress)
                    .createdAt(LocalDateTime.now())
                    .auditStatus(auditStatus)
                    .build();

            auditLogRepository.save(auditLog);
            log.debug("Audit - {} - {} - {} - {} - {}", username, action, entityName, entityId, auditStatus);
        }
        catch (Exception e)
        {
            log.error("Audit log failed - {}", e.getMessage());
        }
    }

    // Lấy username từ SecurityContext
    private String getCurrenUsername()
    {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated())
        {
            return auth.getName();
        }
        return "anonymous";
    }

    // Lấy IP của client
    private String getClientIp()
    {
        try
        {
            ServletRequestAttributes atts = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (atts != null)
            {
                HttpServletRequest request = atts.getRequest();
                String ip = request.getHeader("X-Forwarded-For");

                if (ip == null || ip.isEmpty())
                {
                    ip = request.getRemoteAddr();
                }
                return ip.split(",")[0].trim();
            }
        }
        catch (Exception e)
        {
            log.warn("Cannot get client ip : {}", e.getMessage());
        }
        return "unknown";
    }
}
