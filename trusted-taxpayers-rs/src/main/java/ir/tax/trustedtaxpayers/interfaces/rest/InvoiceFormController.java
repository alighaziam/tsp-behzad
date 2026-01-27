package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.dto.FormFieldDto;
import ir.tax.trustedtaxpayers.application.dto.FormSchemaDto;
import ir.tax.trustedtaxpayers.application.service.InvoiceFormBuilderService;
import ir.tax.trustedtaxpayers.application.service.TemplateService;
import ir.tax.trustedtaxpayers.biz.service.TemplateTemplateFieldService;
import ir.tax.trustedtaxpayers.enums.FieldRequirementType;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateFieldEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTemplateFieldEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateFieldRepository;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * کنترلر تولید فرم‌های صورتحساب برای ارائه ساختار فیلدها بر اساس الگوهای تعریف‌شده.
 * <p>
 * REST controller delivering invoice form schemas and pattern metadata for dynamic form builders.
 */
@RestController
@RequestMapping("/api/invoice-forms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Invoice Form Builder", description = "سرویس تولید فرم‌های صورتحساب بر اساس 16 الگو\n\nراهنمای سریع:\n- Base: /api/invoice-forms\n- دریافت فرم الگو: GET /pattern/{patternNumber}\n- لیست الگوها: GET /patterns\n- همراه با احراز هویت Bearer\n")
public class InvoiceFormController {

    private final InvoiceFormBuilderService invoiceFormBuilderService;
    private final TemplateService templateService;
    private final TemplateTemplateFieldService templateTemplateFieldService;
    private final TemplateFieldRepository templateFieldRepository;

