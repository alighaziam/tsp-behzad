package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.ElectronicInvoiceSystemService;
import ir.tax.trustedtaxpayers.application.service.InvoiceByTemplateTypeService;
import ir.tax.trustedtaxpayers.application.service.InvoiceExportService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.interfaces.dto.ElectronicInvoiceDtos;
import ir.tax.trustedtaxpayers.interfaces.mapper.ElectronicInvoiceMapper;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * کنترلر مدیریت صورتحساب‌ها بر اساس نوع الگو برای گزارش‌گیری و خروجی گرفتن.
 * <p>
 * REST controller that retrieves, counts, exports, and manages cache for invoices grouped by template type.
 */
@RestController
@RequestMapping("/api/invoices/by-template-type")
@Tag(name = "Invoices by Template Type", description = "لود کردن صورتحساب‌ها بر اساس نوع الگو\n\nراهنمای سریع:\n- Base: /api/invoices/by-template-type\n- صفحه‌بندی: GET /{templateTypeCode}?page=0&size=20&sortBy=createdOn&direction=DESC\n- همه: GET /{templateTypeCode}/all | تعداد: /{templateTypeCode}/count\n- خروجی: /{templateTypeCode}/export/pdf | /export/csv\n- کش: DELETE /cache/{templateTypeCode}\n")
public class InvoiceByTemplateTypeController {
    
    private final InvoiceByTemplateTypeService invoiceByTemplateTypeService;
    private final ElectronicInvoiceMapper mapper;
    private final ElectronicInvoiceSystemService systemService;
    private final InvoiceExportService exportService;
    
    public InvoiceByTemplateTypeController(
            InvoiceByTemplateTypeService invoiceByTemplateTypeService,
            ElectronicInvoiceMapper mapper,
            ElectronicInvoiceSystemService systemService,
            InvoiceExportService exportService) {
        this.invoiceByTemplateTypeService = invoiceByTemplateTypeService;
        this.mapper = mapper;
        this.systemService = systemService;
        this.exportService = exportService;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @Operation(
            summary = "دریافت صورتحساب‌ها بر اساس نوع الگو (با Pagination)",
            description = "لود کردن لیست صورتحساب‌ها بر اساس TemplateType code با پشتیبانی از pagination و sorting",
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
    @GetMapping("/{templateTypeCode}")
    public ResponseEntity<Page<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto>> getInvoicesByTemplateType(
            @PathVariable
            @Schema(description = "کد نوع الگو (مثل P01, P02, ...)", example = "P01")
            String templateTypeCode,
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
        
        Page<ElectronicInvoiceHeaderEntity> invoices = invoiceByTemplateTypeService
            .loadInvoicesByTemplateType(templateTypeCode, page, size, sortBy, direction);
        
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
            summary = "دریافت تمام صورتحساب‌ها بر اساس نوع الگو (بدون Pagination)",
            description = "لود کردن تمام صورتحساب‌ها بر اساس TemplateType code بدون pagination",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست صورتحساب‌ها با موفقیت برگردانده شد"
                    )
            }
    )
    @GetMapping("/{templateTypeCode}/all")
    public ResponseEntity<java.util.List<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto>> getAllInvoicesByTemplateType(
            @PathVariable
            @Schema(description = "کد نوع الگو", example = "P01")
            String templateTypeCode) {
        
        var invoices = invoiceByTemplateTypeService.loadInvoicesByTemplateType(templateTypeCode);
        
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

    /*@Operation(
            summary = "تعداد صورتحساب‌ها بر اساس نوع الگو",
            description = "دریافت تعداد کل صورتحساب‌های مربوط به یک نوع الگو",
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
    @GetMapping("/{templateTypeCode}/count")
    public ResponseEntity<Long> countInvoicesByTemplateType(
            @PathVariable
            @Schema(description = "کد نوع الگو", example = "P01")
            String templateTypeCode) {
        
        long count = invoiceByTemplateTypeService.countInvoicesByTemplateType(templateTypeCode);
        return ResponseEntity.ok(count);
    }*/

    @Operation(
            summary = "Invalidate Cache برای نوع الگو",
            description = "پاک کردن cache صورتحساب‌های یک نوع الگو"
    )
    @DeleteMapping("/cache/{templateTypeCode}")
    public ResponseEntity<Void> invalidateCache(
            @PathVariable
            @Schema(description = "کد نوع الگو", example = "P01")
            String templateTypeCode) {
        
        invoiceByTemplateTypeService.invalidateCache(templateTypeCode);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Export صورتحساب‌ها به PDF بر اساس نوع الگو",
            description = "خروجی PDF گزارش صورتحساب‌های یک نوع الگو"
    )
    @GetMapping("/{templateTypeCode}/export/pdf")
    public ResponseEntity<byte[]> exportToPdf(
            @PathVariable String templateTypeCode,
            @RequestParam(required = false, defaultValue = "گزارش صورتحساب‌های نوع الگو")
            String title) {
        
        java.util.List<ElectronicInvoiceHeaderEntity> invoices = invoiceByTemplateTypeService
            .loadInvoicesByTemplateType(templateTypeCode);
        
        byte[] pdfContent = exportService.exportToPdf(invoices, title);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", 
            String.format("invoices_template_type_%s.pdf", templateTypeCode));
        
        return ResponseEntity.ok().headers(headers).body(pdfContent);
    }

    @Operation(
            summary = "Export صورتحساب‌ها به CSV بر اساس نوع الگو",
            description = "خروجی CSV گزارش صورتحساب‌های یک نوع الگو"
    )
    @GetMapping(value = "/{templateTypeCode}/export/csv", produces = "text/csv")
    public ResponseEntity<String> exportToCsv(@PathVariable String templateTypeCode) {
        
        java.util.List<ElectronicInvoiceHeaderEntity> invoices = invoiceByTemplateTypeService
            .loadInvoicesByTemplateType(templateTypeCode);
        
        String csvContent = exportService.exportToCsv(invoices);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", 
            String.format("invoices_template_type_%s.csv", templateTypeCode));
        
        return ResponseEntity.ok().headers(headers).body(csvContent);
    }
}


