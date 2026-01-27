package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.TaxpayerTaxProductService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxProductEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerTaxProductEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * کنترلر مدیریت رابطه بین Taxpayer و TaxProduct
 * 
 * <p>این کنترلر عملیات CRUD و جستجو برای رابطه بین مودی مالیاتی و محصول مالیاتی را مدیریت می‌کند.</p>
 */
@RestController
@RequestMapping("/api/taxpayer-tax-products")
@Tag(name = "Taxpayer Tax Product Management", description = "مدیریت رابطه بین مودی مالیاتی و محصول مالیاتی")
@RequiredArgsConstructor
public class TaxpayerTaxProductController {
    
    private final TaxpayerTaxProductService taxpayerTaxProductService;
    
    @Operation(
            summary = "دریافت تمام محصولات مالیاتی یک مودی مالیاتی",
            description = "دریافت لیست تمام محصولات مالیاتی مرتبط با یک مودی مالیاتی",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست محصولات مالیاتی با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = TaxProductEntity.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "مودی مالیاتی یافت نشد"
                    )
            }
    )
    @GetMapping("/taxpayer/{taxpayerId}/tax-products")
    public ResponseEntity<List<TaxProductEntity>> getTaxProductsByTaxpayerId(
            @PathVariable @NotNull UUID taxpayerId) {
        List<TaxProductEntity> taxProducts = taxpayerTaxProductService.findTaxProductsByTaxpayerId(taxpayerId);
        return ResponseEntity.ok(taxProducts);
    }
    
    @Operation(
            summary = "ایجاد رابطه بین مودی مالیاتی و محصول مالیاتی",
            description = "ایجاد یک رابطه جدید بین مودی مالیاتی و محصول مالیاتی",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "رابطه با موفقیت ایجاد شد",
                            content = @Content(schema = @Schema(implementation = TaxpayerTaxProductEntity.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "اطلاعات ورودی نامعتبر"
                    ),
                    @ApiResponse(
                            responseCode = "409",
                            description = "رابطه تکراری است"
                    )
            }
    )
    @PostMapping
    public ResponseEntity<TaxpayerTaxProductEntity> createRelation(
            @Valid @RequestBody TaxpayerTaxProductEntity entity) {
        TaxpayerTaxProductEntity created = taxpayerTaxProductService.createRelation(
                entity.getTaxpayerId(), 
                entity.getTaxProductId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @Operation(
            summary = "ایجاد روابط متعدد بین مودی مالیاتی و لیستی از محصولات مالیاتی (bulk)",
            description = "ایجاد روابط متعدد بین یک مودی مالیاتی و لیستی از محصولات مالیاتی در یک درخواست. " +
                    "در body باید یک TaxpayerTaxProductEntity با فیلدهای taxpayerId و taxProductIds (لیست) ارسال شود.",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "روابط با موفقیت ایجاد شدند",
                            content = @Content(schema = @Schema(implementation = TaxpayerTaxProductEntity.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "اطلاعات ورودی نامعتبر"
                    )
            }
    )
    @PostMapping("/bulk")
    public ResponseEntity<List<TaxpayerTaxProductEntity>> bulk(
            @Valid @RequestBody TaxpayerTaxProductEntity entity) {
        List<TaxpayerTaxProductEntity> created = taxpayerTaxProductService.bulk(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }


    @Operation(
            summary = "حذف رابطه بین مودی مالیاتی و محصول مالیاتی",
            description = "حذف رابطه بین مودی مالیاتی و محصول مالیاتی",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "رابطه با موفقیت حذف شد"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "رابطه یافت نشد"
                    )
            }
    )
    @DeleteMapping("/taxpayer/{taxpayerId}/tax-product/{taxProductId}")
    public ResponseEntity<Void> deleteRelation(
            @PathVariable @NotNull UUID taxpayerId,
            @PathVariable @NotNull UUID taxProductId) {
        taxpayerTaxProductService.deleteRelation(taxpayerId, taxProductId);
        return ResponseEntity.noContent().build();
    }
    
    @Operation(
            summary = "دریافت تمام روابط",
            description = "دریافت لیست تمام روابط بین مودیان مالیاتی و محصولات مالیاتی",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست روابط با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = TaxpayerTaxProductEntity.class))
                    )
            }
    )
    @GetMapping
    public ResponseEntity<List<TaxpayerTaxProductEntity>> getAllRelations() {
        List<TaxpayerTaxProductEntity> relations = taxpayerTaxProductService.findAll();
        return ResponseEntity.ok(relations);
    }
}

