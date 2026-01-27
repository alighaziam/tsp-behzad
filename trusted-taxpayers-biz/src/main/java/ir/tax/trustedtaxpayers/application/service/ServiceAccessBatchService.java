package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ServiceEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ServiceUsageEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerSubscriptionEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ServiceRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ServiceUsageRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for batch operations on service access
 * سرویس برای عملیات دسته‌ای روی دسترسی به خدمات
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceAccessBatchService {

    private final ServiceAccessService serviceAccessService;
    private final ServiceUsageRepository serviceUsageRepository;
    private final TaxpayerSubscriptionRepository subscriptionRepository;
    private final ServiceRepository serviceRepository;

    /**
     * Batch check service access for multiple taxpayers
     * بررسی دسترسی به خدمات برای چندین مودی
     */
    @Transactional(readOnly = true)
    public Map<UUID, Boolean> batchCheckServiceAccess(List<UUID> taxpayerIds, String serviceCode) {
        return taxpayerIds.stream()
                .collect(Collectors.toMap(
                        taxpayerId -> taxpayerId,
                        taxpayerId -> serviceAccessService.hasServiceAccess(taxpayerId, serviceCode)
                ));
    }

    /**
     * Batch get service quota for multiple taxpayers
     * دریافت سهمیه خدمات برای چندین مودی
     */
    @Transactional(readOnly = true)
    public Map<UUID, ServiceQuotaInfo> batchGetServiceQuota(List<UUID> taxpayerIds, String serviceCode) {
        return taxpayerIds.stream()
                .collect(Collectors.toMap(
                        taxpayerId -> taxpayerId,
                        taxpayerId -> serviceAccessService.getServiceQuota(taxpayerId, serviceCode)
                ));
    }

    /**
     * Batch record service usage for multiple taxpayers
     * ثبت استفاده از خدمات برای چندین مودی
     */
    @Transactional
    public Map<UUID, ServiceUsageEntity> batchRecordServiceUsage(
            List<UUID> taxpayerIds, 
            String serviceCode, 
            String usageType,
            String referenceType) {
        
        return taxpayerIds.stream()
                .collect(Collectors.toMap(
                        taxpayerId -> taxpayerId,
                        taxpayerId -> {
                            try {
                                // Get active subscription
                                TaxpayerSubscriptionEntity subscription = 
                                        subscriptionRepository.findActiveByTaxpayerId(taxpayerId, LocalDate.now())
                                                .orElse(null);
                                
                                if (subscription == null) {
                                    log.warn("No active subscription found for taxpayer: {}", taxpayerId);
                                    return null;
                                }

                                // Get service
                                ServiceEntity service = serviceRepository.findByCode(serviceCode)
                                        .orElse(null);
                                
                                if (service == null) {
                                    log.warn("Service not found: {}", serviceCode);
                                    return null;
                                }

                                // Record usage
                                return serviceAccessService.recordServiceUsage(
                                        taxpayerId,
                                        subscription.getId(),
                                        serviceCode,
                                        usageType,
                                        null, // referenceId
                                        referenceType,
                                        "Batch usage recording"
                                );
                            } catch (Exception e) {
                                log.error("Failed to record service usage for taxpayer: {}", taxpayerId, e);
                                return null;
                            }
                        }
                ));
    }

    /**
     * Batch invalidate cache for multiple taxpayers
     * پاک کردن cache برای چندین مودی
     */
    @Transactional
    public void batchInvalidateCache(List<UUID> taxpayerIds) {
        for (UUID taxpayerId : taxpayerIds) {
            try {
                serviceAccessService.invalidateServiceAccessCache(taxpayerId);
            } catch (Exception e) {
                log.error("Failed to invalidate cache for taxpayer: {}", taxpayerId, e);
            }
        }
    }

    /**
     * Get service usage summary for multiple taxpayers
     * دریافت خلاصه استفاده از خدمات برای چندین مودی
     */
    @Transactional(readOnly = true)
    public Map<UUID, Long> batchGetServiceUsageSummary(
            List<UUID> taxpayerIds, 
            String serviceCode,
            LocalDate startDate,
            LocalDate endDate) {
        
        final LocalDate finalStartDate = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        final LocalDate finalEndDate = endDate != null ? endDate : LocalDate.now();

        return taxpayerIds.stream()
                .collect(Collectors.toMap(
                        taxpayerId -> taxpayerId,
                        taxpayerId -> {
                            try {
                                ServiceEntity service = serviceRepository.findByCode(serviceCode)
                                        .orElse(null);
                                
                                if (service == null) {
                                    return 0L;
                                }

                                Long usage = serviceUsageRepository.getTotalUsageCount(
                                        taxpayerId,
                                        service.getId(),
                                        finalStartDate,
                                        finalEndDate
                                );

                                return usage != null ? usage : 0L;
                            } catch (Exception e) {
                                log.error("Failed to get service usage summary for taxpayer: {}", taxpayerId, e);
                                return 0L;
                            }
                        }
                ));
    }
}

