/*
package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.ElectronicInvoiceSystemService;
import ir.tax.trustedtaxpayers.application.service.InvoiceByTemplateService;
import ir.tax.trustedtaxpayers.application.service.InvoiceExportService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.interfaces.dto.ElectronicInvoiceDtos;
import ir.tax.trustedtaxpayers.interfaces.mapper.ElectronicInvoiceMapper;
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
 * کنترلر مدیریت صورتحساب‌های مبتنی بر الگو برای بازیابی، شمارش و صدور خروجی.
 * <p>
 * REST controller exposing endpoints to list, count, cache-invalidate, and export invoices grouped by template code.
 *//*

@RestController
@RequestMapping("/api/invoices/by-template")
@Tag(name = "Invoices by Template", description = "لود کردن صورتحساب‌ها بر اساس الگو\n\nراهنمای سریع:\n- Base: /api/invoices/by-template\n- صفحه‌بندی: GET /{templateCode}?page=0&size=20&sortBy=createdOn&direction=DESC\n- همه: GET /{templateCode}/all | فیلتر نوع: /{templateCode}/inty/{inty}\n- تعداد: /{templateCode}/count | کش: DELETE /cache/{templateCode}\n- خروجی: /{templateCode}/export/pdf | /export/csv\n")
public class InvoiceByTemplateController {
    
    private final InvoiceByTemplateService invoiceByTemplateService;
    private final ElectronicInvoiceMapper mapper;
    private final ElectronicInvoiceSystemService systemService;
    private final InvoiceExportService exportService;
    
    public InvoiceByTemplateController(
            InvoiceByTemplateService invoiceByTemplateService,
            ElectronicInvoiceMapper mapper,
            ElectronicInvoiceSystemService systemService,
            InvoiceExportService exportService) {
        this.invoiceByTemplateService = invoiceByTemplateService;
        this.mapper = mapper;
        this.systemService = systemService;
        this.exportService = exportService;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @Operation(
            summary = "دریافت صورتحساب‌ها بر اساس الگو (با Pagination)",
            description = "لود کردن لیست صورتحساب‌ها بر اساس Template code با پشتیبانی از pagination و sorting",
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
    @GetMapping("/{templateCode}")
    public ResponseEntity<Page<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto>> getInvoicesByTemplate(
            @PathVariable
            @Schema(description = "کد الگو (مثل T01RETAIL, T01WHOLE, ...)", example = "T01RETAIL")
            String templateCode,
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
        
        Page<ElectronicInvoiceHeaderEntity> invoices = invoiceByTemplateService
            .loadInvoicesByTemplate(templateCode, page, size, sortBy, direction);
        
        // دریافت sts برای هر invoice از system service
        Map<UUID, String> stsMap = invoices.getContent().stream()
            .collect(Collectors.toMap(
                ElectronicInvoiceHeaderEntity::getId,
                invoice -> systemService.findByHeaderId(invoice.getId())
                    .map(sys -> sys.getSts())
                    .orElse((String) null)
            ));
        
        Page<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto> dtos = invoices.map(invoice -> 
            mapper.toSummaryDto(invoice, stsMap.get(invoice.getId()))
        );
        
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        int totalPages = dtos.getTotalPages();
        java.util.List<String> links = new java.util.ArrayList<>();
        String base = request.getRequestURI();
        java.util.function.BiFunction<Integer, Integer, String> linkOf = (p, s) -> "<" + base + "?page=" + p + "&size=" + s + ">; rel=\"%s\"";
        if (totalPages > 0) {
            links.add(String.format(linkOf.apply(0, size), "first"));
            if (page > 0) links.add(String.format(linkOf.apply(page - 1, size), "prev"));
            if (page + 1 < totalPages) links.add(String.format(linkOf.apply(page + 1, size), "next"));
            links.add(String.format(linkOf.apply(Math.max(totalPages - 1, 0), size), "last"));
        }
        if (!links.isEmpty()) headers.add(org.springframework.http.HttpHeaders.LINK, String.join(", ", links));
        headers.add("X-Total-Count", String.valueOf(dtos.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(dtos);
    }

    @Operation(
            summary = "دریافت تمام صورتحساب‌ها بر اساس الگو (بدون Pagination)",
            description = "لود کردن تمام صورتحساب‌ها بر اساس Template code بدون pagination",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست صورتحساب‌ها با موفقیت برگردانده شد"
                    )
            }
    )
    @GetMapping("/{templateCode}/all")
    public ResponseEntity<List<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto>> getAllInvoicesByTemplate(
            @PathVariable
            @Schema(description = "کد الگو", example = "T01RETAIL")
            String templateCode) {
        
        var invoices = invoiceByTemplateService.loadInvoicesByTemplate(templateCode);
        
        // دریافت sts برای هر invoice
        Map<UUID, String> stsMap = invoices.stream()
            .collect(Collectors.toMap(
                ElectronicInvoiceHeaderEntity::getId,
                invoice -> systemService.findByHeaderId(invoice.getId())
                    .map(sys -> sys.getSts())
                    .orElse((String) null)
            ));
        
        var dtos = invoices.stream()
            .map(invoice -> mapper.toSummaryDto(invoice, stsMap.get(invoice.getId())))
            .toList();
        
        return ResponseEntity.ok(dtos);
    }

    @Operation(
            summary = "دریافت صورتحساب‌ها بر اساس الگو و نوع",
            description = "لود کردن لیست صورتحساب‌ها بر اساس Template code و inty",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست صورتحساب‌ها با موفقیت برگردانده شد"
                    )
            }
    )
    @GetMapping("/{templateCode}/inty/{inty}")
    public ResponseEntity<Page<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto>> getInvoicesByTemplateAndInty(
            @PathVariable
            @Schema(description = "کد الگو", example = "T01RETAIL")
            String templateCode,
            @PathVariable
            @Schema(description = "نوع صورتحساب (1-16)", example = "1")
            Integer inty,
            @RequestParam(defaultValue = "0")
            @Schema(description = "شماره صفحه", example = "0")
            Integer page,
            @RequestParam(defaultValue = "20")
            @Schema(description = "تعداد آیتم در هر صفحه", example = "20")
            Integer size,
            @RequestParam(required = false, defaultValue = "createdOn")
            @Schema(description = "فیلد برای sort", example = "createdOn")
            String sortBy,
            @RequestParam(required = false, defaultValue = "DESC")
            @Schema(description = "جهت sort", example = "DESC")
            String direction,
            jakarta.servlet.http.HttpServletRequest request) {
        
        Page<ElectronicInvoiceHeaderEntity> invoices = invoiceByTemplateService
            .loadInvoicesByTemplateAndInty(templateCode, inty, page, size, sortBy, direction);
        
        // دریافت sts برای هر invoice
        Map<UUID, String> stsMap = invoices.getContent().stream()
            .collect(Collectors.toMap(
                ElectronicInvoiceHeaderEntity::getId,
                invoice -> systemService.findByHeaderId(invoice.getId())
                    .map(sys -> sys.getSts())
                    .orElse((String) null)
            ));
        
        Page<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto> dtos = invoices.map(invoice -> 
            mapper.toSummaryDto(invoice, stsMap.get(invoice.getId()))
        );
        
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        int totalPages = dtos.getTotalPages();
        java.util.List<String> links = new java.util.ArrayList<>();
        String base = request.getRequestURI();
        java.util.function.BiFunction<Integer, Integer, String> linkOf = (p, s) -> "<" + base + "?page=" + p + "&size=" + s + ">; rel=\"%s\"";
        if (totalPages > 0) {
            links.add(String.format(linkOf.apply(0, size), "first"));
            if (page > 0) links.add(String.format(linkOf.apply(page - 1, size), "prev"));
            if (page + 1 < totalPages) links.add(String.format(linkOf.apply(page + 1, size), "next"));
            links.add(String.format(linkOf.apply(Math.max(totalPages - 1, 0), size), "last"));
        }
        if (!links.isEmpty()) headers.add(org.springframework.http.HttpHeaders.LINK, String.join(", ", links));
        headers.add("X-Total-Count", String.valueOf(dtos.getTotalElements()));
        return ResponseEntity.ok().headers(headers).body(dtos);
    }

    @Operation(
            summary = "تعداد صورتحساب‌ها بر اساس الگو",
            description = "دریافت تعداد کل صورتحساب‌های مربوط به یک الگو",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "تعداد با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = Long.class)
                            )
                    )
            }
    )
    @GetMapping("/{templateCode}/count")
    public ResponseEntity<Long> countInvoicesByTemplate(
            @PathVariable
            @Schema(description = "کد الگو", example = "T01RETAIL")
            String templateCode) {
        
        long count = invoiceByTemplateService.countInvoicesByTemplate(templateCode);
        return ResponseEntity.ok(count);
    }

    @Operation(
            summary = "Invalidate Cache برای الگو",
            description = "پاک کردن cache صورتحساب‌های یک الگو"
    )
    @DeleteMapping("/cache/{templateCode}")
    public ResponseEntity<Void> invalidateCache(
            @PathVariable
            @Schema(description = "کد الگو", example = "T01RETAIL")
            String templateCode) {
        
        invoiceByTemplateService.invalidateCache(templateCode);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Export صورتحساب‌ها به PDF بر اساس الگو",
            description = "خروجی PDF گزارش صورتحساب‌های یک الگو"
    )
    @GetMapping("/{templateCode}/export/pdf")
    public ResponseEntity<byte[]> exportToPdf(
            @PathVariable String templateCode,
            @RequestParam(required = false, defaultValue = "گزارش صورتحساب‌های الگو")
            String title) {
        
        java.util.List<ElectronicInvoiceHeaderEntity> invoices = invoiceByTemplateService
            .loadInvoicesByTemplate(templateCode);
        
        byte[] pdfContent = exportService.exportToPdf(invoices, title);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", 
            String.format("invoices_template_%s.pdf", templateCode));
        
        return ResponseEntity.ok().headers(headers).body(pdfContent);
    }

    @Operation(
            summary = "Export صورتحساب‌ها به CSV بر اساس الگو",
            description = "خروجی CSV گزارش صورتحساب‌های یک الگو"
    )
    @GetMapping(value = "/{templateCode}/export/csv", produces = "text/csv")
    public ResponseEntity<String> exportToCsv(@PathVariable String templateCode) {
        
        java.util.List<ElectronicInvoiceHeaderEntity> invoices = invoiceByTemplateService
            .loadInvoicesByTemplate(templateCode);
        
        String csvContent = exportService.exportToCsv(invoices);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", 
            String.format("invoices_template_%s.csv", templateCode));
        
        return ResponseEntity.ok().headers(headers).body(csvContent);
    }
}


*/
