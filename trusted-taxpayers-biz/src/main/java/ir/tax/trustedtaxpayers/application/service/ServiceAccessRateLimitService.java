package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ServiceEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.SubscriptionPlanServiceEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerSubscriptionEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ServiceRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.SubscriptionPlanServiceRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for calculating rate limits based on subscription plans
 * سرویس برای محاسبه محدودیت نرخ بر اساس پلن‌های اشتراک
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceAccessRateLimitService {

    private final ServiceAccessService serviceAccessService;
    private final TaxpayerSubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanServiceRepository planServiceRepository;
    private final ServiceRepository serviceRepository;

    /**
     * Get rate limit configuration for a taxpayer and service
     * دریافت پیکربندی محدودیت نرخ برای یک مودی و خدمات
     */
    @Transactional(readOnly = true)
    public RateLimitConfig getRateLimitConfig(UUID taxpayerId, String serviceCode) {
        Optional<TaxpayerSubscriptionEntity> activeSubscription = 
                subscriptionRepository.findActiveByTaxpayerId(taxpayerId, LocalDate.now());
        
        if (activeSubscription.isEmpty()) {
            // No subscription - return default limits (very restrictive)
            return RateLimitConfig.builder()
                    .perSecond(1)
                    .perMinute(10)
                    .perHour(100)
                    .build();
        }

        UUID planId = activeSubscription.get().getPlanId();
        Optional<ServiceEntity> service = serviceRepository.findByCode(serviceCode);
        
        if (service.isEmpty()) {
            // Service not found - return default limits
            return RateLimitConfig.builder()
                    .perSecond(1)
                    .perMinute(10)
                    .perHour(100)
                    .build();
        }

        Optional<SubscriptionPlanServiceEntity> planService = 
                planServiceRepository.findByPlanId(planId).stream()
                        .filter(ps -> ps.getService().getId().equals(service.get().getId()))
                        .findFirst();
        
        if (planService.isEmpty() || !planService.get().getIsActive()) {
            // Service not enabled for this plan - return default limits
            return RateLimitConfig.builder()
                    .perSecond(1)
                    .perMinute(10)
                    .perHour(100)
                    .build();
        }

        SubscriptionPlanServiceEntity ps = planService.get();
        java.math.BigDecimal limitValue = ps.getLimitValue();
        String limitUnit = ps.getLimitUnit();

        // Convert service limit to rate limit based on limit unit
        if (limitValue == null || "UNLIMITED".equalsIgnoreCase(limitUnit)) {
            // Unlimited - return high limits
            return RateLimitConfig.builder()
                    .perSecond(1000)
                    .perMinute(10000)
                    .perHour(100000)
                    .build();
        }

        long limit = limitValue.longValue();
        
        // Calculate rate limits based on limit unit
        // For example, if monthly limit is 1000, that's about 33 per day, 1.4 per hour
        return switch (limitUnit.toUpperCase()) {
            case "DAILY" -> RateLimitConfig.builder()
                    .perSecond((int) Math.max(1, limit / 86400)) // limit per day / seconds per day
                    .perMinute((int) Math.max(1, limit / 1440)) // limit per day / minutes per day
                    .perHour((int) Math.max(1, limit / 24)) // limit per day / hours per day
                    .build();
            case "MONTHLY" -> RateLimitConfig.builder()
                    .perSecond((int) Math.max(1, limit / 2592000)) // limit per month / seconds per month
                    .perMinute((int) Math.max(1, limit / 43200)) // limit per month / minutes per month
                    .perHour((int) Math.max(1, limit / 720)) // limit per month / hours per month
                    .build();
            case "YEARLY" -> RateLimitConfig.builder()
                    .perSecond((int) Math.max(1, limit / 31536000)) // limit per year / seconds per year
                    .perMinute((int) Math.max(1, limit / 525600)) // limit per year / minutes per year
                    .perHour((int) Math.max(1, limit / 8760)) // limit per year / hours per year
                    .build();
            default -> RateLimitConfig.builder()
                    .perSecond((int) Math.max(1, limit / 2592000)) // Default to monthly
                    .perMinute((int) Math.max(1, limit / 43200))
                    .perHour((int) Math.max(1, limit / 720))
                    .build();
        };
    }

    /**
     * Rate limit configuration
     */
    @lombok.Builder
    @lombok.Data
    public static class RateLimitConfig {
        private int perSecond;
        private int perMinute;
        private int perHour;
    }
}

