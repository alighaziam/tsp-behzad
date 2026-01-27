/*
package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.*;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ErrorResponse;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceHeaderRepository;
import ir.tax.trustedtaxpayers.core.spec.FilterRequest;
import ir.tax.trustedtaxpayers.interfaces.dto.ElectronicInvoiceDtos;
import ir.tax.trustedtaxpayers.interfaces.mapper.ElectronicInvoiceMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

*/
/**
 * کنترلر مدیریت سربرگ صورتحساب‌های الکترونیکی برای بازیابی، فیلتر و نمایش جزئیات کامل.
 * <p>
 * REST controller exposing CRUD-style endpoints and advanced search for electronic invoice headers.
 *//*

@RestController
@RequestMapping("/api/electronic-invoices")
@Tag(name = "Electronic Invoice Management", description = "مدیریت صورتحساب‌های الکترونیکی مالیاتی\n\nراهنمای سریع:\n- Base: /api/electronic-invoices\n- همه: /all | بر اساس TAX_ID: /by-tax-id/{taxId}\n- فروشنده/خریدار (page): /by-seller/{tins} | /by-buyer/{tinb}\n- قالب و نوع (page): /by-template/{templateCode}/type/{inty}\n- جستجوی سفارشی (page): POST /search-custom?page=0&size=10 → [{key,operand,value}]\n")
public class ElectronicInvoiceHeaderController extends BaseController<ElectronicInvoiceHeaderEntity, UUID> {
    private final ElectronicInvoiceHeaderService headerService;
    private final ElectronicInvoiceHeaderRepository headerRepository;
    private final ElectronicInvoiceMapper mapper;
    private final ElectronicInvoiceItemService itemService;
    private final ElectronicInvoicePaymentService paymentService;
    private final ElectronicInvoiceSystemService systemService;

    public ElectronicInvoiceHeaderController(
            ElectronicInvoiceHeaderService headerService,
            ElectronicInvoiceHeaderRepository headerRepository,
            ElectronicInvoiceMapper mapper,
            ElectronicInvoiceItemService itemService,
            ElectronicInvoicePaymentService paymentService,
            ElectronicInvoiceSystemService systemService) {
        super(headerService, headerRepository);
        this.headerService = headerService;
        this.headerRepository = headerRepository;
        this.mapper = mapper;
        this.itemService = itemService;
        this.paymentService = paymentService;
        this.systemService = systemService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<ElectronicInvoiceHeaderEntity>> getAll() {
        return ResponseEntity.ok(headerService.findAll());
    }

    */
