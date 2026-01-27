package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ServiceRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ServiceUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Health check for service access system
 * بررسی سلامت سیستم مدیریت دسترسی به سرویس‌ها
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceAccessHealthCheck implements HealthIndicator {

    private final ServiceRepository serviceRepository;
    private final ServiceUsageRepository serviceUsageRepository;

    @Override
    public Health health() {
        try {
            // بررسی تعداد خدمات فعال
            long activeServicesCount = serviceRepository.findAll().stream()
                    .filter(service -> service.getIsActive())
                    .count();
            
            // بررسی تعداد کل خدمات
            long totalServicesCount = serviceRepository.count();
            
            // بررسی تعداد استفاده از خدمات (برای monitoring)
            // Handle case where SERVICE_USAGE table might not exist yet
            long totalUsageCount = 0L;
            boolean usageTableAvailable = true;
            try {
                totalUsageCount = serviceUsageRepository.count();
            } catch (InvalidDataAccessResourceUsageException e) {
                // Table doesn't exist - migration V110 may not have run yet
                if (e.getMessage() != null && e.getMessage().contains("ORA-00942")) {
                    log.debug("SERVICE_USAGE table not found - migration may not have run yet");
                    usageTableAvailable = false;
                } else {
                    // Re-throw if it's a different database error
                    throw e;
                }
            }
            
            // بررسی وضعیت سیستم
            boolean isHealthy = activeServicesCount > 0 && totalServicesCount > 0;
            
            Map<String, Object> details = new HashMap<>();
            details.put("activeServices", activeServicesCount);
            details.put("totalServices", totalServicesCount);
            details.put("totalUsageRecords", totalUsageCount);
            details.put("usageTableAvailable", usageTableAvailable);
            details.put("status", isHealthy ? "UP" : "DOWN");
            
            if (!usageTableAvailable) {
                details.put("warning", "SERVICE_USAGE table not available - migration V110 may be pending");
            }
            
            if (isHealthy) {
                return Health.up()
                        .withDetails(details)
                        .build();
            } else {
                return Health.down()
                        .withDetails(details)
                        .withDetail("error", "No active services found")
                        .build();
            }
        } catch (Exception e) {
            log.error("Error checking service access health", e);
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}

