/*
package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.ElectronicInvoiceSystemService;
import ir.tax.trustedtaxpayers.application.service.InvoiceExportService;a
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.interfaces.dto.ElectronicInvoiceDtos;
import ir.tax.trustedtaxpayers.interfaces.mapper.ElectronicInvoiceMapper;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

*/
/**
 * کنترلر گزارش صورتحساب بر اساس خریدار و فروشنده برای مشاهده مشترک، شمارش و خروجی گرفتن.
 * <p>
 * REST controller exposing buyer/seller combined invoice listings with pagination, exports, and cache actions.
 *//*

@RestController
@RequestMapping("/api/invoices/by-buyer-seller")
@Tag(name = "Invoices by Buyer and Seller", description = "لود کردن صورتحساب‌ها بر اساس خریدار و فروشنده\n\nراهنمای سریع:\n- Base: /api/invoices/by-buyer-seller\n- فروشنده: GET /seller/{sellerTaxpayerId}?page=0&size=20...\n- خریدار: GET /buyer/{buyerTaxpayerId}?page=0&size=20...\n- هر دو: GET /seller/{sellerId}/buyer/{buyerId}?page=0&size=20...\n- نسخه‌های all | count موجود است\n- کش: DELETE /cache/seller/{id} | /cache/buyer/{id} | /cache/all\n- خروجی: مسیرهای export/pdf | export/csv موجود است\n")
@PreAuthorize("hasAuthority('ADMIN')")
public class InvoiceByBuyerAndSellerController {
    
    private final InvoiceByBuyerAndSellerService invoiceByBuyerAndSellerService;
    private final ElectronicInvoiceMapper mapper;
    private final ElectronicInvoiceSystemService systemService;
    private final InvoiceExportService exportService;
    
    public InvoiceByBuyerAndSellerController(
            InvoiceByBuyerAndSellerService invoiceByBuyerAndSellerService,
            ElectronicInvoiceMapper mapper,
            ElectronicInvoiceSystemService systemService,
            InvoiceExportService exportService) {
        this.invoiceByBuyerAndSellerService = invoiceByBuyerAndSellerService;
        this.mapper = mapper;
        this.systemService = systemService;
        this.exportService = exportService;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @Operation(
            summary = "دریافت صورتحساب‌ها بر اساس فروشنده",
            description = "لود کردن لیست صورتحساب‌ها بر اساس شناسه مودی فروشنده با پشتیبانی از pagination و sorting",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست صورتحساب‌ها با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto.class)
                            )
                    )
            }
    )
    @GetMapping("/seller/{sellerTaxpayerId}")
    public ResponseEntity<Page<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto>> getInvoicesBySeller(
            @PathVariable
            @Schema(description = "شناسه مودی فروشنده (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID sellerTaxpayerId,
            @RequestParam(defaultValue = "0")
            @Schema(description = "شماره صفحه (0-based)", example = "0")
            Integer page,
            @RequestParam(defaultValue = "20")
            @Schema(description = "تعداد آیتم در هر صفحه", example = "20")
            Integer size,
            @RequestParam(required = false, defaultValue = "createdOn")
            @Schema(description = "فیلد برای sort", example = "createdOn")
            String sortBy,
            @RequestParam(required = false, defaultValue = "DESC")
            @Schema(description = "جهت sort (ASC/DESC)", example = "DESC")
            String direction,
            jakarta.servlet.http.HttpServletRequest request) {
        
        Page<ElectronicInvoiceHeaderEntity> invoices = invoiceByBuyerAndSellerService
            .loadInvoicesBySeller(sellerTaxpayerId, page, size, sortBy, direction);
        
        // دریافت sts به صورت batch برای جلوگیری از N+1
        java.util.List<UUID> headerIds = invoices.getContent().stream().map(ElectronicInvoiceHeaderEntity::getId).toList();
        Map<UUID, String> stsMap = systemService.findStsByHeaderIds(headerIds);
        
        Page<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto> dtos = invoices.map(invoice -> 
            mapper.toSummaryDto(invoice, stsMap.get(invoice.getId()))
        );
        
        org.springframework.http.HttpHeaders headers = ir.tax.trustedtaxpayers.interfaces.rest.util.PaginationHeaderUtil
                .buildLinkHeaders(dtos, page, size, request.getRequestURI());
        return ResponseEntity.ok().headers(headers).body(dtos);
    }

    @Operation(
            summary = "دریافت صورتحساب‌ها بر اساس خریدار",
            description = "لود کردن لیست صورتحساب‌ها بر اساس شناسه مودی خریدار با پشتیبانی از pagination و sorting",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست صورتحساب‌ها با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto.class)
                            )
                    )
            }
    )
    @GetMapping("/buyer/{buyerTaxpayerId}")
    public ResponseEntity<Page<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto>> getInvoicesByBuyer(
            @PathVariable
            @Schema(description = "شناسه مودی خریدار (UUID)", example = "660e8400-e29b-41d4-a716-446655440000")
            UUID buyerTaxpayerId,
            @RequestParam(defaultValue = "0")
            @Schema(description = "شماره صفحه (0-based)", example = "0")
            Integer page,
            @RequestParam(defaultValue = "20")
            @Schema(description = "تعداد آیتم در هر صفحه", example = "20")
            Integer size,
            @RequestParam(required = false, defaultValue = "createdOn")
            @Schema(description = "فیلد برای sort", example = "createdOn")
            String sortBy,
            @RequestParam(required = false, defaultValue = "DESC")
            @Schema(description = "جهت sort (ASC/DESC)", example = "DESC")
            String direction,
            jakarta.servlet.http.HttpServletRequest request) {
        
        Page<ElectronicInvoiceHeaderEntity> invoices = invoiceByBuyerAndSellerService
            .loadInvoicesByBuyer(buyerTaxpayerId, page, size, sortBy, direction);
        
        java.util.List<UUID> headerIds = invoices.getContent().stream().map(ElectronicInvoiceHeaderEntity::getId).toList();
        Map<UUID, String> stsMap = systemService.findStsByHeaderIds(headerIds);
        
        Page<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto> dtos = invoices.map(invoice -> 
            mapper.toSummaryDto(invoice, stsMap.get(invoice.getId()))
        );
        
        org.springframework.http.HttpHeaders headers = ir.tax.trustedtaxpayers.interfaces.rest.util.PaginationHeaderUtil
                .buildLinkHeaders(dtos, page, size, request.getRequestURI());
        return ResponseEntity.ok().headers(headers).body(dtos);
    }

    @Operation(
            summary = "دریافت صورتحساب‌ها بر اساس خریدار و فروشنده",
            description = "لود کردن لیست صورتحساب‌ها بر اساس شناسه مودی خریدار و فروشنده با پشتیبانی از pagination و sorting",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست صورتحساب‌ها با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto.class)
                            )
                    )
            }
    )
    @GetMapping("/seller/{sellerTaxpayerId}/buyer/{buyerTaxpayerId}")
    public ResponseEntity<Page<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto>> getInvoicesByBuyerAndSeller(
            @PathVariable
            @Schema(description = "شناسه مودی فروشنده (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID sellerTaxpayerId,
            @PathVariable
            @Schema(description = "شناسه مودی خریدار (UUID)", example = "660e8400-e29b-41d4-a716-446655440000")
            UUID buyerTaxpayerId,
            @RequestParam(defaultValue = "0")
            @Schema(description = "شماره صفحه (0-based)", example = "0")
            Integer page,
            @RequestParam(defaultValue = "20")
            @Schema(description = "تعداد آیتم در هر صفحه", example = "20")
            Integer size,
            @RequestParam(required = false, defaultValue = "createdOn")
            @Schema(description = "فیلد برای sort", example = "createdOn")
            String sortBy,
            @RequestParam(required = false, defaultValue = "DESC")
            @Schema(description = "جهت sort (ASC/DESC)", example = "DESC")
            String direction,
            jakarta.servlet.http.HttpServletRequest request) {
        
        Page<ElectronicInvoiceHeaderEntity> invoices = invoiceByBuyerAndSellerService
            .loadInvoicesByBuyerAndSeller(sellerTaxpayerId, buyerTaxpayerId, page, size, sortBy, direction);
        
        java.util.List<UUID> headerIds = invoices.getContent().stream().map(ElectronicInvoiceHeaderEntity::getId).toList();
        Map<UUID, String> stsMap = systemService.findStsByHeaderIds(headerIds);
        
        Page<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto> dtos = invoices.map(invoice -> 
            mapper.toSummaryDto(invoice, stsMap.get(invoice.getId()))
        );
        
        org.springframework.http.HttpHeaders headers = ir.tax.trustedtaxpayers.interfaces.rest.util.PaginationHeaderUtil
                .buildLinkHeaders(dtos, page, size, request.getRequestURI());
        return ResponseEntity.ok().headers(headers).body(dtos);
    }

    @Operation(
            summary = "دریافت تمام صورتحساب‌ها بر اساس فروشنده (بدون Pagination)",
            description = "لود کردن تمام صورتحساب‌ها بر اساس شناسه مودی فروشنده بدون pagination",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست صورتحساب‌ها با موفقیت برگردانده شد"
                    )
            }
    )
    @GetMapping("/seller/{sellerTaxpayerId}/all")
    public ResponseEntity<List<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto>> getAllInvoicesBySeller(
            @PathVariable
            @Schema(description = "شناسه مودی فروشنده", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID sellerTaxpayerId) {
        
        var invoices = invoiceByBuyerAndSellerService.loadInvoicesBySeller(sellerTaxpayerId);
        
        Map<UUID, String> stsMap = systemService.findStsByHeaderIds(invoices.stream().map(ElectronicInvoiceHeaderEntity::getId).toList());
        
        var dtos = invoices.stream()
            .map(invoice -> mapper.toSummaryDto(invoice, stsMap.get(invoice.getId())))
            .toList();
        
        return ResponseEntity.ok(dtos);
    }

    @Operation(
            summary = "دریافت تمام صورتحساب‌ها بر اساس خریدار (بدون Pagination)",
            description = "لود کردن تمام صورتحساب‌ها بر اساس شناسه مودی خریدار بدون pagination",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست صورتحساب‌ها با موفقیت برگردانده شد"
                    )
            }
    )
    @GetMapping("/buyer/{buyerTaxpayerId}/all")
    public ResponseEntity<List<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto>> getAllInvoicesByBuyer(
            @PathVariable
            @Schema(description = "شناسه مودی خریدار", example = "660e8400-e29b-41d4-a716-446655440000")
            UUID buyerTaxpayerId) {
        
        var invoices = invoiceByBuyerAndSellerService.loadInvoicesByBuyer(buyerTaxpayerId);
        
        Map<UUID, String> stsMap = systemService.findStsByHeaderIds(invoices.stream().map(ElectronicInvoiceHeaderEntity::getId).toList());
        
        var dtos = invoices.stream()
            .map(invoice -> mapper.toSummaryDto(invoice, stsMap.get(invoice.getId())))
            .toList();
        
        return ResponseEntity.ok(dtos);
    }

    @Operation(
            summary = "تعداد صورتحساب‌ها بر اساس فروشنده",
            description = "دریافت تعداد کل صورتحساب‌های مربوط به یک فروشنده",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "تعداد با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = Long.class),
                                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            value = "250"
                                    )
                            )
                    )
            }
    )
    @GetMapping("/seller/{sellerTaxpayerId}/count")
    public ResponseEntity<Long> countInvoicesBySeller(
            @PathVariable
            @Schema(description = "شناسه مودی فروشنده", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID sellerTaxpayerId) {
        
        long count = invoiceByBuyerAndSellerService.countInvoicesBySeller(sellerTaxpayerId);
        return ResponseEntity.ok(count);
    }

    @Operation(
            summary = "تعداد صورتحساب‌ها بر اساس خریدار",
            description = "دریافت تعداد کل صورتحساب‌های مربوط به یک خریدار",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "تعداد با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = Long.class),
                                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            value = "150"
                                    )
                            )
                    )
            }
    )
    @GetMapping("/buyer/{buyerTaxpayerId}/count")
    public ResponseEntity<Long> countInvoicesByBuyer(
            @PathVariable
            @Schema(description = "شناسه مودی خریدار", example = "660e8400-e29b-41d4-a716-446655440000")
            UUID buyerTaxpayerId) {
        
        long count = invoiceByBuyerAndSellerService.countInvoicesByBuyer(buyerTaxpayerId);
        return ResponseEntity.ok(count);
    }

    @Operation(
            summary = "تعداد صورتحساب‌ها بر اساس خریدار و فروشنده",
            description = "دریافت تعداد کل صورتحساب‌های مربوط به یک جفت خریدار-فروشنده",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "تعداد با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = Long.class),
                                    examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            value = "50"
                                    )
                            )
                    )
            }
    )
    @GetMapping("/seller/{sellerTaxpayerId}/buyer/{buyerTaxpayerId}/count")
    public ResponseEntity<Long> countInvoicesByBuyerAndSeller(
            @PathVariable
            @Schema(description = "شناسه مودی فروشنده", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID sellerTaxpayerId,
            @PathVariable
            @Schema(description = "شناسه مودی خریدار", example = "660e8400-e29b-41d4-a716-446655440000")
            UUID buyerTaxpayerId) {
        
        long count = invoiceByBuyerAndSellerService.countInvoicesByBuyerAndSeller(sellerTaxpayerId, buyerTaxpayerId);
        return ResponseEntity.ok(count);
    }

    @Operation(
            summary = "Invalidate Cache برای فروشنده",
            description = "پاک کردن cache صورتحساب‌های یک فروشنده"
    )
    @DeleteMapping("/cache/seller/{sellerTaxpayerId}")
    public ResponseEntity<Void> invalidateSellerCache(
            @PathVariable
            @Schema(description = "شناسه مودی فروشنده", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID sellerTaxpayerId) {
        
        invoiceByBuyerAndSellerService.invalidateSellerCache(sellerTaxpayerId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Invalidate Cache برای خریدار",
            description = "پاک کردن cache صورتحساب‌های یک خریدار"
    )
    @DeleteMapping("/cache/buyer/{buyerTaxpayerId}")
    public ResponseEntity<Void> invalidateBuyerCache(
            @PathVariable
            @Schema(description = "شناسه مودی خریدار", example = "660e8400-e29b-41d4-a716-446655440000")
            UUID buyerTaxpayerId) {
        
        invoiceByBuyerAndSellerService.invalidateBuyerCache(buyerTaxpayerId);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Invalidate تمام Cache ها",
            description = "پاک کردن تمام cache های صورتحساب‌های خریدار و فروشنده"
    )
    @DeleteMapping("/cache/all")
    public ResponseEntity<Void> invalidateAllCache() {
        invoiceByBuyerAndSellerService.invalidateAllCache();
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Export صورتحساب‌ها به PDF بر اساس فروشنده",
            description = "خروجی PDF گزارش صورتحساب‌های یک فروشنده"
    )
    @GetMapping("/seller/{sellerTaxpayerId}/export/pdf")
    public ResponseEntity<byte[]> exportSellerToPdf(
            @PathVariable UUID sellerTaxpayerId,
            @RequestParam(required = false, defaultValue = "گزارش صورتحساب‌های فروشنده")
            String title) {
        
        List<ElectronicInvoiceHeaderEntity> invoices = invoiceByBuyerAndSellerService
            .loadInvoicesBySeller(sellerTaxpayerId);
        
        byte[] pdfContent = exportService.exportToPdf(invoices, title);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", 
            String.format("invoices_seller_%s.pdf", sellerTaxpayerId));
        
        return ResponseEntity.ok().headers(headers).body(pdfContent);
    }

    @Operation(
            summary = "Export صورتحساب‌ها به CSV بر اساس فروشنده",
            description = "خروجی CSV گزارش صورتحساب‌های یک فروشنده"
    )
    @GetMapping(value = "/seller/{sellerTaxpayerId}/export/csv", produces = "text/csv")
    public ResponseEntity<String> exportSellerToCsv(@PathVariable UUID sellerTaxpayerId) {
        
        List<ElectronicInvoiceHeaderEntity> invoices = invoiceByBuyerAndSellerService
            .loadInvoicesBySeller(sellerTaxpayerId);
        
        String csvContent = exportService.exportToCsv(invoices);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", 
            String.format("invoices_seller_%s.csv", sellerTaxpayerId));
        
        return ResponseEntity.ok().headers(headers).body(csvContent);
    }

    @Operation(
            summary = "Export صورتحساب‌ها به PDF بر اساس خریدار",
            description = "خروجی PDF گزارش صورتحساب‌های یک خریدار"
    )
    @GetMapping("/buyer/{buyerTaxpayerId}/export/pdf")
    public ResponseEntity<byte[]> exportBuyerToPdf(
            @PathVariable UUID buyerTaxpayerId,
            @RequestParam(required = false, defaultValue = "گزارش صورتحساب‌های خریدار")
            String title) {
        
        List<ElectronicInvoiceHeaderEntity> invoices = invoiceByBuyerAndSellerService
            .loadInvoicesByBuyer(buyerTaxpayerId);
        
        byte[] pdfContent = exportService.exportToPdf(invoices, title);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", 
            String.format("invoices_buyer_%s.pdf", buyerTaxpayerId));
        
        return ResponseEntity.ok().headers(headers).body(pdfContent);
    }

    @Operation(
            summary = "Export صورتحساب‌ها به CSV بر اساس خریدار",
            description = "خروجی CSV گزارش صورتحساب‌های یک خریدار"
    )
    @GetMapping(value = "/buyer/{buyerTaxpayerId}/export/csv", produces = "text/csv")
    public ResponseEntity<String> exportBuyerToCsv(@PathVariable UUID buyerTaxpayerId) {
        
        List<ElectronicInvoiceHeaderEntity> invoices = invoiceByBuyerAndSellerService
            .loadInvoicesByBuyer(buyerTaxpayerId);
        
        String csvContent = exportService.exportToCsv(invoices);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", 
            String.format("invoices_buyer_%s.csv", buyerTaxpayerId));
        
        return ResponseEntity.ok().headers(headers).body(csvContent);
    }
}


*/
