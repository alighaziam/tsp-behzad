package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.ServiceAccessService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.SubscriptionPlanServiceEntity;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.SubscriptionPlanServiceDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping({"/api/admin/services", "/api/v1/admin/services"})
@Tag(name = "Admin Services", description = "مدیریت خدمات و دسترسی‌ها (فقط برای مدیران)")
@PreAuthorize("hasAuthority('ADMIN')")
@RequiredArgsConstructor
public class AdminServiceController {

    private final ServiceAccessService serviceAccessService;

    @PostMapping("/plans/{planId}/services/{serviceId}")
    @Operation(summary = "افزودن خدمات به طرح اشتراک")
    public ResponseEntity<SubscriptionPlanServiceDto> addServiceToPlan(
            @PathVariable UUID planId,
            @PathVariable UUID serviceId,
            @RequestParam(required = false) Boolean isEnabled,
            @RequestParam(required = false) BigDecimal limitValue,
            @RequestParam(required = false) String limitUnit,
            @RequestParam(required = false) String notes) {
        
        SubscriptionPlanServiceEntity planService = serviceAccessService.addServiceToPlan(
            planId, serviceId, isEnabled, limitValue, limitUnit, notes
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).body(toPlanServiceDto(planService));
    }

    @PutMapping("/plan-services/{planServiceId}")
    @Operation(summary = "به‌روزرسانی تنظیمات خدمات در طرح")
    public ResponseEntity<SubscriptionPlanServiceDto> updatePlanService(
            @PathVariable UUID planServiceId,
            @RequestParam(required = false) Boolean isEnabled,
            @RequestParam(required = false) BigDecimal limitValue,
            @RequestParam(required = false) String limitUnit,
            @RequestParam(required = false) String notes) {
        
        SubscriptionPlanServiceEntity planService = serviceAccessService.updatePlanService(
            planServiceId, isEnabled, limitValue, limitUnit, notes
        );
        
        return ResponseEntity.ok(toPlanServiceDto(planService));
    }

    @DeleteMapping("/plans/{planId}/services/{serviceId}")
    @Operation(summary = "حذف خدمات از طرح اشتراک")
    public ResponseEntity<Void> removeServiceFromPlan(
            @PathVariable UUID planId,
            @PathVariable UUID serviceId) {
        
        serviceAccessService.removeServiceFromPlan(planId, serviceId);
        return ResponseEntity.noContent().build();
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
}

