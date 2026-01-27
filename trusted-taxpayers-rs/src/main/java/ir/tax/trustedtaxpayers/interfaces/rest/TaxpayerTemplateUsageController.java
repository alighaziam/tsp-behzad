package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.biz.service.TaxpayerTemplateUsageService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerTemplateUsageEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping({"/api/taxpayer-template-usage", "/api/v1/taxpayer-template-usage"})
@Tag(name = "Taxpayer Template Usage Management", description = "مدیریت استفاده مودی از قالب‌ها")
public class TaxpayerTemplateUsageController {
    
    private final TaxpayerTemplateUsageService service;
    
    public TaxpayerTemplateUsageController(TaxpayerTemplateUsageService service) {
        this.service = service;
    }
    
    @Operation(
            summary = "دریافت لیست قالب‌های استفاده شده توسط یک مودی",
            description = "دریافت 10 قالب آخرین استفاده شده توسط یک مودی بر اساس زمان استفاده",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست قالب‌ها با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = TaxpayerTemplateUsageEntity.class))
                    )
            }
    )
    @GetMapping("/taxpayer/{taxpayerId}/templates")
    public ResponseEntity<List<TaxpayerTemplateUsageEntity>> getTemplatesByTaxpayerId(
            @PathVariable
            @Schema(description = "شناسه مودی", example = "123e4567-e89b-12d3-a456-426614174000")
            UUID taxpayerId) {
        List<TaxpayerTemplateUsageEntity> templates = service.findTop10ByTaxpayerIdOrderByLastUsedAtDesc(taxpayerId);
        return ResponseEntity.ok(templates);
    }
}





