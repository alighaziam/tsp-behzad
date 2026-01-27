package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.ServiceAccessService;
import ir.tax.trustedtaxpayers.application.service.SubscriptionService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ServiceEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.SubscriptionPlanEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.SubscriptionPlanServiceEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerSubscriptionEntity;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/subscriptions", "/api/v1/subscriptions"})
@Tag(name = "Subscriptions", description = "مدیریت اشتراک‌ها و پلن‌های اشتراک")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final ServiceAccessService serviceAccessService;

    @GetMapping("/plans")
    @Operation(summary = "لیست پلن‌های فعال")
    public ResponseEntity<List<SubscriptionPlanDto>> getActivePlans() {
        List<SubscriptionPlanEntity> plans = subscriptionService.getActivePlans();
        return ResponseEntity.ok(plans.stream().map(this::toPlanDto).collect(Collectors.toList()));
    }

    @GetMapping("/plans/{planId}")
    @Operation(summary = "دریافت پلن بر اساس شناسه")
    public ResponseEntity<SubscriptionPlanDto> getPlan(@PathVariable UUID planId) {
        return subscriptionService.getPlanById(planId)
                .map(this::toPlanDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/plans/{planId}/with-services")
    @Operation(summary = "دریافت پلن همراه با خدمات")
    public ResponseEntity<SubscriptionPlanWithServicesDto> getPlanWithServices(@PathVariable UUID planId) {
        return subscriptionService.getPlanById(planId)
                .map(this::toPlanWithServicesDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/plans/{planId}/services")
    @Operation(summary = "لیست خدمات یک پلن")
    public ResponseEntity<List<ServiceDto>> getPlanServices(@PathVariable UUID planId) {
        List<ServiceEntity> services = subscriptionService.getPlanServices(planId);
        return ResponseEntity.ok(services.stream().map(this::toServiceDto).collect(Collectors.toList()));
    }

    @GetMapping("/taxpayer/{taxpayerId}")
    @Operation(summary = "لیست اشتراک‌های یک مودی")
    public ResponseEntity<List<TaxpayerSubscriptionDto>> getTaxpayerSubscriptions(@PathVariable UUID taxpayerId) {
        List<TaxpayerSubscriptionEntity> subscriptions = subscriptionService.getTaxpayerSubscriptions(taxpayerId);
        return ResponseEntity.ok(subscriptions.stream().map(this::toSubscriptionDto).collect(Collectors.toList()));
    }

    @GetMapping("/taxpayer/{taxpayerId}/active")
    @Operation(summary = "اشتراک فعال مودی")
    public ResponseEntity<TaxpayerSubscriptionDto> getActiveSubscription(@PathVariable UUID taxpayerId) {
        return subscriptionService.getActiveSubscription(taxpayerId)
                .map(this::toSubscriptionDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }

    @GetMapping("/taxpayer/{taxpayerId}/services")
    @Operation(summary = "لیست خدمات قابل دسترس برای مودی")
    public ResponseEntity<List<ServiceDto>> getTaxpayerServices(@PathVariable UUID taxpayerId) {
        List<ServiceEntity> services = subscriptionService.getTaxpayerServices(taxpayerId);
        return ResponseEntity.ok(services.stream().map(this::toServiceDto).collect(Collectors.toList()));
    }

    @GetMapping("/taxpayer/{taxpayerId}/has-access/{serviceCode}")
    @Operation(summary = "بررسی دسترسی مودی به یک خدمات")
    public ResponseEntity<Boolean> hasTaxpayerServiceAccess(
            @PathVariable UUID taxpayerId,
            @PathVariable String serviceCode) {
        boolean hasAccess = subscriptionService.hasTaxpayerServiceAccess(taxpayerId, serviceCode);
        return ResponseEntity.ok(hasAccess);
    }

    @PostMapping("/purchase")
    @Operation(summary = "خرید اشتراک")
    public ResponseEntity<TaxpayerSubscriptionDto> purchaseSubscription(
            @RequestParam UUID taxpayerId,
            @Valid @RequestBody PurchaseSubscriptionRequest request) {
        
        TaxpayerSubscriptionEntity subscription = subscriptionService.purchaseSubscription(
                taxpayerId,
                request.planId(),
                request.paymentReference(),
                request.notes()
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(toSubscriptionDto(subscription));
    }

    @GetMapping("/expiring")
    @Operation(summary = "اشتراک‌های در حال انقضا")
    public ResponseEntity<List<TaxpayerSubscriptionDto>> getExpiringSubscriptions(
            @RequestParam(defaultValue = "30") int withinDays) {
        List<TaxpayerSubscriptionEntity> subscriptions = subscriptionService.getExpiringSubscriptions(withinDays);
        return ResponseEntity.ok(subscriptions.stream().map(this::toSubscriptionDto).collect(Collectors.toList()));
    }

    @GetMapping("/expired")
    @Operation(summary = "اشتراک‌های منقضی شده")
    public ResponseEntity<List<TaxpayerSubscriptionDto>> getExpiredSubscriptions() {
        List<TaxpayerSubscriptionEntity> subscriptions = subscriptionService.getExpiredSubscriptions();
        return ResponseEntity.ok(subscriptions.stream().map(this::toSubscriptionDto).collect(Collectors.toList()));
    }

    private SubscriptionPlanDto toPlanDto(SubscriptionPlanEntity entity) {
        return new SubscriptionPlanDto(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getCurrency(),
                entity.getDurationDays(),
                entity.getMaxInvoices(),
                entity.getFeatures(),
                entity.getIsActive()
        );
    }

    private TaxpayerSubscriptionDto toSubscriptionDto(TaxpayerSubscriptionEntity entity) {
        return new TaxpayerSubscriptionDto(
                entity.getId(),
                entity.getTaxpayerId(),
                entity.getPlanId(),
                entity.getPlan() != null ? entity.getPlan().getName() : null,
                entity.getPlan() != null ? entity.getPlan().getCode() : null,
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getStatus(),
                entity.getRemainingInvoices(),
                entity.getPaymentReference(),
                entity.getNotes(),
                entity.getIsActive()
        );
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

    private SubscriptionPlanWithServicesDto toPlanWithServicesDto(SubscriptionPlanEntity entity) {
        // دریافت اطلاعات کامل SubscriptionPlanServiceEntity
        List<SubscriptionPlanServiceEntity> planServices = serviceAccessService.getEnabledPlanServices(entity.getId());
        
        // تبدیل به DTO
        List<ir.tax.trustedtaxpayers.interfaces.rest.dto.SubscriptionPlanServiceDto> planServicesDto = 
                planServices.stream()
                .map(ps -> {
                    ServiceEntity service = ps.getService();
                    return new ir.tax.trustedtaxpayers.interfaces.rest.dto.SubscriptionPlanServiceDto(
                            ps.getId(),
                            ps.getPlan().getId(),
                            entity.getCode(),
                            entity.getName(),
                            ps.getService().getId(),
                            service != null ? service.getCode() : null,
                            service != null ? service.getName() : null,
                            service != null ? service.getCategory() : null,
                            ps.getIsActive(),
                            ps.getLimitValue(),
                            ps.getLimitUnit(),
                            ps.getNotes()
                    );
                })
                .collect(Collectors.toList());

        return new SubscriptionPlanWithServicesDto(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getCurrency(),
                entity.getDurationDays(),
                entity.getMaxInvoices(),
                entity.getFeatures(),
                entity.getIsActive(),
                planServicesDto
        );
    }
}

