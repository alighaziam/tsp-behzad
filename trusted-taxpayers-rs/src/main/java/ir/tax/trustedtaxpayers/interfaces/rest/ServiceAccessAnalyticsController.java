package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.ServiceAccessAnalyticsService;
import ir.tax.trustedtaxpayers.application.service.ServiceAccessService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ServiceEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ServiceRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerSubscriptionRepository;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ServiceAccessAnalyticsDto;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ServiceAccessDashboardDto;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ServiceAccessSummaryDto;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ServiceUsageRankingDto;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.TaxpayerUsageRankingDto;
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
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"/api/service-access/analytics", "/api/v1/service-access/analytics"})
@Tag(name = "Service Access Analytics", description = "تحلیل و آمار دسترسی به خدمات")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN', 'TSP')")
public class ServiceAccessAnalyticsController {

    private final ServiceAccessAnalyticsService analyticsService;
    private final ServiceAccessService serviceAccessService;
    private final ServiceRepository serviceRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final TaxpayerSubscriptionRepository subscriptionRepository;

    @GetMapping("/trends/{serviceCode}")
    @Operation(summary = "روند استفاده از خدمات")
    public ResponseEntity<ServiceAccessAnalyticsDto> getServiceUsageTrends(
            @PathVariable String serviceCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "DAILY") String period) {
        
        Map<String, Object> trends = analyticsService.getServiceUsageTrends(
                serviceCode, startDate, endDate, period);
        
        ServiceAccessAnalyticsDto dto = new ServiceAccessAnalyticsDto(
                (String) trends.get("serviceCode"),
                (String) trends.get("serviceName"),
                LocalDate.parse((String) trends.get("startDate")),
                LocalDate.parse((String) trends.get("endDate")),
                (String) trends.get("period"),
                (Map<String, Long>) trends.get("trends"),
                ((Number) trends.get("totalUsage")).longValue()
        );
        
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/top-services")
    @Operation(summary = "پراستفاده‌ترین خدمات")
    public ResponseEntity<List<Map<String, Object>>> getTopServices(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        
        List<Map<String, Object>> topServices = analyticsService.getTopServicesByUsage(
                startDate, endDate, limit);
        
        return ResponseEntity.ok(topServices);
    }

    @GetMapping("/top-taxpayers/{serviceCode}")
    @Operation(summary = "پراستفاده‌ترین مودیان برای یک خدمات")
    public ResponseEntity<List<Map<String, Object>>> getTopTaxpayers(
            @PathVariable String serviceCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        
        List<Map<String, Object>> topTaxpayers = analyticsService.getTopTaxpayersByUsage(
                serviceCode, startDate, endDate, limit);
        
        return ResponseEntity.ok(topTaxpayers);
    }

    @GetMapping("/usage-by-hour/{serviceCode}")
    @Operation(summary = "توزیع استفاده بر اساس ساعت روز")
    public ResponseEntity<Map<String, Long>> getUsageByHour(
            @PathVariable String serviceCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        Map<String, Long> usageByHour = analyticsService.getServiceUsageByHour(serviceCode, date);
        
        return ResponseEntity.ok(usageByHour);
    }

    @GetMapping("/access-rates/{serviceCode}")
    @Operation(summary = "نرخ موفقیت/شکست دسترسی")
    public ResponseEntity<Map<String, Object>> getAccessRates(
            @PathVariable String serviceCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        Map<String, Object> rates = analyticsService.getServiceAccessRates(
                serviceCode, startDate, endDate);
        
        return ResponseEntity.ok(rates);
    }

    @GetMapping("/forecast/{serviceCode}")
    @Operation(summary = "پیش‌بینی استفاده از خدمات")
    public ResponseEntity<Map<String, Object>> getUsageForecast(
            @PathVariable String serviceCode,
            @RequestParam(required = false, defaultValue = "30") int forecastDays) {
        
        Map<String, Object> forecast = analyticsService.getServiceUsageForecast(
                serviceCode, forecastDays);
        
        return ResponseEntity.ok(forecast);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "داشبورد دسترسی به خدمات")
    public ResponseEntity<ServiceAccessDashboardDto> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        if (date == null) {
            date = LocalDate.now();
        }

        LocalDate startDate = date.withDayOfMonth(1);
        LocalDate endDate = date;

        // Get top services
        List<Map<String, Object>> topServicesData = analyticsService.getTopServicesByUsage(
                startDate, endDate, 10);
        
        // Get top taxpayers (for first service if available)
        List<Map<String, Object>> topTaxpayersData = List.of();
        if (!topServicesData.isEmpty()) {
            String firstServiceCode = (String) topServicesData.get(0).get("serviceCode");
            topTaxpayersData = analyticsService.getTopTaxpayersByUsage(
                    firstServiceCode, startDate, endDate, 10);
        }

        // Get usage by hour (for first service if available)
        Map<String, Long> usageByHour = Map.of();
        if (!topServicesData.isEmpty()) {
            String firstServiceCode = (String) topServicesData.get(0).get("serviceCode");
            usageByHour = analyticsService.getServiceUsageByHour(firstServiceCode, date);
        }

        // Get access rates (aggregated)
        Map<String, Object> accessRates = analyticsService.getServiceAccessRates(
                null, startDate, endDate);

        // Convert topServicesData to ServiceUsageRankingDto
        List<ServiceUsageRankingDto> topServices = new java.util.ArrayList<>();
        int rank = 1;
        for (Map<String, Object> serviceData : topServicesData) {
            topServices.add(new ServiceUsageRankingDto(
                    (String) serviceData.get("serviceCode"),
                    (String) serviceData.get("serviceName"),
                    ((Number) serviceData.get("usageCount")).longValue(),
                    rank++
            ));
        }

        // Convert topTaxpayersData to TaxpayerUsageRankingDto
        List<TaxpayerUsageRankingDto> topTaxpayers = new java.util.ArrayList<>();
        rank = 1;
        for (Map<String, Object> taxpayerData : topTaxpayersData) {
            String taxpayerIdStr = (String) taxpayerData.get("taxpayerId");
            UUID taxpayerId = UUID.fromString(taxpayerIdStr);
            TaxpayerEntity taxpayer = taxpayerRepository.findById(taxpayerId).orElse(null);
            
            topTaxpayers.add(new TaxpayerUsageRankingDto(
                    taxpayerIdStr,
                    taxpayer != null ? taxpayer.getName() : "Unknown",
                    ((Number) taxpayerData.get("usageCount")).longValue(),
                    ((Number) taxpayerData.getOrDefault("usagePercentage", 0.0)).doubleValue(),
                    (String) taxpayerData.getOrDefault("status", "UNKNOWN"),
                    rank++
            ));
        }

        // Get summary
        long activeServicesCount = serviceRepository.findAll().stream()
                .filter(ServiceEntity::getIsActive)
                .count();
        long activeSubscriptionsCount = subscriptionRepository.findActiveSubscriptions(date).size();

        ServiceAccessSummaryDto summary = new ServiceAccessSummaryDto(
                ((Number) accessRates.getOrDefault("totalAccess", 0L)).longValue(),
                ((Number) accessRates.getOrDefault("successfulAccess", 0L)).longValue(),
                ((Number) accessRates.getOrDefault("totalAccess", 0L)).longValue() - 
                ((Number) accessRates.getOrDefault("successfulAccess", 0L)).longValue(),
                ((Number) accessRates.getOrDefault("successRate", 0.0)).doubleValue(),
                ((Number) accessRates.getOrDefault("failureRate", 0.0)).doubleValue()
        );

        ServiceAccessDashboardDto dashboard = new ServiceAccessDashboardDto(
                date,
                summary,
                topServices,
                topTaxpayers,
                usageByHour,
                ((Number) accessRates.getOrDefault("successRate", 0.0)).doubleValue(),
                ((Number) accessRates.getOrDefault("failureRate", 0.0)).doubleValue(),
                activeServicesCount,
                activeSubscriptionsCount
        );

        return ResponseEntity.ok(dashboard);
    }
}