/**
     * جستجوی صورتحساب بر اساس شماره مالیاتی
     * 
     * <p>این متد صورتحساب الکترونیکی را بر اساس شماره منحصر به فرد مالیاتی (TAX_ID) جستجو می‌کند
     * و تمام اطلاعات مربوط شامل اقلام، پرداخت‌ها و اطلاعات سیستمی را برمی‌گرداند.</p>
     * 
     * @param taxId شماره منحصر به فرد مالیاتی (22 کاراکتر)
     * @return جزئیات کامل صورتحساب شامل اقلام و پرداخت‌ها
     * @throws ResourceNotFoundException در صورت عدم یافتن صورتحساب
     *//*

    @Operation(
            summary = "جستجوی صورتحساب بر اساس شماره مالیاتی",
            description = "دریافت جزئیات کامل صورتحساب الکترونیکی بر اساس شماره منحصر به فرد مالیاتی (TAX_ID). " +
                    "شماره مالیاتی باید دقیقاً 22 کاراکتر باشد.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "صورتحساب با موفقیت یافت شد",
                            content = @Content(
                                    schema = @Schema(implementation = ElectronicInvoiceDtos.ElectronicInvoiceHeaderDetailDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "{\n" +
                                                    "  \"id\": \"123e4567-e89b-12d3-a456-426614174000\",\n" +
                                                    "  \"taxId\": \"1234567890123456789012\",\n" +
                                                    "  \"templateCode\": \"TEMPLATE_01\",\n" +
                                                    "  \"inty\": 1,\n" +
                                                    "  \"inno\": \"A1B2C3D4E5\",\n" +
                                                    "  \"indatim\": 1704067200000,\n" +
                                                    "  \"tins\": \"12345678901\",\n" +
                                                    "  \"tinb\": \"98765432109\",\n" +
                                                    "  \"tbill\": 1000000.00,\n" +
                                                    "  \"items\": [\n" +
                                                    "    {\n" +
                                                    "      \"id\": \"item-uuid\",\n" +
                                                    "      \"sstid\": \"1234567890123\",\n" +
                                                    "      \"sstt\": \"کالای نمونه\",\n" +
                                                    "      \"am\": 10.00,\n" +
                                                    "      \"fee\": 100000.00\n" +
                                                    "    }\n" +
                                                    "  ]\n" +
                                                    "}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "صورتحساب یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @GetMapping("/by-tax-id/{taxId}")
    public ResponseEntity<ElectronicInvoiceDtos.ElectronicInvoiceHeaderDetailDto> findByTaxId(
            @PathVariable 
            @Schema(description = "شماره منحصر به فرد مالیاتی (22 کاراکتر)", example = "1234567890123456789012", required = true)
            String taxId) {
        var header = headerService.findByTaxId(taxId)
                .orElseThrow(() -> new ir.tax.trustedtaxpayers.application.exception.ResourceNotFoundException("Invoice with TAX_ID " + taxId + " not found"));
        
        var items = itemService.findByHeaderId(header.getId()).stream()
                .map(mapper::toItemDto)
                .collect(Collectors.toList());
        
        var payments = paymentService.findByHeaderId(header.getId()).stream()
                .map(mapper::toPaymentDto)
                .collect(Collectors.toList());
        
        var systemDto = systemService.findByHeaderId(header.getId())
                .map(mapper::toSystemDto)
                .orElse(null);
        
        return ResponseEntity.ok(mapper.toDetailDto(header, items, payments, systemDto));
    }

    @Operation(
            summary = "جستجوی صورتحساب‌ها بر اساس شماره مالیاتی فروشنده",
            description = "دریافت لیست صورتحساب‌های الکترونیکی بر اساس شماره مالیاتی فروشنده (TINS) با امکان صفحه‌بندی",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست صورتحساب‌ها با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "شماره مالیاتی نامعتبر است",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @GetMapping("/by-seller/{tins}")
    public ResponseEntity<List<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto>> findBySeller(
            @PathVariable
            @Schema(description = "شماره مالیاتی فروشنده", example = "12345678901", required = true)
            String tins,
            @RequestParam(defaultValue = "0")
            @Schema(description = "شماره صفحه (شروع از 0)", example = "0")
            int page,
            @RequestParam(defaultValue = "10")
            @Schema(description = "تعداد رکوردها در هر صفحه", example = "10")
            int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ElectronicInvoiceHeaderEntity> result = headerService.findBySellerTin(tins, pageable);
        return ResponseEntity.ok(result.getContent().stream()
                .map(header -> {
                    String sts = systemService.findByHeaderId(header.getId())
                            .map(sys -> sys.getSts())
                            .orElse(null);
                    return mapper.toSummaryDto(header, sts);
                })
                .collect(Collectors.toList()));
    }

    @Operation(
            summary = "جستجوی صورتحساب‌ها بر اساس شماره مالیاتی خریدار",
            description = "دریافت لیست صورتحساب‌های الکترونیکی بر اساس شماره مالیاتی خریدار (TINB) با امکان صفحه‌بندی",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست صورتحساب‌ها با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "شماره مالیاتی نامعتبر است",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @GetMapping("/by-buyer/{tinb}")
    public ResponseEntity<List<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto>> findByBuyer(
            @PathVariable
            @Schema(description = "شماره مالیاتی خریدار", example = "98765432109", required = true)
            String tinb,
            @RequestParam(defaultValue = "0")
            @Schema(description = "شماره صفحه (شروع از 0)", example = "0")
            int page,
            @RequestParam(defaultValue = "10")
            @Schema(description = "تعداد رکوردها در هر صفحه", example = "10")
            int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ElectronicInvoiceHeaderEntity> result = headerService.findByBuyerTin(tinb, pageable);
        return ResponseEntity.ok(result.getContent().stream()
                .map(header -> {
                    String sts = systemService.findByHeaderId(header.getId())
                            .map(sys -> sys.getSts())
                            .orElse(null);
                    return mapper.toSummaryDto(header, sts);
                })
                .collect(Collectors.toList()));
    }

    @Operation(
            summary = "جستجوی صورتحساب‌ها بر اساس قالب و نوع",
            description = "دریافت لیست صورتحساب‌های الکترونیکی بر اساس کد قالب و نوع صورتحساب (inty) با امکان صفحه‌بندی",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست صورتحساب‌ها با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "کد قالب یا نوع صورتحساب نامعتبر است",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "قالب یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @GetMapping("/by-template/{templateCode}/type/{inty}")
    public ResponseEntity<List<ElectronicInvoiceDtos.ElectronicInvoiceHeaderSummaryDto>> findByTemplateAndType(
            @PathVariable
            @Schema(description = "کد قالب", example = "TEMPLATE_01", required = true)
            String templateCode,
            @PathVariable
            @Schema(description = "نوع صورتحساب (1=فروش, 2=خرید, 3=برگشت از فروش)", example = "1", required = true)
            Integer inty,
            @RequestParam(defaultValue = "0")
            @Schema(description = "شماره صفحه (شروع از 0)", example = "0")
            int page,
            @RequestParam(defaultValue = "10")
            @Schema(description = "تعداد رکوردها در هر صفحه", example = "10")
            int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ElectronicInvoiceHeaderEntity> result = headerService.findByTemplateAndInty(templateCode, inty, pageable);
        return ResponseEntity.ok(result.getContent().stream()
                .map(header -> {
                    String sts = systemService.findByHeaderId(header.getId())
                            .map(sys -> sys.getSts())
                            .orElse(null);
                    return mapper.toSummaryDto(header, sts);
                })
                .collect(Collectors.toList()));
    }

    @io.swagger.v3.oas.annotations.Operation(
            summary = "جستجوی پیشرفته صورتحساب‌ها (سفارشی)",
            description = "Body می‌تواند شیء واحد یا آرایه‌ای از فیلترها باشد.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = FilterRequest.class),
                            examples = {
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "یک فیلتر (جستجو بر اساس TAX_ID)",
                                            value = "{\n  \"key\": \"taxId\",\n  \"operand\": \"=\",\n  \"value\": \"1234567890123456789012\"\n}"
                                    ),
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "چند فیلتر (Template و نوع)",
                                            value = "[\n  {\n    \"key\": \"templateCode\",\n    \"operand\": \"=\",\n    \"value\": \"TEMPLATE_01\"\n  },\n  {\n    \"key\": \"inty\",\n    \"operand\": \">=\",\n    \"value\": 1\n  }\n]"
                                    ),
                                    @io.swagger.v3.oas.annotations.media.ExampleObject(
                                            name = "فیلتر بر اساس فروشنده/خریدار",
                                            value = "[\n  {\n    \"key\": \"tins\",\n    \"operand\": \"=\",\n    \"value\": \"12345678901\"\n  },\n  {\n    \"key\": \"tinb\",\n    \"operand\": \"=\",\n    \"value\": \"98765432109\"\n  }\n]"
                                    )
                            }
                    )
            )
    )
    @PostMapping(value = "/search-custom", consumes = org.springframework.http.MediaType.APPLICATION_JSON_VALUE, produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    public org.springframework.http.ResponseEntity<org.springframework.data.domain.Page<ElectronicInvoiceHeaderEntity>> searchCustom(
            @org.springframework.web.bind.annotation.RequestBody java.util.List<FilterRequest> filters,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "10") int size,
            jakarta.servlet.http.HttpServletRequest request) {
        // استفاده از پیاده‌سازی والد
        return (org.springframework.http.ResponseEntity) super.search(filters, page, size, request);
    }
}


*/
