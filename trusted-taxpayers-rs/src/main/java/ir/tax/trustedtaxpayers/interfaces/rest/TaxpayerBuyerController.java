package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.TaxpayerBuyerService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerBuyerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * کنترلر مدیریت رابطه بین Taxpayer (فروشنده) و Buyer (خریدار)
 * 
 * <p>این کنترلر عملیات CRUD و جستجو برای رابطه بین مودی مالیاتی به عنوان فروشنده و مودی مالیاتی به عنوان خریدار را مدیریت می‌کند.</p>
 */
@RestController
@RequestMapping("/api/taxpayer-buyers")
@PreAuthorize("hasAuthority('ADMIN')")
@Tag(name = "Taxpayer Buyer Management", description = "مدیریت رابطه بین مودی مالیاتی (فروشنده) و خریدار")
@RequiredArgsConstructor
public class TaxpayerBuyerController {
    
    private final TaxpayerBuyerService taxpayerBuyerService;
    
    @Operation(
            summary = "دریافت تمام خریداران یک مودی مالیاتی",
            description = "دریافت لیست تمام خریداران مرتبط با یک مودی مالیاتی (فروشنده)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست خریداران با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = TaxpayerEntity.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "مودی مالیاتی یافت نشد"
                    )
            }
    )
    @GetMapping("/taxpayer/{taxpayerId}/buyers")
    public ResponseEntity<List<TaxpayerEntity>> getBuyersByTaxpayerId(
            @PathVariable @NotNull UUID taxpayerId) {
        List<TaxpayerEntity> buyers = taxpayerBuyerService.findBuyersByTaxpayerId(taxpayerId);
        return ResponseEntity.ok(buyers);
    }
    
    @Operation(
            summary = "دریافت تمام فروشندگان یک خریدار",
            description = "دریافت لیست تمام فروشندگان مرتبط با یک خریدار",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست فروشندگان با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = TaxpayerEntity.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "خریدار یافت نشد"
                    )
            }
    )
    @GetMapping("/buyer/{buyerId}/taxpayers")
    public ResponseEntity<List<TaxpayerEntity>> getTaxpayersByBuyerId(
            @PathVariable @NotNull UUID buyerId) {
        List<TaxpayerEntity> taxpayers = taxpayerBuyerService.findTaxpayersByBuyerId(buyerId);
        return ResponseEntity.ok(taxpayers);
    }
    
    @Operation(
            summary = "ایجاد رابطه بین مودی مالیاتی و خریدار",
            description = "ایجاد یک رابطه جدید بین مودی مالیاتی (فروشنده) و خریدار",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "رابطه با موفقیت ایجاد شد",
                            content = @Content(schema = @Schema(implementation = TaxpayerBuyerEntity.class))
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
    public ResponseEntity<TaxpayerBuyerEntity> createRelation(
            @Valid @RequestBody TaxpayerBuyerEntity entity) {
        TaxpayerBuyerEntity created = taxpayerBuyerService.createRelation(
                entity.getTaxpayerId(), 
                entity.getBuyerId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
    
    @Operation(
            summary = "حذف رابطه بین مودی مالیاتی و خریدار",
            description = "حذف رابطه بین مودی مالیاتی (فروشنده) و خریدار",
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
    @DeleteMapping("/taxpayer/{taxpayerId}/buyer/{buyerId}")
    public ResponseEntity<Void> deleteRelation(
            @PathVariable @NotNull UUID taxpayerId,
            @PathVariable @NotNull UUID buyerId) {
        taxpayerBuyerService.deleteRelation(taxpayerId, buyerId);
        return ResponseEntity.noContent().build();
    }
    
    @Operation(
            summary = "دریافت تمام روابط",
            description = "دریافت لیست تمام روابط بین مودیان مالیاتی و خریداران",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست روابط با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = TaxpayerBuyerEntity.class))
                    )
            }
    )
    @GetMapping
    public ResponseEntity<List<TaxpayerBuyerEntity>> getAllRelations() {
        List<TaxpayerBuyerEntity> relations = taxpayerBuyerService.findAll();
        return ResponseEntity.ok(relations);
    }
}






