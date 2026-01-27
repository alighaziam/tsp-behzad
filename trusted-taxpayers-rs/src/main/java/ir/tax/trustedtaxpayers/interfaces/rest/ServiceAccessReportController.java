package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.ServiceAccessService;
import ir.tax.trustedtaxpayers.application.service.ServiceQuotaInfo;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.AuditLogEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ServiceEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ServiceUsageEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.AuditLogRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ServiceUsageRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerSubscriptionRepository;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ServiceAccessReportDto;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ServiceUsageStatisticsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/service-access/reports", "/api/v1/service-access/reports"})
@Tag(name = "Service Access Reports", description = "گزارش‌گیری دسترسی و استفاده از خدمات")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN', 'TSP')")
public class ServiceAccessReportController {

    private final ServiceAccessService serviceAccessService;
    private final ServiceUsageRepository serviceUsageRepository;
    private final AuditLogRepository auditLogRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final TaxpayerSubscriptionRepository subscriptionRepository;

    @GetMapping("/taxpayer/{taxpayerId}")
    @Operation(summary = "گزارش دسترسی و استفاده از خدمات برای یک مودی")
    public ResponseEntity<ServiceAccessReportDto> getTaxpayerServiceAccessReport(
            @PathVariable UUID taxpayerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        final LocalDate finalStartDate = startDate != null ? startDate : LocalDate.now().withDayOfMonth(1);
        final LocalDate finalEndDate = endDate != null ? endDate : LocalDate.now();

        TaxpayerEntity taxpayer = taxpayerRepository.findById(taxpayerId)
                .orElseThrow(() -> new RuntimeException("مودی یافت نشد"));

        // Get service usage statistics
        List<ServiceEntity> services = serviceAccessService.getServicesForTaxpayer(taxpayerId);
        List<ServiceUsageStatisticsDto> serviceUsage = services.stream()
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

        // Get audit logs for service access
        List<AuditLogEntity> accessLogs = auditLogRepository.findByEntityNameAndEntityId(
                "SERVICE_ACCESS", taxpayerId.toString());
        
        long totalSuccessfulAccess = accessLogs.stream()
                .filter(log -> log.getAction().contains("GRANTED"))
                .count();
        
        long totalFailedAccess = accessLogs.stream()
                .filter(log -> log.getAction().contains("DENIED"))
                .count();

        // Get total service usage
        List<ServiceUsageEntity> usageEntities = serviceUsageRepository.findByTaxpayerId(taxpayerId);
        long totalServiceUsage = usageEntities.stream()
                .mapToInt(ServiceUsageEntity::getUsageCount)
                .sum();

        // Get subscription info
        final UUID[] subscriptionId = new UUID[1];
        final String[] planName = new String[1];
        subscriptionRepository.findActiveByTaxpayerId(taxpayerId, LocalDate.now())
                .ifPresent(subscription -> {
                    subscriptionId[0] = subscription.getId();
                    if (subscription.getPlan() != null) {
                        planName[0] = subscription.getPlan().getName();
                    }
                });

        ServiceAccessReportDto report = new ServiceAccessReportDto(
                taxpayerId,
                taxpayer.getName(),
                subscriptionId[0],
                planName[0],
                finalStartDate,
                finalEndDate,
                serviceUsage,
                totalSuccessfulAccess,
                totalFailedAccess,
                totalServiceUsage
        );

        return ResponseEntity.ok(report);
    }

    @GetMapping("/summary")
    @Operation(summary = "خلاصه گزارش دسترسی به خدمات")
    public ResponseEntity<ServiceAccessSummaryDto> getServiceAccessSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        if (startDate == null) {
            startDate = LocalDate.now().withDayOfMonth(1);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        // Get all service access logs
        List<AuditLogEntity> accessLogs = auditLogRepository.findByEntityName("SERVICE_ACCESS");
        
        long totalAccess = accessLogs.size();
        long totalSuccessfulAccess = accessLogs.stream()
                .filter(log -> log.getAction().contains("GRANTED"))
                .count();
        long totalFailedAccess = accessLogs.stream()
                .filter(log -> log.getAction().contains("DENIED"))
                .count();
        
        // Get total service usage
        long totalServiceUsage = serviceUsageRepository.count();

        ServiceAccessSummaryDto summary = new ServiceAccessSummaryDto(
                startDate,
                endDate,
                totalAccess,
                totalSuccessfulAccess,
                totalFailedAccess,
                totalServiceUsage
        );

        return ResponseEntity.ok(summary);
    }

    // Inner record for summary
    private record ServiceAccessSummaryDto(
            LocalDate startDate,
            LocalDate endDate,
            Long totalAccess,
            Long totalSuccessfulAccess,
            Long totalFailedAccess,
            Long totalServiceUsage
    ) {}
}

