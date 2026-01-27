package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.ElectronicInvoiceSystemService;
import ir.tax.trustedtaxpayers.application.service.InvoiceByTaxpayerService;
import ir.tax.trustedtaxpayers.application.service.InvoiceExportService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.interfaces.dto.ElectronicInvoiceDtos;
import ir.tax.trustedtaxpayers.interfaces.mapper.ElectronicInvoiceMapper;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

/**
 * کنترلر مدیریت صورتحساب‌های مودی برای جستجو، شمارش و صادرات گزارش‌ها بر اساس شناسه یا شماره اقتصادی.
 * <p>
 * REST controller that retrieves, paginates, and exports taxpayer-centric invoice reports by id or economic code.
 */
@RestController
@RequestMapping("/api/invoices/by-taxpayer")
@Tag(name = "Invoices by Taxpayer", description = "لود کردن صورتحساب‌ها بر اساس مودی مالیاتی\n\nراهنمای سریع:\n- Base: /api/invoices/by-taxpayer\n- صفحه‌بندی: GET /{taxpayerId}?page=0&size=20&sortBy=createdOn&direction=DESC\n- بر اساس شماره اقتصادی: GET /no/{taxpayerNo} (page)\n- فروشنده/خریدار: GET /{taxpayerId}/as-seller | /as-buyer\n- همه/تعداد: GET /{taxpayerId}/all | /count\n- خروجی: /export/pdf | /export/csv (برای id یا no)\n")
public class InvoiceByTaxpayerController {
    
    private final InvoiceByTaxpayerService invoiceByTaxpayerService;
    private final ElectronicInvoiceMapper mapper;
    private final ElectronicInvoiceSystemService systemService;
    private final InvoiceExportService exportService;
    
