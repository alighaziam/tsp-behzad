package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.ServiceAccessBatchService;
import ir.tax.trustedtaxpayers.application.service.ServiceAccessService;
import ir.tax.trustedtaxpayers.application.service.ServiceQuotaInfo;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ServiceEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ServiceUsageEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.SubscriptionPlanServiceEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ServiceUsageRepository;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/services", "/api/v1/services"})
@Tag(name = "Services", description = "مدیریت خدمات و دسترسی‌ها")
@RequiredArgsConstructor
public class ServiceController {

    private final ServiceAccessService serviceAccessService;
    private final ServiceAccessBatchService batchService;
    private final ServiceUsageRepository serviceUsageRepository;

    @GetMapping
    @Operation(summary = "لیست تمام خدمات")
    public ResponseEntity<List<ServiceDto>> getAllServices() {
        List<ServiceEntity> services = serviceAccessService.getAllServices();
        return ResponseEntity.ok(services.stream().map(this::toServiceDto).collect(Collectors.toList()));
    }

    @GetMapping("/active")
    @Operation(summary = "لیست خدمات فعال")
    public ResponseEntity<List<ServiceDto>> getActiveServices() {
        List<ServiceEntity> services = serviceAccessService.getActiveServices();
        return ResponseEntity.ok(services.stream().map(this::toServiceDto).collect(Collectors.toList()));
    }

    @GetMapping("/category/{category}")
    @Operation(summary = "لیست خدمات بر اساس دسته‌بندی")
    public ResponseEntity<List<ServiceDto>> getServicesByCategory(@PathVariable String category) {
        List<ServiceEntity> services = serviceAccessService.getServicesByCategory(category);
        return ResponseEntity.ok(services.stream().map(this::toServiceDto).collect(Collectors.toList()));
    }

