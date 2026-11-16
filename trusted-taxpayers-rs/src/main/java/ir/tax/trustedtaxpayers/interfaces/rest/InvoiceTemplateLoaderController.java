package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.InvoiceTemplateLoaderService;
import ir.tax.trustedtaxpayers.beans.model.InvoiceType;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateEntity;
import ir.tax.trustedtaxpayers.interfaces.dto.ElectronicInvoiceDtos;
import ir.tax.trustedtaxpayers.interfaces.mapper.ElectronicInvoiceMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * کنترلر لودکننده الگوهای صورتحساب برای نگاشت نوع صورتحساب به قالب‌های فعال و کش مرتبط.
 * <p>
 * REST controller exposing invoice template lookups, mappings, and cache management by type or enum.
 */
@RestController
@RequestMapping("/api/invoice-templates")
@Tag(name = "Invoice Template Loader", description = "لود کردن الگوهای صورتحساب بر اساس نوع\n\nراهنمای سریع:\n- Base: /api/invoice-templates\n- بر اساس inty: GET /inty/{inty}\n- بر اساس enum: GET /type/{invoiceType}\n- الگوی خاص: GET /inty/{inty}/code/{code} | اولین: /inty/{inty}/first\n- وجود: /inty/{inty}/exists | مپینگ‌ها: /mappings | TemplateType: /inty/{inty}/template-type\n- کش: DELETE /cache/inty/{inty} | /cache/all\n")
public class InvoiceTemplateLoaderController {
    
    private final InvoiceTemplateLoaderService templateLoaderService;
    private final ElectronicInvoiceMapper mapper;
    
