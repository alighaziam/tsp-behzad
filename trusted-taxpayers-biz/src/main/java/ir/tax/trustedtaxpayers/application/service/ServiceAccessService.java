package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.core.exception.ResourceNotFoundException;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.*;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceAccessService {

    private final ServiceRepository serviceRepository;
    private final SubscriptionPlanServiceRepository planServiceRepository;
    private final SubscriptionPlanRepository planRepository;
    private final TaxpayerSubscriptionRepository subscriptionRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final ServiceUsageRepository serviceUsageRepository;
    @Lazy
    private final CacheService cacheService;

    @Transactional(readOnly = true)
    public List<ServiceEntity> getAllServices() {
        return serviceRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ServiceEntity> getActiveServices() {
        return serviceRepository.findAll().stream()
                .filter(ServiceEntity::getIsActive)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ServiceEntity> getServiceById(UUID serviceId) {
        return serviceRepository.findById(serviceId);
    }

    @Transactional(readOnly = true)
    public Optional<ServiceEntity> getServiceByCode(String code) {
        return serviceRepository.findByCode(code);
    }

    @Transactional(readOnly = true)
    public List<ServiceEntity> getServicesByCategory(String category) {
        return serviceRepository.findAll().stream()
                .filter(s -> s.getCategory().equals(category))
                .filter(ServiceEntity::getIsActive)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanServiceEntity> getPlanServices(UUID planId) {
        return planServiceRepository.findByPlanId(planId);
    }

    @Transactional(readOnly = true)
    public List<SubscriptionPlanServiceEntity> getEnabledPlanServices(UUID planId) {
        return planServiceRepository.findByPlanId(planId);
    }

    @Transactional(readOnly = true)
    public List<ServiceEntity> getServicesForPlan(UUID planId) {
        List<SubscriptionPlanServiceEntity> planServices = getEnabledPlanServices(planId);
        return planServices.stream()
                .map(SubscriptionPlanServiceEntity::getService)
                .filter(ServiceEntity::getIsActive)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ServiceEntity> getServicesForTaxpayer(UUID taxpayerId) {
        // استفاده از cache برای بهبود performance
        String cacheKey = "taxpayer_services:" + taxpayerId;
        return cacheService.get("taxpayerServices", cacheKey, () -> {
            Optional<TaxpayerSubscriptionEntity> activeSubscription = 
                    subscriptionRepository.findActiveByTaxpayerId(taxpayerId, LocalDate.now());
            
            if (activeSubscription.isEmpty()) {
                return List.<ServiceEntity>of();
            }

            UUID planId = activeSubscription.get().getPlanId();
            return getServicesForPlan(planId);
        });
    }

    @Transactional(readOnly = true)
    public boolean hasServiceAccess(UUID taxpayerId, String serviceCode) {
        // استفاده از cache برای بهبود performance
        String cacheKey = "service_access:" + taxpayerId + ":" + serviceCode;
        return cacheService.get("serviceAccess", cacheKey, () -> {
            List<ServiceEntity> services = getServicesForTaxpayer(taxpayerId);
            boolean hasAccess = services.stream()
                    .anyMatch(s -> s.getCode().equals(serviceCode) && s.getIsActive());
            return hasAccess;
        });
    }

    @Transactional(readOnly = true)
    public boolean hasServiceAccess(UUID taxpayerId, UUID serviceId) {
        List<ServiceEntity> services = getServicesForTaxpayer(taxpayerId);
        return services.stream()
                .anyMatch(s -> s.getId().equals(serviceId) && s.getIsActive());
    }

    @Transactional(readOnly = true)
    public Optional<SubscriptionPlanServiceEntity> getPlanService(UUID planId, UUID serviceId) {
        List<SubscriptionPlanServiceEntity> planServices = planServiceRepository.findByPlanId(planId);
        return planServices.stream()
                .filter(ps -> ps.getService().getId().equals(serviceId))
                .findFirst();
    }

    @Transactional(readOnly = true)
    public boolean isServiceEnabledForPlan(UUID planId, UUID serviceId) {
        Optional<SubscriptionPlanServiceEntity> planService = getPlanService(planId, serviceId);
        return planService.isPresent() 
                && planService.get().getService().getIsActive();
    }

    @Transactional(readOnly = true)
    public boolean checkServiceLimit(UUID taxpayerId, String serviceCode) {
        Optional<TaxpayerSubscriptionEntity> activeSubscription = 
                subscriptionRepository.findActiveByTaxpayerId(taxpayerId, LocalDate.now());
        
        if (activeSubscription.isEmpty()) {
            return false;
        }

        UUID planId = activeSubscription.get().getPlanId();
        Optional<ServiceEntity> service = serviceRepository.findByCode(serviceCode);
        
        if (service.isEmpty()) {
            return false;
        }

        Optional<SubscriptionPlanServiceEntity> planService = getPlanService(planId, service.get().getId());
        
        if (planService.isEmpty() || !planService.get().getIsActive()) {
            return false;
        }

        // اگر محدودیت نامحدود است
        if (planService.get().getLimitValue() == null || 
            "UNLIMITED".equalsIgnoreCase(planService.get().getLimitUnit())) {
            return true;
        }

        // بررسی محدودیت بر اساس نوع واحد
        String limitUnit = planService.get().getLimitUnit();
        java.math.BigDecimal limitValue = planService.get().getLimitValue();
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate;

        if ("MONTHLY".equalsIgnoreCase(limitUnit)) {
            startDate = today.withDayOfMonth(1);
            endDate = today.withDayOfMonth(today.lengthOfMonth());
        } else if ("YEARLY".equalsIgnoreCase(limitUnit)) {
            startDate = today.withDayOfYear(1);
            endDate = today.withDayOfYear(today.lengthOfYear());
        } else if ("DAILY".equalsIgnoreCase(limitUnit)) {
            startDate = today;
            endDate = today;
        } else {
            // Default: monthly
            startDate = today.withDayOfMonth(1);
            endDate = today.withDayOfMonth(today.lengthOfMonth());
        }

        // دریافت تعداد استفاده از سرویس
        Long totalUsage = serviceUsageRepository.getTotalUsageCount(
            taxpayerId, 
            service.get().getId(), 
            startDate, 
            endDate
        );

        if (totalUsage == null) {
            totalUsage = 0L;
        }

        // بررسی اینکه آیا محدودیت رعایت شده است
        return totalUsage < limitValue.longValue();
    }

    @Transactional
    public ServiceUsageEntity recordServiceUsage(
            UUID taxpayerId,
            UUID subscriptionId,
            String serviceCode,
            String usageType,
            UUID referenceId,
            String referenceType,
            String notes) {
        
        Optional<TaxpayerSubscriptionEntity> subscription = 
                subscriptionRepository.findById(subscriptionId);
        if (subscription.isEmpty() || !subscription.get().getTaxpayerId().equals(taxpayerId)) {
            throw new ResourceNotFoundException("اشتراک یافت نشد یا متعلق به مودی نیست.");
        }

        Optional<ServiceEntity> service = serviceRepository.findByCode(serviceCode);
        if (service.isEmpty()) {
            throw new ResourceNotFoundException("خدمات یافت نشد.");
        }

        LocalDate today = LocalDate.now();

        // بررسی وجود رکورد استفاده قبلی
        Optional<ServiceUsageEntity> existingUsage = serviceUsageRepository.findExistingUsage(
            taxpayerId,
            service.get().getId(),
            today,
            usageType,
            referenceId
        );

        ServiceUsageEntity usage;
        if (existingUsage.isPresent()) {
            // افزایش تعداد استفاده
            usage = existingUsage.get();
            usage.setUsageCount(usage.getUsageCount() + 1);
        } else {
            // ایجاد رکورد جدید
            usage = ServiceUsageEntity.builder()
                .taxpayer(subscription.get().getTaxpayer())
                .subscription(subscription.get())
                .service(service.get())
                .serviceCode(serviceCode)
                .usageDate(today)
                .usageCount(1)
                .usageType(usageType)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .notes(notes)
                .build();
        }

        ServiceUsageEntity saved = serviceUsageRepository.save(usage);
        
        // Invalidate cache after recording usage (to update quota info)
        if (cacheService != null) {
            invalidateServiceAccessCache(taxpayerId);
        }
        
        // Record metrics (if metrics service is available)
        // Note: This would require injecting ServiceAccessMetrics, but we'll avoid circular dependency
        // Metrics will be recorded in the aspect layer
        
        // Audit service usage (if audit service is available)
        // Note: This would require injecting ServiceAccessAuditService, but we'll avoid circular dependency
        // Audit will be recorded in the aspect layer or where recordServiceUsage is called
        
        return saved;
    }

    @Transactional(readOnly = true)
    public ServiceQuotaInfo getServiceQuota(UUID taxpayerId, String serviceCode) {
        Optional<TaxpayerSubscriptionEntity> activeSubscription = 
                subscriptionRepository.findActiveByTaxpayerId(taxpayerId, LocalDate.now());
        
        if (activeSubscription.isEmpty()) {
            return ServiceQuotaInfo.noAccess();
        }

        UUID planId = activeSubscription.get().getPlanId();
        Optional<ServiceEntity> service = serviceRepository.findByCode(serviceCode);
        
        if (service.isEmpty()) {
            return ServiceQuotaInfo.noAccess();
        }

        Optional<SubscriptionPlanServiceEntity> planService = getPlanService(planId, service.get().getId());
        
        if (planService.isEmpty() || !planService.get().getIsActive()) {
            return ServiceQuotaInfo.noAccess();
        }

        java.math.BigDecimal limitValue = planService.get().getLimitValue();
        String limitUnit = planService.get().getLimitUnit();

        // اگر محدودیت نامحدود است
        if (limitValue == null || "UNLIMITED".equalsIgnoreCase(limitUnit)) {
            return ServiceQuotaInfo.unlimited();
        }

        // محاسبه استفاده
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate endDate;

        if ("MONTHLY".equalsIgnoreCase(limitUnit)) {
            startDate = today.withDayOfMonth(1);
            endDate = today.withDayOfMonth(today.lengthOfMonth());
        } else if ("YEARLY".equalsIgnoreCase(limitUnit)) {
            startDate = today.withDayOfYear(1);
            endDate = today.withDayOfYear(today.lengthOfYear());
        } else if ("DAILY".equalsIgnoreCase(limitUnit)) {
            startDate = today;
            endDate = today;
        } else {
            startDate = today.withDayOfMonth(1);
            endDate = today.withDayOfMonth(today.lengthOfMonth());
        }

        Long totalUsage = serviceUsageRepository.getTotalUsageCount(
            taxpayerId, 
            service.get().getId(), 
            startDate, 
            endDate
        );

        if (totalUsage == null) {
            totalUsage = 0L;
        }

        long limit = limitValue.longValue();
        long remaining = Math.max(0, limit - totalUsage);
        double usagePercentage = limit > 0 ? (totalUsage * 100.0 / limit) : 0.0;

        String status;
        if (totalUsage >= limit) {
            status = "EXCEEDED";
        } else if (usagePercentage >= 80) {
            status = "WARNING";
        } else {
            status = "OK";
        }

        return new ServiceQuotaInfo(
            taxpayerId,
            service.get().getId(),
            serviceCode,
            service.get().getName(),
            limitValue,
            limitUnit,
            totalUsage,
            remaining,
            usagePercentage,
            status,
            false
        );
    }

    @Transactional
    public SubscriptionPlanServiceEntity addServiceToPlan(
            UUID planId, 
            UUID serviceId, 
            Boolean isEnabled,
            java.math.BigDecimal limitValue,
            String limitUnit,
            String notes) {
        
        SubscriptionPlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("پلن اشتراک یافت نشد."));
        
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ResourceNotFoundException("خدمات یافت نشد."));

        if (planServiceRepository.existsByPlanIdAndServiceId(planId, serviceId)) {
            throw new IllegalArgumentException("این خدمات قبلاً به این طرح اضافه شده است.");
        }

        SubscriptionPlanServiceEntity planService = SubscriptionPlanServiceEntity.builder()
                .plan(plan)
                .service(service)
                .limitValue(limitValue)
                .limitUnit(limitUnit)
                .notes(notes)
                .build();

        SubscriptionPlanServiceEntity saved = planServiceRepository.save(planService);
        
        // Invalidate cache after adding service to plan
        if (cacheService != null) {
            cacheService.evictAll("taxpayerServices");
            cacheService.evictAll("serviceAccess");
        }
        
        return saved;
    }

    @Transactional
    public SubscriptionPlanServiceEntity updatePlanService(
            UUID planServiceId,
            Boolean isEnabled,
            java.math.BigDecimal limitValue,
            String limitUnit,
            String notes) {
        
        SubscriptionPlanServiceEntity planService = planServiceRepository.findById(planServiceId)
                .orElseThrow(() -> new ResourceNotFoundException("ارتباط طرح-خدمات یافت نشد."));

        if (limitValue != null) {
            planService.setLimitValue(limitValue);
        }
        if (limitUnit != null) {
            planService.setLimitUnit(limitUnit);
        }
        if (notes != null) {
            planService.setNotes(notes);
        }

        SubscriptionPlanServiceEntity saved = planServiceRepository.save(planService);
        
        // Invalidate cache after updating plan service
        if (cacheService != null) {
            cacheService.evictAll("taxpayerServices");
            cacheService.evictAll("serviceAccess");
        }
        
        return saved;
    }

    @Transactional
    public void removeServiceFromPlan(UUID planId, UUID serviceId) {
        Optional<SubscriptionPlanServiceEntity> planService = getPlanService(planId, serviceId);
        if (planService.isPresent()) {
            planServiceRepository.delete(planService.get());
            // Invalidate cache
            if (cacheService != null) {
                cacheService.evictAll("taxpayerServices");
                cacheService.evictAll("serviceAccess");
            }
        }
    }

    @Transactional
    public void invalidateServiceAccessCache(UUID taxpayerId) {
        if (cacheService != null) {
            // Invalidate specific taxpayer cache
            cacheService.evict("taxpayerServices", "taxpayer_services:" + taxpayerId);
            // Invalidate all service access cache for this taxpayer
            // Since we can't evict by pattern easily, we evict all
            cacheService.evictAll("serviceAccess");
        }
    }
}