    @GetMapping("/{serviceId}")
    @Operation(summary = "دریافت خدمات بر اساس شناسه")
    public ResponseEntity<ServiceDto> getService(@PathVariable UUID serviceId) {
        return serviceAccessService.getServiceById(serviceId)
                .map(this::toServiceDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "دریافت خدمات بر اساس کد")
    public ResponseEntity<ServiceDto> getServiceByCode(@PathVariable String code) {
        return serviceAccessService.getServiceByCode(code)
                .map(this::toServiceDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/plan/{planId}")
    @Operation(summary = "لیست خدمات یک طرح اشتراک")
    public ResponseEntity<List<SubscriptionPlanServiceDto>> getPlanServices(@PathVariable UUID planId) {
        List<SubscriptionPlanServiceEntity> planServices = serviceAccessService.getPlanServices(planId);
        return ResponseEntity.ok(planServices.stream().map(this::toPlanServiceDto).collect(Collectors.toList()));
    }

    @GetMapping("/plan/{planId}/enabled")
    @Operation(summary = "لیست خدمات فعال یک طرح اشتراک")
    public ResponseEntity<List<SubscriptionPlanServiceDto>> getEnabledPlanServices(@PathVariable UUID planId) {
        List<SubscriptionPlanServiceEntity> planServices = serviceAccessService.getEnabledPlanServices(planId);
        return ResponseEntity.ok(planServices.stream().map(this::toPlanServiceDto).collect(Collectors.toList()));
    }

    @GetMapping("/taxpayer/{taxpayerId}")
    @Operation(summary = "لیست خدمات قابل دسترس برای یک مودی")
    public ResponseEntity<List<ServiceDto>> getTaxpayerServices(@PathVariable UUID taxpayerId) {
        List<ServiceEntity> services = serviceAccessService.getServicesForTaxpayer(taxpayerId);
        return ResponseEntity.ok(services.stream().map(this::toServiceDto).collect(Collectors.toList()));
    }

    @GetMapping("/taxpayer/{taxpayerId}/has-access/{serviceCode}")
    @Operation(summary = "بررسی دسترسی مودی به یک خدمات")
    public ResponseEntity<Boolean> hasServiceAccess(
            @PathVariable UUID taxpayerId,
            @PathVariable String serviceCode) {
        boolean hasAccess = serviceAccessService.hasServiceAccess(taxpayerId, serviceCode);
        return ResponseEntity.ok(hasAccess);
    }

    @GetMapping("/taxpayer/{taxpayerId}/has-access-id/{serviceId}")
    @Operation(summary = "بررسی دسترسی مودی به یک خدمات (بر اساس شناسه)")
    public ResponseEntity<Boolean> hasServiceAccessById(
            @PathVariable UUID taxpayerId,
            @PathVariable UUID serviceId) {
        boolean hasAccess = serviceAccessService.hasServiceAccess(taxpayerId, serviceId);
        return ResponseEntity.ok(hasAccess);
    }

    @GetMapping("/taxpayer/{taxpayerId}/quota/{serviceCode}")
    @Operation(summary = "دریافت سهمیه و محدودیت خدمات برای مودی")
    public ResponseEntity<ServiceQuotaDto> getServiceQuota(
            @PathVariable UUID taxpayerId,
            @PathVariable String serviceCode) {
        ServiceQuotaInfo quota = serviceAccessService.getServiceQuota(taxpayerId, serviceCode);
        return ResponseEntity.ok(toQuotaDto(quota));
    }

    @GetMapping("/taxpayer/{taxpayerId}/statistics")
    @Operation(summary = "آمار استفاده از خدمات برای مودی")
    public ResponseEntity<List<ServiceUsageStatisticsDto>> getServiceUsageStatistics(
            @PathVariable UUID taxpayerId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        
        final LocalDate finalStartDate = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        final LocalDate finalEndDate = endDate != null ? endDate : LocalDate.now();

        List<ServiceEntity> services = serviceAccessService.getServicesForTaxpayer(taxpayerId);
        List<ServiceUsageStatisticsDto> statistics = services.stream()
                .map(service -> {
                    ServiceQuotaInfo quota = serviceAccessService.getServiceQuota(taxpayerId, service.getCode());
                    return new ServiceUsageStatisticsDto(
                        taxpayerId,
                        service.getId(),
                        service.getCode(),
                        service.getName(),
                        finalStartDate,
                        finalEndDate,
                        quota.used(),
                        quota.limitValue(),
                        quota.limitUnit(),
                        quota.usagePercentage(),
                        quota.remaining(),
                        quota.status()
                    );
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(statistics);
    }

    @GetMapping("/taxpayer/{taxpayerId}/usage")
    @Operation(summary = "تاریخچه استفاده از خدمات برای مودی")
    public ResponseEntity<List<ServiceUsageDto>> getServiceUsageHistory(
            @PathVariable UUID taxpayerId,
            @RequestParam(required = false) String serviceCode,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        
        List<ServiceUsageEntity> usage;
        if (serviceCode != null) {
            usage = serviceUsageRepository.findByServiceCode(serviceCode).stream()
                    .filter(u -> u.getTaxpayer().getId().equals(taxpayerId))
                    .collect(Collectors.toList());
        } else {
            usage = serviceUsageRepository.findByTaxpayerId(taxpayerId);
        }

        if (fromDate != null) {
            usage = usage.stream()
                    .filter(u -> !u.getUsageDate().isBefore(fromDate))
                    .collect(Collectors.toList());
        }
        if (toDate != null) {
            usage = usage.stream()
                    .filter(u -> !u.getUsageDate().isAfter(toDate))
                    .collect(Collectors.toList());
        }

        List<ServiceUsageDto> dtos = usage.stream()
                .map(this::toUsageDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/batch/check-access")
    @Operation(summary = "بررسی دسترسی دسته‌ای به خدمات")
    public ResponseEntity<BatchServiceAccessResponse> batchCheckServiceAccess(
            @RequestBody BatchServiceAccessRequest request) {
        Map<UUID, Boolean> accessMap = batchService.batchCheckServiceAccess(
                request.taxpayerIds(), request.serviceCode());
        
        int totalCount = accessMap.size();
        long grantedCount = accessMap.values().stream().filter(Boolean::booleanValue).count();
        long deniedCount = totalCount - grantedCount;
        
        BatchServiceAccessResponse response = new BatchServiceAccessResponse(
                accessMap,
                totalCount,
                (int) grantedCount,
                (int) deniedCount
        );
        
        return ResponseEntity.ok(response);
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/batch/quota")
    @Operation(summary = "دریافت سهمیه دسته‌ای خدمات")
    public ResponseEntity<BatchServiceQuotaResponse> batchGetServiceQuota(
            @RequestBody BatchServiceAccessRequest request) {
        Map<UUID, ServiceQuotaInfo> quotaMap = batchService.batchGetServiceQuota(
                request.taxpayerIds(), request.serviceCode());
        
        List<ServiceQuotaDto> quotas = quotaMap.values().stream()
                .map(this::toQuotaDto)
                .collect(Collectors.toList());
        
        BatchServiceQuotaResponse response = new BatchServiceQuotaResponse(
                quotas,
                quotas.size()
        );
        
        return ResponseEntity.ok(response);
    }

    private ServiceDto toServiceDto(ServiceEntity entity) {
        return new ServiceDto(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getCategory(),
                entity.getIsActive()
        );
    }

    private SubscriptionPlanServiceDto toPlanServiceDto(SubscriptionPlanServiceEntity entity) {
        return new SubscriptionPlanServiceDto(
                entity.getId(),
                entity.getPlan().getId(),
                entity.getPlan() != null ? entity.getPlan().getCode() : null,
                entity.getPlan() != null ? entity.getPlan().getName() : null,
                entity.getService().getId(),
                entity.getService() != null ? entity.getService().getCode() : null,
                entity.getService() != null ? entity.getService().getName() : null,
                entity.getService() != null ? entity.getService().getCategory() : null,
                entity.getIsActive(),
                entity.getLimitValue(),
                entity.getLimitUnit(),
                entity.getNotes()
        );
    }

    private ServiceQuotaDto toQuotaDto(ServiceQuotaInfo quota) {
        return new ServiceQuotaDto(
                quota.taxpayerId(),
                quota.serviceId(),
                quota.serviceCode(),
                quota.serviceName(),
                quota.limitValue(),
                quota.limitUnit(),
                quota.used(),
                quota.remaining(),
                quota.usagePercentage(),
                quota.status(),
                quota.isUnlimited()
        );
    }

    private ServiceUsageDto toUsageDto(ServiceUsageEntity entity) {
        return new ServiceUsageDto(
                entity.getId(),
                entity.getTaxpayer().getId(),
                entity.getSubscription() != null ? entity.getSubscription().getId() : null,
                entity.getService().getId(),
                entity.getServiceCode(),
                entity.getService().getName(),
                entity.getUsageDate(),
                entity.getUsageCount(),
                entity.getUsageType(),
                entity.getReferenceId(),
                entity.getReferenceType(),
                entity.getNotes(),
                entity.getCreatedOn(),
                entity.getUpdatedOn()
        );
    }
}

