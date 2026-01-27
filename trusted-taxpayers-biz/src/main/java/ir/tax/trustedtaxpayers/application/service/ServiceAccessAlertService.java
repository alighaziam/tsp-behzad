package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.service.ServiceQuotaInfo;
import ir.tax.trustedtaxpayers.application.service.ServiceAccessWebhookService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ServiceEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerSubscriptionEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ServiceRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for monitoring and alerting on service access usage
 * سرویس برای مانیتورینگ و هشدار استفاده از خدمات
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceAccessAlertService {

    private final ServiceAccessService serviceAccessService;
    private final TaxpayerSubscriptionRepository subscriptionRepository;
    private final ServiceRepository serviceRepository;
    private final ServiceAccessAuditService auditService;
    private final ServiceAccessWebhookService webhookService;

    /**
     * Check quota usage and send alerts if needed
     * بررسی استفاده از سهمیه و ارسال هشدار در صورت نیاز
     */
    @Scheduled(cron = "0 0 9 * * ?") // هر روز ساعت 9 صبح
    @Transactional(readOnly = true)
    public void checkQuotaUsageAndAlert() {
        log.info("Starting quota usage check and alert job");

        try {
            LocalDate today = LocalDate.now();
            List<TaxpayerSubscriptionEntity> activeSubscriptions = 
                    subscriptionRepository.findActiveSubscriptions(today);

            int alertCount = 0;
            for (TaxpayerSubscriptionEntity subscription : activeSubscriptions) {
                if (subscription.getTaxpayer() == null) {
                    log.warn("Subscription {} has no taxpayer", subscription.getId());
                    continue;
                }
                
                UUID taxpayerId = subscription.getTaxpayer().getId();
                List<ServiceEntity> services = serviceAccessService.getServicesForTaxpayer(taxpayerId);

                for (ServiceEntity service : services) {
                    ServiceQuotaInfo quota = serviceAccessService.getServiceQuota(
                            taxpayerId, service.getCode());

                    if (quota.status() != null && 
                        (quota.status().equals("WARNING") || quota.status().equals("EXCEEDED"))) {
                        // Send alert
                        sendQuotaAlert(taxpayerId, service.getCode(), quota);
                        alertCount++;
                    }
                }
            }

            log.info("Quota usage check completed. Alerts sent: {}", alertCount);
        } catch (Exception e) {
            log.error("Error in quota usage check and alert job", e);
        }
    }

    /**
     * Check for taxpayers approaching quota limits
     * بررسی مودیانی که به محدودیت نزدیک می‌شوند
     */
    @Scheduled(cron = "0 0 */6 * * ?") // هر 6 ساعت
    @Transactional(readOnly = true)
    public void checkApproachingLimits() {
        log.info("Starting approaching limits check job");

        try {
            LocalDate today = LocalDate.now();
            List<TaxpayerSubscriptionEntity> activeSubscriptions = 
                    subscriptionRepository.findActiveSubscriptions(today);

            int warningCount = 0;
            for (TaxpayerSubscriptionEntity subscription : activeSubscriptions) {
                if (subscription.getTaxpayer() == null) {
                    log.warn("Subscription {} has no taxpayer", subscription.getId());
                    continue;
                }
                
                UUID taxpayerId = subscription.getTaxpayer().getId();
                List<ServiceEntity> services = serviceAccessService.getServicesForTaxpayer(taxpayerId);

                for (ServiceEntity service : services) {
                    ServiceQuotaInfo quota = serviceAccessService.getServiceQuota(
                            taxpayerId, service.getCode());

                    // Alert if usage is above 80%
                    if (quota.usagePercentage() != null && 
                        quota.usagePercentage() >= 80.0 && quota.usagePercentage() < 100.0) {
                        sendApproachingLimitAlert(taxpayerId, service.getCode(), quota);
                        warningCount++;
                    }
                }
            }

            log.info("Approaching limits check completed. Warnings sent: {}", warningCount);
        } catch (Exception e) {
            log.error("Error in approaching limits check job", e);
        }
    }

    private void sendQuotaAlert(UUID taxpayerId, String serviceCode, ServiceQuotaInfo quota) {
        log.warn("Quota alert: taxpayerId={}, serviceCode={}, status={}, used={}, limit={}, percentage={}%", 
                taxpayerId, serviceCode, quota.status(), quota.used(), 
                quota.limitValue(), quota.usagePercentage());

        // Build metadata map
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        if (quota.used() != null) {
            metadata.put("used", quota.used());
        }
        if (quota.limitValue() != null) {
            metadata.put("limit", quota.limitValue());
        }
        if (quota.usagePercentage() != null) {
            metadata.put("percentage", quota.usagePercentage());
        }
        if (quota.remaining() != null) {
            metadata.put("remaining", quota.remaining());
        }

        // Audit the alert
        auditService.auditServiceAccess(
                taxpayerId,
                null, // System-generated alert
                serviceCode,
                "QUOTA_ALERT",
                false,
                quota.status(),
                metadata
        );

        // TODO: Send notification to taxpayer/admin
        // This could be implemented using:
        // - Email notification
        // - SMS notification
        // - In-app notification
        // - Webhook notification
    }

    private void sendApproachingLimitAlert(UUID taxpayerId, String serviceCode, ServiceQuotaInfo quota) {
        log.info("Approaching limit alert: taxpayerId={}, serviceCode={}, percentage={}%", 
                taxpayerId, serviceCode, quota.usagePercentage());

        // Build metadata map
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        if (quota.used() != null) {
            metadata.put("used", quota.used());
        }
        if (quota.limitValue() != null) {
            metadata.put("limit", quota.limitValue());
        }
        if (quota.usagePercentage() != null) {
            metadata.put("percentage", quota.usagePercentage());
        }
        if (quota.remaining() != null) {
            metadata.put("remaining", quota.remaining());
        }

        // Audit the warning
        auditService.auditServiceAccess(
                taxpayerId,
                null, // System-generated alert
                serviceCode,
                "APPROACHING_LIMIT_WARNING",
                true,
                "Usage is above 80%",
                metadata
        );

        // Send webhook if configured
        // Note: In production, this would load webhook URLs from database
        // For now, we'll log the warning
        log.info("Approaching limit alert sent for taxpayer {} and service {}", taxpayerId, serviceCode);
        
        // TODO: Send notification to taxpayer
        // This could be implemented using:
        // - Email notification
        // - SMS notification
        // - In-app notification
        // - Webhook notification (via ServiceAccessWebhookService)
    }
}