    public InvoiceTemplateLoaderController(
            InvoiceTemplateLoaderService templateLoaderService,
            ElectronicInvoiceMapper mapper) {
        this.templateLoaderService = templateLoaderService;
        this.mapper = mapper;
    }

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @Operation(
            summary = "دریافت الگوهای صورتحساب بر اساس نوع (inty)",
            description = "لود کردن لیست الگوهای صورتحساب فعال بر اساس نوع صورتحساب (inty: 1-16)",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست الگوها با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = ElectronicInvoiceDtos.TemplateDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "[\n" +
                                                    "  {\n" +
                                                    "    \"code\": \"T01RETAIL\",\n" +
                                                    "    \"name\": \"فروش کالا - نوع ۱\",\n" +
                                                    "    \"templateTypeCode\": \"P01\",\n" +
                                                    "    \"active\": \"Y\"\n" +
                                                    "  },\n" +
                                                    "  {\n" +
                                                    "    \"code\": \"T01WHOLE\",\n" +
                                                    "    \"name\": \"فروش کالا - نوع ۲\",\n" +
                                                    "    \"templateTypeCode\": \"P01\",\n" +
                                                    "    \"active\": \"Y\"\n" +
                                                    "  }\n" +
                                                    "]"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "نوع صورتحساب نامعتبر",
                            content = @Content(schema = @Schema())
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "الگویی برای این نوع یافت نشد",
                            content = @Content(schema = @Schema())
                    )
            }
    )
    @GetMapping("/inty/{inty}")
    public ResponseEntity<List<ElectronicInvoiceDtos.TemplateDto>> getTemplatesByInty(
            @PathVariable
            @Schema(description = "نوع صورتحساب (1-16)", example = "1", required = true)
            Integer inty) {
        
        if (inty == null || inty < 1 || inty > 16) {
            return ResponseEntity.badRequest().build();
        }
        
        List<TemplateEntity> templates = templateLoaderService.loadTemplatesByInty(inty);
        
        if (templates.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        List<ElectronicInvoiceDtos.TemplateDto> dtos = templates.stream()
                .map(mapper::toTemplateDto)
                .toList();
        
        return ResponseEntity.ok(dtos);
    }

    @Operation(
            summary = "دریافت الگوهای صورتحساب بر اساس InvoiceType enum",
            description = "لود کردن لیست الگوهای صورتحساب فعال بر اساس InvoiceType enum",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست الگوها با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = ElectronicInvoiceDtos.TemplateDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "نوع صورتحساب نامعتبر",
                            content = @Content(schema = @Schema())
                    )
            }
    )
    @GetMapping("/type/{invoiceType}")
    public ResponseEntity<List<ElectronicInvoiceDtos.TemplateDto>> getTemplatesByInvoiceType(
            @PathVariable
            @Schema(description = "نوع صورتحساب (TYPE1_SALES, TYPE2_FX, ...)", 
                    example = "TYPE1_SALES", required = true)
            String invoiceType) {
        
        try {
            InvoiceType type = InvoiceType.valueOf(invoiceType.toUpperCase());
            List<TemplateEntity> templates = templateLoaderService.loadTemplatesByInvoiceType(type);
            
            List<ElectronicInvoiceDtos.TemplateDto> dtos = templates.stream()
                    .map(mapper::toTemplateDto)
                    .toList();
            
            return ResponseEntity.ok(dtos);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @Operation(
            summary = "دریافت یک الگوی خاص",
            description = "لود کردن یک الگوی خاص بر اساس نوع صورتحساب و کد الگو",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "الگو با موفقیت یافت شد",
                            content = @Content(
                                    schema = @Schema(implementation = ElectronicInvoiceDtos.TemplateDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "الگو یافت نشد",
                            content = @Content(schema = @Schema())
                    )
            }
    )
    @GetMapping("/inty/{inty}/code/{code}")
    public ResponseEntity<ElectronicInvoiceDtos.TemplateDto> getTemplateByIntyAndCode(
            @PathVariable
            @Schema(description = "نوع صورتحساب (1-16)", example = "1")
            Integer inty,
            @PathVariable
            @Schema(description = "کد الگو", example = "T01RETAIL")
            String code) {
        
        if (inty == null || inty < 1 || inty > 16 || code == null || code.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        
        Optional<TemplateEntity> template = templateLoaderService
                .loadTemplateByIntyAndCode(inty, code);
        
        return template.map(entity -> ResponseEntity.ok(mapper.toTemplateDto(entity)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "دریافت اولین الگوی فعال",
            description = "لود کردن اولین الگوی فعال برای یک نوع صورتحساب",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "الگو با موفقیت یافت شد",
                            content = @Content(
                                    schema = @Schema(implementation = ElectronicInvoiceDtos.TemplateDto.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "الگویی یافت نشد",
                            content = @Content(schema = @Schema())
                    )
            }
    )
    @GetMapping("/inty/{inty}/first")
    public ResponseEntity<ElectronicInvoiceDtos.TemplateDto> getFirstTemplateByInty(
            @PathVariable
            @Schema(description = "نوع صورتحساب (1-16)", example = "1")
            Integer inty) {
        
        if (inty == null || inty < 1 || inty > 16) {
            return ResponseEntity.badRequest().build();
        }
        
        Optional<TemplateEntity> template = templateLoaderService.loadFirstTemplateByInty(inty);
        
        return template.map(entity -> ResponseEntity.ok(mapper.toTemplateDto(entity)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "بررسی وجود الگو برای نوع صورتحساب",
            description = "بررسی اینکه آیا الگوی فعالی برای یک نوع صورتحساب وجود دارد یا نه",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "نتیجه بررسی",
                            content = @Content(
                                    schema = @Schema(implementation = Boolean.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ",
                                            value = "true"
                                    )
                            )
                    )
            }
    )
    @GetMapping("/inty/{inty}/exists")
    public ResponseEntity<Boolean> hasTemplatesForInty(
            @PathVariable
            @Schema(description = "نوع صورتحساب (1-16)", example = "1")
            Integer inty) {
        
        if (inty == null || inty < 1 || inty > 16) {
            return ResponseEntity.badRequest().build();
        }
        
        boolean exists = templateLoaderService.hasTemplatesForInty(inty);
        return ResponseEntity.ok(exists);
    }

    @Operation(
            summary = "دریافت Mapping بین InvoiceType و TemplateType",
            description = "دریافت تمام mapping های بین inty و TemplateType code",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Mapping ها با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = Map.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ",
                                            value = "{\n" +
                                                    "  \"1\": \"P01\",\n" +
                                                    "  \"2\": \"P02\",\n" +
                                                    "  \"3\": \"P03\"\n" +
                                                    "}"
                                    )
                            )
                    )
            }
    )
    @GetMapping("/mappings")
    public ResponseEntity<Map<Integer, String>> getAllMappings() {
        Map<Integer, String> mappings = templateLoaderService.getAllMappings();
        return ResponseEntity.ok(mappings);
    }

    @Operation(
            summary = "دریافت TemplateType code بر اساس inty",
            description = "دریافت کد TemplateType مربوط به یک نوع صورتحساب",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "کد TemplateType برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = String.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ",
                                            value = "P01"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Mapping یافت نشد",
                            content = @Content(schema = @Schema())
                    )
            }
    )
    @GetMapping("/inty/{inty}/template-type")
    public ResponseEntity<String> getTemplateTypeCodeByInty(
            @PathVariable
            @Schema(description = "نوع صورتحساب (1-16)", example = "1")
            Integer inty) {
        
        if (inty == null || inty < 1 || inty > 16) {
            return ResponseEntity.badRequest().build();
        }
        
        String templateTypeCode = templateLoaderService.getTemplateTypeCode(inty);
        
        if (templateTypeCode == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(templateTypeCode);
    }

    @Operation(
            summary = "Invalidate Cache برای یک نوع صورتحساب",
            description = "پاک کردن cache الگوهای یک نوع صورتحساب",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Cache با موفقیت invalidate شد"
                    )
            }
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/cache/inty/{inty}")
    public ResponseEntity<Void> invalidateCacheByInty(
            @PathVariable
            @Schema(description = "نوع صورتحساب (1-16)", example = "1")
            Integer inty) {
        
        if (inty == null || inty < 1 || inty > 16) {
            return ResponseEntity.badRequest().build();
        }
        
        templateLoaderService.invalidateCache(inty);
        return ResponseEntity.ok().build();
    }

    @Operation(
            summary = "Invalidate تمام Cache ها",
            description = "پاک کردن تمام cache های الگوها",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "تمام cache ها با موفقیت invalidate شدند"
                    )
            }
    )
    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/cache/all")
    public ResponseEntity<Void> invalidateAllCache() {
        templateLoaderService.invalidateAllCache();
        return ResponseEntity.ok().build();
    }
}


