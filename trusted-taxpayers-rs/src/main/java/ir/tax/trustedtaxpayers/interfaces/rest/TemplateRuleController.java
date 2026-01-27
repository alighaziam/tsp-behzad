package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.TemplateRuleService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateRuleEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateRuleRepository;
import ir.tax.trustedtaxpayers.interfaces.dto.ElectronicInvoiceDtos;
import ir.tax.trustedtaxpayers.interfaces.mapper.ElectronicInvoiceMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * کنترلر مدیریت قواعد قالب برای خدمت‌رسانی به اعتبارسنجی و تنظیمات فرم.
 * <p>
 * REST controller exposing invoice template rules for listing, filtering, and DTO conversion.
 */
@RestController
@RequestMapping({"/api/template-rules", "/api/v1/template-rules"})
@Tag(name = "Template Rule Management", description = "مدیریت قواعد قالب‌های صورتحساب\n\nراهنمای سریع:\n- Base: /api/template-rules\n- همه: GET /all\n- قواعد یک قالب: GET /template/{templateCode}\n- قواعد فعال یک قالب: GET /template/{templateCode}/active\n- قواعد یک فیلد: GET /field/{fieldName}\n")
public class TemplateRuleController extends BaseController<TemplateRuleEntity, UUID> {
    private final TemplateRuleService templateRuleService;
    private final TemplateRuleRepository templateRuleRepository;
    private final ElectronicInvoiceMapper mapper;

    public TemplateRuleController(
            TemplateRuleService templateRuleService,
            TemplateRuleRepository templateRuleRepository,
            ElectronicInvoiceMapper mapper) {
        super(templateRuleService, templateRuleRepository);
        this.templateRuleService = templateRuleService;
        this.templateRuleRepository = templateRuleRepository;
        this.mapper = mapper;
    }

    @GetMapping("/all")
    public ResponseEntity<List<TemplateRuleEntity>> getAll() {
        return ResponseEntity.ok(templateRuleService.findAll());
    }

    @Operation(
            summary = "دریافت قواعد یک قالب",
            description = "دریافت لیست تمام قواعد مرتبط با یک قالب خاص",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست قواعد با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = ElectronicInvoiceDtos.TemplateRuleDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "[\n" +
                                                    "  {\n" +
                                                    "    \"id\": \"123e4567-e89b-12d3-a456-426614174000\",\n" +
                                                    "    \"templateCode\": \"TEMPLATE_01\",\n" +
                                                    "    \"fieldName\": \"taxId\",\n" +
                                                    "    \"ruleType\": \"REQUIRED\",\n" +
                                                    "    \"ruleParams\": null,\n" +
                                                    "    \"explanation\": \"شناسه مالیاتی اجباری است\",\n" +
                                                    "    \"active\": \"Y\"\n" +
                                                    "  },\n" +
                                                    "  {\n" +
                                                    "    \"id\": \"123e4567-e89b-12d3-a456-426614174001\",\n" +
                                                    "    \"templateCode\": \"TEMPLATE_01\",\n" +
                                                    "    \"fieldName\": \"tinb\",\n" +
                                                    "    \"ruleType\": \"LENGTH\",\n" +
                                                    "    \"ruleParams\": \"11-14\",\n" +
                                                    "    \"explanation\": \"شماره اقتصادی باید بین 11 تا 14 رقم باشد\",\n" +
                                                    "    \"active\": \"Y\"\n" +
                                                    "  }\n" +
                                                    "]"
                                    )
                            )
                    )
            }
    )
    @GetMapping("/template/{templateCode}")
    public ResponseEntity<List<ElectronicInvoiceDtos.TemplateRuleDto>> findByTemplateCode(
            @PathVariable
            @Schema(description = "کد قالب", example = "TEMPLATE_01")
            String templateCode) {
        var rules = templateRuleService.findByTemplateCode(templateCode);
        return ResponseEntity.ok(rules.stream().map(mapper::toTemplateRuleDto).toList());
    }

    @Operation(
            summary = "دریافت قواعد فعال یک قالب",
            description = "دریافت لیست قواعد فعال مرتبط با یک قالب خاص",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست قواعد فعال با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = ElectronicInvoiceDtos.TemplateRuleDto.class)
                            )
                    )
            }
    )
    @GetMapping("/template/{templateCode}/active")
    public ResponseEntity<List<ElectronicInvoiceDtos.TemplateRuleDto>> findByTemplateCodeAndActive(
            @PathVariable
            @Schema(description = "کد قالب", example = "TEMPLATE_01")
            String templateCode) {
        var rules = templateRuleService.findByTemplateCodeAndActive(templateCode, "Y");
        return ResponseEntity.ok(rules.stream().map(mapper::toTemplateRuleDto).toList());
    }

    @Operation(
            summary = "دریافت قواعد یک فیلد",
            description = "دریافت لیست تمام قواعد مرتبط با یک فیلد خاص",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست قواعد با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = ElectronicInvoiceDtos.TemplateRuleDto.class)
                            )
                    )
            }
    )
    @GetMapping("/field/{fieldName}")
    public ResponseEntity<List<ElectronicInvoiceDtos.TemplateRuleDto>> findByFieldName(
            @PathVariable
            @Schema(description = "نام فیلد", example = "taxId")
            String fieldName) {
        var rules = templateRuleService.findByFieldName(fieldName);
        return ResponseEntity.ok(rules.stream().map(mapper::toTemplateRuleDto).toList());
    }
}