    @Operation(
            summary = "دریافت فرم صورتحساب بر اساس شماره الگو",
            description = "این endpoint با دریافت شماره الگو (1-16)، فرم مناسب با فیلدهای مورد نیاز آن الگو را برمی‌گرداند",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "فرم با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = FormSchemaDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه فرم الگوی 1",
                                            value = "{\n" +
                                                    "  \"entityName\": \"InvoicePattern1\",\n" +
                                                    "  \"title\": \"فرم صورتحساب الگوی 1 - صورتحساب الکترونیکی نوع اول\",\n" +
                                                    "  \"description\": \"فرم ایجاد صورتحساب الکترونیکی الگوی 1\",\n" +
                                                    "  \"fields\": [\n" +
                                                    "    {\n" +
                                                    "      \"key\": \"inno\",\n" +
                                                    "      \"type\": \"text\",\n" +
                                                    "      \"name\": \"شماره صورتحساب\",\n" +
                                                    "      \"label\": \"شماره صورتحساب\",\n" +
                                                    "      \"required\": true,\n" +
                                                    "      \"mandatory\": true,\n" +
                                                    "      \"minLength\": 1,\n" +
                                                    "      \"maxLength\": 10,\n" +
                                                    "      \"pattern\": \"^[A-Za-z0-9-]{1,10}$\",\n" +
                                                    "      \"placeholder\": \"لطفا شماره صورتحساب را وارد کنید\",\n" +
                                                    "      \"order\": 0,\n" +
                                                    "      \"disabled\": false,\n" +
                                                    "      \"readonly\": false\n" +
                                                    "    },\n" +
                                                    "    {\n" +
                                                    "      \"key\": \"indatim\",\n" +
                                                    "      \"type\": \"datetime\",\n" +
                                                    "      \"name\": \"تاریخ صورتحساب\",\n" +
                                                    "      \"label\": \"تاریخ صورتحساب\",\n" +
                                                    "      \"required\": true,\n" +
                                                    "      \"mandatory\": true,\n" +
                                                    "      \"placeholder\": \"لطفا تاریخ صورتحساب را وارد کنید\",\n" +
                                                    "      \"order\": 1,\n" +
                                                    "      \"disabled\": false,\n" +
                                                    "      \"readonly\": false\n" +
                                                    "    },\n" +
                                                    "    {\n" +
                                                    "      \"key\": \"tins\",\n" +
                                                    "      \"type\": \"text\",\n" +
                                                    "      \"name\": \"شناسه ملی/اقتصادی فروشنده\",\n" +
                                                    "      \"label\": \"شناسه ملی/اقتصادی فروشنده\",\n" +
                                                    "      \"required\": true,\n" +
                                                    "      \"mandatory\": true,\n" +
                                                    "      \"minLength\": 10,\n" +
                                                    "      \"maxLength\": 14,\n" +
                                                    "      \"pattern\": \"^\\\\d{10,14}$\",\n" +
                                                    "      \"order\": 2\n" +
                                                    "    }\n" +
                                                    "  ]\n" +
                                                    "}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "شماره الگو نامعتبر است (باید بین 1 تا 16 باشد)",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "خطا در تولید فرم",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @GetMapping("/pattern/{patternNumber}")
    public ResponseEntity<FormSchemaDto> getInvoiceFormByPattern(
            @PathVariable("patternNumber") @Parameter(description = "شماره الگو (1-16)", example = "1") Integer patternNumber,
            @RequestParam(value = "invoiceType", required = false, defaultValue = "1") 
            @Parameter(description = "نوع صورتحساب (1=نوع اول, 2=نوع دوم)", example = "1") Integer invoiceType) {
        try {
            // استفاده از template-based approach
            String templateCode = buildTemplateCode(patternNumber, invoiceType);
            if (templateCode != null) {
                ResponseEntity<FormSchemaDto> templateResponse = getFormSchemaByTemplateCode(templateCode);
                if (templateResponse.getStatusCode().is2xxSuccessful() && templateResponse.getBody() != null) {
                    return templateResponse;
                }
            }
            
            // Fallback: استفاده از InvoiceFormBuilderService در صورت عدم وجود template
            log.warn("Template برای pattern {} و invoiceType {} یافت نشد، استفاده از فیلدهای پیش‌فرض", patternNumber, invoiceType);
            FormSchemaDto schema = invoiceFormBuilderService.generateInvoiceFormSchema(patternNumber);
            return ResponseEntity.ok(schema);
        } catch (IllegalArgumentException e) {
            log.warn("شماره الگوی نامعتبر: {}", patternNumber);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("خطا در تولید فرم الگوی {}: {}", patternNumber, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * ساخت template code از pattern number و invoice type
     */
    private String buildTemplateCode(Integer patternNumber, Integer invoiceType) {
        if (patternNumber == null || invoiceType == null) {
            return null;
        }
        
        String typeSuffix = invoiceType == 1 ? "TYPE1" : "TYPE2";
        
        // Mapping pattern number به template code prefix
        Map<Integer, String> patternToCodeMap = Map.ofEntries(
            Map.entry(1, "SALE_TEMPLATE_1"),
            Map.entry(2, "CURRENCY_SALE_TEMPLATE"),
            Map.entry(3, "GOLD_JEWELRY_TEMPLATE"),
            Map.entry(4, "CONTRACT_TEMPLATE"),
            Map.entry(5, "SERVICE_BILL_TEMPLATE"),
            Map.entry(6, "AIRLINE_TICKET_TEMPLATE"),
            Map.entry(7, "EXPORT_TEMPLATE"),
            Map.entry(8, "WAYBILL_TEMPLATE"),
            Map.entry(9, "PETROLEUM_PRODUCT_TEMPLATE"),
            Map.entry(11, "COMMODITY_EXCHANGE_TEMPLATE"),
            Map.entry(13, "INSURANCE_SERVICE_TEMPLATE")
        );
        
        String codePrefix = patternToCodeMap.get(patternNumber);
        if (codePrefix == null) {
            return null;
        }
        
        String templateCode = codePrefix + "_" + typeSuffix;
        
        // بررسی وجود template در دیتابیس
        if (templateService.findByCode(templateCode).isPresent()) {
            return templateCode;
        }
        
        // اگر TYPE2 وجود نداشت، TYPE1 را برگردان (fallback)
        if (invoiceType == 2) {
            String type1Code = codePrefix + "_TYPE1";
            if (templateService.findByCode(type1Code).isPresent()) {
                return type1Code;
            }
        }
        
        return null;
    }

    private ResponseEntity<FormSchemaDto> getFormSchemaByTemplateCode(String templateCode) {
        return templateService.findByCode(templateCode)
                .map(template -> {
                    List<TemplateTemplateFieldEntity> templateFields = templateTemplateFieldService.getTemplateFieldsByTemplateId(template.getId());
                    List<UUID> fieldIds = templateFields.stream()
                            .map(TemplateTemplateFieldEntity::getTemplateFieldId)
                            .collect(Collectors.toList());
                    List<TemplateFieldEntity> fields = templateFieldRepository.findAllById(fieldIds);
                    
                    Map<UUID, TemplateTemplateFieldEntity> templateFieldMap = templateFields.stream()
                            .collect(Collectors.toMap(
                                    TemplateTemplateFieldEntity::getTemplateFieldId,
                                    tf -> tf
                            ));
                    
                    List<FormFieldDto> formFields = fields.stream()
                            .filter(field -> {
                                TemplateTemplateFieldEntity templateField = templateFieldMap.get(field.getId());
                                return templateField != null && 
                                       Boolean.TRUE.equals(templateField.getEnable()) &&
                                       templateField.getFieldRequirementType() != FieldRequirementType.IGNORE;
                            })
                            .map(field -> {
                                TemplateTemplateFieldEntity templateField = templateFieldMap.get(field.getId());
                                FieldRequirementType requirementType = templateField != null ? templateField.getFieldRequirementType() : null;
                                boolean isRequired = requirementType != null && 
                                                    (requirementType == FieldRequirementType.REQUIRED ||
                                                     requirementType == FieldRequirementType.CONDITIONALLY_REQUIRED);
                                
                                FormFieldDto.FormFieldDtoBuilder builder = FormFieldDto.builder()
                                        .key(field.getTaxCode())
                                        .type("text")
                                        .name(field.getTspCode())
                                        .label(field.getTitle())
                                        .required(isRequired)
                                        .mandatory(isRequired)
                                        .disabled(Boolean.FALSE.equals(templateField != null ? templateField.getEnable() : null))
                                        .pattern(field.getRegexPattern())
                                        .validationMessage(field.getErrorMessageFa());
                                
                                // اضافه کردن requirementType به additionalProperties برای نمایش در frontend
                                if (requirementType != null) {
                                    builder.additionalProperties(Map.of("requirementType", requirementType.name()));
                                }
                                
                                return builder.build();
                            })
                            .collect(Collectors.toList());
                    
                    FormSchemaDto schema = FormSchemaDto.builder()
                            .entityName("Template")
                            .title("فرم قالب: " + template.getTitle())
                            .description("فرم ایجاد و ویرایش قالب " + template.getCode())
                            .fields(formFields)
                            .build();
                    
                    return ResponseEntity.ok(schema);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "دریافت لیست تمام الگوهای صورتحساب",
            description = "این endpoint لیست تمام 16 الگوی صورتحساب را برمی‌گرداند",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست الگوها با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ",
                                            value = "[\n" +
                                                    "  {\n" +
                                                    "    \"patternNumber\": 1,\n" +
                                                    "    \"name\": \"صورتحساب الکترونیکی نوع اول\",\n" +
                                                    "    \"code\": \"TYPE1_SALES\",\n" +
                                                    "    \"hasProvider\": true\n" +
                                                    "  },\n" +
                                                    "  {\n" +
                                                    "    \"patternNumber\": 2,\n" +
                                                    "    \"name\": \"صورتحساب الکترونیکی نوع دوم\",\n" +
                                                    "    \"code\": \"TYPE2_FX\",\n" +
                                                    "    \"hasProvider\": true\n" +
                                                    "  }\n" +
                                                    "]"
                                    )
                            )
                    )
            }
    )
    @GetMapping("/patterns")
    public ResponseEntity<List<Map<String, Object>>> getAllPatterns() {
        List<Map<String, Object>> patterns = invoiceFormBuilderService.getAllPatterns();
        return ResponseEntity.ok(patterns);
    }
}