    public InvoiceByTaxpayerController(
            InvoiceByTaxpayerService invoiceByTaxpayerService,
            ElectronicInvoiceMapper mapper,
            ElectronicInvoiceSystemService systemService,
            InvoiceExportService exportService) {
        this.invoiceByTaxpayerService = invoiceByTaxpayerService;
        this.mapper = mapper;
        this.systemService = systemService;
        this.exportService = exportService;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @Operation(
            summary = "دریافت صورتحساب‌ها بر اساس مودی (Taxpayer ID)",
            description = "لود کردن لیست صورتحساب‌ها بر اساس شناسه مودی مالیاتی با پشتیبانی از pagination و sorting",
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
    @GetMapping("/{taxpayerId}")
    public ResponseEntity<Page<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto>> getInvoicesByTaxpayer(
            @PathVariable
            @Schema(description = "شناسه مودی مالیاتی (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
            UUID taxpayerId,
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
        
        /*Page<ElectronicInvoiceHeaderEntity> invoices = invoiceByTaxpayerService
            .loadInvoicesByTaxpayer(taxpayerId, page, size, sortBy, direction);*/

        Page<ElectronicInvoiceHeaderEntity> invoices = null;
        
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
            summary = "دریافت صورتحساب‌ها بر اساس شماره اقتصادی مودی",
            description = "لود کردن لیست صورتحساب‌ها بر اساس شماره اقتصادی (Taxpayer No) با pagination",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست صورتحساب‌ها با موفقیت برگردانده شد"
                    )
            }
    )
    @GetMapping("/no/{taxpayerNo}")
    public ResponseEntity<Page<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto>> getInvoicesByTaxpayerNo(
            @PathVariable
            @Schema(description = "شماره اقتصادی مودی", example = "1234567890")
            String taxpayerNo,
            @RequestParam(defaultValue = "0")
            Integer page,
            @RequestParam(defaultValue = "20")
            Integer size,
            @RequestParam(required = false, defaultValue = "createdOn")
            String sortBy,
            @RequestParam(required = false, defaultValue = "DESC")
            String direction,
            jakarta.servlet.http.HttpServletRequest request) {
        
        Page<ElectronicInvoiceHeaderEntity> invoices = invoiceByTaxpayerService
            .loadInvoicesByTaxpayerNo(taxpayerNo, page, size, sortBy, direction);
        
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
            summary = "دریافت صورتحساب‌ها به عنوان فروشنده",
            description = "لود کردن صورتحساب‌هایی که مودی به عنوان فروشنده در آن‌ها حضور دارد"
    )
    @GetMapping("/{taxpayerId}/as-seller")
    public ResponseEntity<Page<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto>> getInvoicesAsSeller(
            @PathVariable UUID taxpayerId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false, defaultValue = "createdOn") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String direction,
            jakarta.servlet.http.HttpServletRequest request) {
        
        Page<ElectronicInvoiceHeaderEntity> invoices = invoiceByTaxpayerService
            .loadInvoicesAsSeller(taxpayerId, page, size, sortBy, direction);
        
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
            summary = "دریافت صورتحساب‌ها به عنوان خریدار",
            description = "لود کردن صورتحساب‌هایی که مودی به عنوان خریدار در آن‌ها حضور دارد"
    )
    @GetMapping("/{taxpayerId}/as-buyer")
    public ResponseEntity<Page<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto>> getInvoicesAsBuyer(
            @PathVariable UUID taxpayerId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false, defaultValue = "createdOn") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String direction,
            jakarta.servlet.http.HttpServletRequest request) {
        
        Page<ElectronicInvoiceHeaderEntity> invoices = invoiceByTaxpayerService
            .loadInvoicesAsBuyer(taxpayerId, page, size, sortBy, direction);

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
            summary = "دریافت تمام صورتحساب‌ها بر اساس مودی (بدون Pagination)",
            description = "لود کردن تمام صورتحساب‌ها بر اساس شناسه مودی بدون pagination"
    )
    @GetMapping("/{taxpayerId}/all")
    public ResponseEntity<List<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto>> getAllInvoicesByTaxpayer(
            @PathVariable UUID taxpayerId) {
        
        var invoices = invoiceByTaxpayerService.loadAllInvoicesByTaxpayer(taxpayerId);
        
        Map<UUID, String> stsMap = systemService.findStsByHeaderIds(invoices.stream().map(ElectronicInvoiceHeaderEntity::getId).toList());
        
        var dtos = invoices.stream()
            .map(invoice -> mapper.toSummaryDto(invoice, stsMap.get(invoice.getId())))
            .toList();
        
        return ResponseEntity.ok(dtos);
    }

    @Operation(
            summary = "تعداد صورتحساب‌ها بر اساس مودی",
            description = "دریافت تعداد کل صورتحساب‌های مربوط به یک مودی"
    )
    @GetMapping("/{taxpayerId}/count")
    public ResponseEntity<Long> countInvoicesByTaxpayer(@PathVariable UUID taxpayerId) {
        long count = invoiceByTaxpayerService.countInvoicesByTaxpayer(taxpayerId);
        return ResponseEntity.ok(count);
    }

    @Operation(
            summary = "Export صورتحساب‌ها به PDF بر اساس مودی",
            description = "خروجی PDF گزارش صورتحساب‌های یک مودی"
    )
    @GetMapping("/{taxpayerId}/export/pdf")
    public ResponseEntity<byte[]> exportToPdf(
            @PathVariable UUID taxpayerId,
            @RequestParam(required = false, defaultValue = "گزارش صورتحساب‌های مودی")
            String title) {
        
        List<ElectronicInvoiceHeaderEntity> invoices = invoiceByTaxpayerService
            .loadAllInvoicesByTaxpayer(taxpayerId);
        
        byte[] pdfContent = exportService.exportToPdf(invoices, title);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", 
            String.format("invoices_taxpayer_%s.pdf", taxpayerId));
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfContent);
    }

    @Operation(
            summary = "Export صورتحساب‌ها به CSV بر اساس مودی",
            description = "خروجی CSV گزارش صورتحساب‌های یک مودی"
    )
    @GetMapping(value = "/{taxpayerId}/export/csv", produces = "text/csv")
    public ResponseEntity<String> exportToCsv(@PathVariable UUID taxpayerId) {
        
        List<ElectronicInvoiceHeaderEntity> invoices = invoiceByTaxpayerService
            .loadAllInvoicesByTaxpayer(taxpayerId);
        
        String csvContent = exportService.exportToCsv(invoices);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", 
            String.format("invoices_taxpayer_%s.csv", taxpayerId));
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(csvContent);
    }

    @Operation(
            summary = "Export صورتحساب‌ها به PDF بر اساس شماره اقتصادی",
            description = "خروجی PDF گزارش صورتحساب‌های یک مودی بر اساس شماره اقتصادی"
    )
    @GetMapping("/no/{taxpayerNo}/export/pdf")
    public ResponseEntity<byte[]> exportToPdfByTaxpayerNo(
            @PathVariable String taxpayerNo,
            @RequestParam(required = false, defaultValue = "گزارش صورتحساب‌های مودی")
            String title) {
        
        List<ElectronicInvoiceHeaderEntity> invoices = invoiceByTaxpayerService
            .loadAllInvoicesByTaxpayerNo(taxpayerNo);
        
        byte[] pdfContent = exportService.exportToPdf(invoices, title);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", 
            String.format("invoices_taxpayer_%s.pdf", taxpayerNo));
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(pdfContent);
    }

    @Operation(
            summary = "Export صورتحساب‌ها به CSV بر اساس شماره اقتصادی",
            description = "خروجی CSV گزارش صورتحساب‌های یک مودی بر اساس شماره اقتصادی"
    )
    @GetMapping(value = "/no/{taxpayerNo}/export/csv", produces = "text/csv")
    public ResponseEntity<String> exportToCsvByTaxpayerNo(@PathVariable String taxpayerNo) {
        
        List<ElectronicInvoiceHeaderEntity> invoices = invoiceByTaxpayerService
            .loadAllInvoicesByTaxpayerNo(taxpayerNo);
        
        String csvContent = exportService.exportToCsv(invoices);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", 
            String.format("invoices_taxpayer_%s.csv", taxpayerNo));
        
        return ResponseEntity.ok()
            .headers(headers)
            .body(csvContent);
    }
}


