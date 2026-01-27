package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.AuditLogEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for auditing service access
 * سرویس برای ثبت لاگ دسترسی‌های سرویس
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceAccessAuditService {

    private final AuditLogService auditLogService;

    @Transactional
    public void auditServiceAccess(UUID taxpayerId, UUID userId, String serviceCode, 
                                   String action, boolean granted, String reason, 
                                   Map<String, Object> metadata) {
        try {
            Map<String, Object> auditMetadata = new HashMap<>();
            if (metadata != null) {
                auditMetadata.putAll(metadata);
            }
            
            if (taxpayerId != null) {
                auditMetadata.put("taxpayerId", taxpayerId.toString());
            }
            auditMetadata.put("serviceCode", serviceCode);
            auditMetadata.put("action", action);
            auditMetadata.put("granted", granted);
            if (reason != null) {
                auditMetadata.put("reason", reason);
            }

            String details = buildDetails(auditMetadata);

            AuditLogEntity auditLog = AuditLogEntity.builder()
                    .actorUserId(userId)
                    .entityName("SERVICE_ACCESS")
                    .entityId(taxpayerId != null ? taxpayerId.toString() : "UNKNOWN")
                    .action(action + "_" + (granted ? "GRANTED" : "DENIED"))
                    .details(details)
                    .build();

            auditLogService.create(auditLog);
            
            log.debug("Service access audited: taxpayerId={}, serviceCode={}, action={}, granted={}", 
                    taxpayerId, serviceCode, action, granted);
        } catch (Exception e) {
            log.error("Failed to audit service access: taxpayerId={}, serviceCode={}", 
                    taxpayerId, serviceCode, e);
            // Fail-safe: don't break the flow if audit fails
        }
    }

    @Transactional
    public void auditServiceUsage(UUID taxpayerId, UUID userId, String serviceCode, 
                                  UUID referenceId, String referenceType, int usageCount) {
        try {
            Map<String, Object> auditMetadata = new HashMap<>();
            auditMetadata.put("taxpayerId", taxpayerId.toString());
            auditMetadata.put("serviceCode", serviceCode);
            auditMetadata.put("referenceId", referenceId != null ? referenceId.toString() : null);
            auditMetadata.put("referenceType", referenceType);
            auditMetadata.put("usageCount", usageCount);

            String details = buildDetails(auditMetadata);

            AuditLogEntity auditLog = AuditLogEntity.builder()
                    .actorUserId(userId)
                    .entityName("SERVICE_USAGE")
                    .entityId(taxpayerId.toString())
                    .action("SERVICE_USED")
                    .details(details)
                    .build();

            auditLogService.create(auditLog);
            
            log.debug("Service usage audited: taxpayerId={}, serviceCode={}, usageCount={}", 
                    taxpayerId, serviceCode, usageCount);
        } catch (Exception e) {
            log.error("Failed to audit service usage: taxpayerId={}, serviceCode={}", 
                    taxpayerId, serviceCode, e);
            // Fail-safe: don't break the flow if audit fails
        }
    }

    @Transactional
    public void auditLimitExceeded(UUID taxpayerId, UUID userId, String serviceCode, 
                                   long used, long limit, String limitUnit) {
        try {
            Map<String, Object> auditMetadata = new HashMap<>();
            auditMetadata.put("taxpayerId", taxpayerId.toString());
            auditMetadata.put("serviceCode", serviceCode);
            auditMetadata.put("used", used);
            auditMetadata.put("limit", limit);
            auditMetadata.put("limitUnit", limitUnit);
            auditMetadata.put("percentage", limit > 0 ? (used * 100.0 / limit) : 0.0);

            String details = buildDetails(auditMetadata);

            AuditLogEntity auditLog = AuditLogEntity.builder()
                    .actorUserId(userId)
                    .entityName("SERVICE_LIMIT")
                    .entityId(taxpayerId.toString())
                    .action("LIMIT_EXCEEDED")
                    .details(details)
                    .build();

            auditLogService.create(auditLog);
            
            log.warn("Service limit exceeded audited: taxpayerId={}, serviceCode={}, used={}, limit={}", 
                    taxpayerId, serviceCode, used, limit);
        } catch (Exception e) {
            log.error("Failed to audit limit exceeded: taxpayerId={}, serviceCode={}", 
                    taxpayerId, serviceCode, e);
            // Fail-safe: don't break the flow if audit fails
        }
    }

    private String buildDetails(Map<String, Object> metadata) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = 
                    new com.fasterxml.jackson.databind.ObjectMapper();
            return objectMapper.writeValueAsString(metadata);
        } catch (Exception e) {
            return metadata.toString();
        }
    }
}

