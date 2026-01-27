package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.dto.AllEntitiesResponseDto;
import ir.tax.trustedtaxpayers.application.dto.EntityInfoDto;
import ir.tax.trustedtaxpayers.application.dto.FormFieldDto;
import ir.tax.trustedtaxpayers.application.dto.FormSchemaDto;
import ir.tax.trustedtaxpayers.application.service.EntityRegistryService;
import ir.tax.trustedtaxpayers.application.service.FormSchemaService;
import ir.tax.trustedtaxpayers.application.service.TemplateService;
import ir.tax.trustedtaxpayers.biz.service.TemplateTemplateFieldService;
import ir.tax.trustedtaxpayers.enums.FieldRequirementType;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateFieldEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTemplateFieldEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateFieldRepository;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * کنترلر عمومی برای دریافت اسکیمای فرم و اطلاعات موجودیت‌ها جهت ساخت UI پویا.
 * <p>
 * REST controller allowing clients to fetch entity form schemas and metadata for dynamic builders.
 */
@RestController
@RequestMapping("/api/form-schema")
@RequiredArgsConstructor
@Tag(name = "Form Schema", description = "دریافت مشخصات فیلدهای فرم برای Angular Form Builder\n\nراهنمای سریع:\n- Base: /api/form-schema\n- بر اساس نام کامل کلاس: GET /by-class-name?className=...\n- بر اساس نام ساده: GET /by-simple-name?simpleName=...\n- بررسی وجود: GET /exists?className=...\n- همه Entityها: GET /all-entities\n- اطلاعات Entity+روابط: GET /entity-with-relations?className=...\n")
public class FormSchemaController {

    private final FormSchemaService formSchemaService;
    private final EntityRegistryService entityRegistryService;
    private final TemplateService templateService;
    private final TemplateTemplateFieldService templateTemplateFieldService;
    private final TemplateFieldRepository templateFieldRepository;

    @Operation(
            summary = "دریافت Form Schema از نام کامل کلاس Entity",
            description = "دریافت JSON Schema برای Angular Form Builder از نام کامل کلاس Entity. " +
                         "مثال: ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateEntity",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Schema با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = FormSchemaDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "{\n" +
                                                    "  \"entityName\": \"TemplateEntity\",\n" +
                                                    "  \"title\": \"فرم TemplateEntity\",\n" +
                                                    "  \"description\": \"فرم ایجاد و ویرایش TemplateEntity\",\n" +
                                                    "  \"fields\": [\n" +
                                                    "    {\n" +
                                                    "      \"key\": \"code\",\n" +
                                                    "      \"type\": \"text\",\n" +
                                                    "      \"name\": \"Code\",\n" +
                                                    "      \"label\": \"Code\",\n" +
                                                    "      \"required\": true,\n" +
                                                    "      \"mandatory\": true,\n" +
                                                    "      \"maxLength\": 10\n" +
                                                    "    }\n" +
                                                    "  ]\n" +
                                                    "}"
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "کلاس Entity یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @GetMapping("/by-class-name")
    public ResponseEntity<FormSchemaDto> getFormSchemaByClassName(
            @RequestParam("className")
            @Schema(description = "نام کامل کلاس Entity", example = "ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateEntity")
            String className) {
        Optional<FormSchemaDto> schema = formSchemaService.getFormSchemaByClassName(className);
        return schema.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "دریافت Form Schema از نام ساده کلاس Entity",
            description = "دریافت JSON Schema برای Angular Form Builder از نام ساده کلاس Entity. " +
                         "سرویس به صورت خودکار در package های رایج جستجو می‌کند. " +
                         "مثال: TemplateEntity",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Schema با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = FormSchemaDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "کلاس Entity یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @GetMapping("/by-simple-name")
    public ResponseEntity<FormSchemaDto> getFormSchemaBySimpleName(
            @RequestParam("simpleName")
            @Schema(description = "نام ساده کلاس Entity", example = "TemplateEntity")
            String simpleName) {
        Optional<FormSchemaDto> schema = formSchemaService.getFormSchemaBySimpleName(simpleName);
        return schema.map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
            summary = "بررسی وجود کلاس Entity",
            description = "بررسی اینکه آیا کلاس Entity با نام داده شده وجود دارد یا نه",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "نتیجه بررسی برگردانده شد",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ",
                                            value = "{\n" +
                                                    "  \"exists\": true\n" +
                                                    "}"
                                    )
                            )
                    )
            }
    )
    @GetMapping("/exists")
    public ResponseEntity<ir.tax.trustedtaxpayers.interfaces.rest.dto.ExistsResponse> checkEntityExists(
            @RequestParam("className")
            @Schema(description = "نام کامل کلاس Entity", example = "ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateEntity")
            String className) {
        boolean exists = formSchemaService.entityClassExists(className);
        return ResponseEntity.ok(new ir.tax.trustedtaxpayers.interfaces.rest.dto.ExistsResponse(exists));
    }

    @Operation(
            summary = "دریافت اطلاعات تمام Entity های سیستم",
            description = "دریافت لیست کامل تمام Entity های موجود در سیستم به همراه رابطه‌های آن‌ها و Schema های مربوطه. " +
                         "این endpoint شامل Entity های مرتبط با هر Entity هم می‌شود.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست Entity ها با موفقیت برگردانده شد",
                            content = @Content(
                                    schema = @Schema(implementation = AllEntitiesResponseDto.class),
                                    examples = @ExampleObject(
                                            name = "نمونه پاسخ موفق",
                                            value = "{\n" +
                                                    "  \"totalCount\": 10,\n" +
                                                    "  \"entities\": [\n" +
                                                    "    {\n" +
                                                    "      \"className\": \"ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateEntity\",\n" +
                                                    "      \"simpleName\": \"TemplateEntity\",\n" +
                                                    "      \"tableName\": \"TEMPLATE\",\n" +
                                                    "      \"packageName\": \"ir.tax.trustedtaxpayers.infrastructure.jpa.entity\",\n" +
                                                    "      \"fieldCount\": 8,\n" +
                                                    "      \"hasSchema\": true,\n" +
                                                    "      \"relations\": [\n" +
                                                    "        {\n" +
                                                    "          \"fieldName\": \"templateType\",\n" +
                                                    "          \"relationType\": \"MANY_TO_ONE\",\n" +
                                                    "          \"relatedEntityClassName\": \"ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTypeEntity\",\n" +
                                                    "          \"relatedEntitySimpleName\": \"TemplateTypeEntity\",\n" +
                                                    "          \"required\": false,\n" +
                                                    "          \"nullable\": true\n" +
                                                    "        }\n" +
                                                    "      ],\n" +
                                                    "      \"relatedEntities\": [\n" +
                                                    "        \"ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTypeEntity\"\n" +
                                                    "      ]\n" +
                                                    "    }\n" +
                                                    "  ],\n" +
                                                    "  \"schemas\": {\n" +
                                                    "    \"TemplateEntity\": {\n" +
                                                    "      \"entityName\": \"TemplateEntity\",\n" +
                                                    "      \"title\": \"فرم TemplateEntity\",\n" +
                                                    "      \"fields\": [...]\n" +
                                                    "    }\n" +
                                                    "  }\n" +
                                                    "}"
                                    )
                            )
                    )
            }
    )
    @GetMapping("/all-entities")
    public ResponseEntity<AllEntitiesResponseDto> getAllEntities() {
        AllEntitiesResponseDto response = entityRegistryService.getAllEntities();
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "دریافت اطلاعات یک Entity به همراه Entity های مرتبط",
            description = "دریافت اطلاعات کامل یک Entity شامل رابطه‌های آن با سایر Entity ها",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "اطلاعات Entity با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = EntityInfoDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Entity یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @GetMapping("/entity-with-relations")
    public ResponseEntity<EntityInfoDto> getEntityWithRelations(
            @RequestParam("className")
            @Schema(description = "نام کامل کلاس Entity", example = "ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateEntity")
            String className) {
        EntityInfoDto entityInfo = entityRegistryService.getEntityWithRelations(className);
        if (entityInfo != null) {
            return ResponseEntity.ok(entityInfo);
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(
            summary = "دریافت Form Schema بر اساس شماره الگو و نوع صورتحساب",
            description = "دریافت JSON Schema برای Angular Form Builder بر اساس شماره الگو (pattern) و نوع صورتحساب (invoice type). " +
                         "این endpoint از جدول واسط TEMPLATE_TEMPLATE_FIELD برای دریافت فیلدهای قالب استفاده می‌کند.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Schema با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = FormSchemaDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "قالب یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @GetMapping("/template/pattern/{patternNumber}/type/{invoiceType}")
    public ResponseEntity<FormSchemaDto> getFormSchemaByPatternAndType(
            @PathVariable
            @Schema(description = "شماره الگو (1-16)", example = "1")
            Integer patternNumber,
            @PathVariable
            @Schema(description = "نوع صورتحساب (1=نوع اول, 2=نوع دوم)", example = "1")
            Integer invoiceType) {
        String templateCode = buildTemplateCode(patternNumber, invoiceType);
        if (templateCode == null) {
            return ResponseEntity.notFound().build();
        }
        return getFormSchemaByTemplateCodeInternal(templateCode);
    }

    @Operation(
            summary = "دریافت Form Schema بر اساس کد قالب",
            description = "دریافت JSON Schema برای Angular Form Builder بر اساس کد قالب. " +
                         "این endpoint از جدول واسط TEMPLATE_TEMPLATE_FIELD برای دریافت فیلدهای قالب استفاده می‌کند.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Schema با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = FormSchemaDto.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "قالب یافت نشد",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @GetMapping("/template/{templateCode}")
    public ResponseEntity<FormSchemaDto> getFormSchemaByTemplateCode(
            @PathVariable
            @Schema(description = "کد قالب", example = "SALE_TEMPLATE_1_TYPE1")
            String templateCode) {
        return getFormSchemaByTemplateCodeInternal(templateCode);
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
        
        // بررسی اینکه آیا این ترکیب pattern + type وجود دارد
        // از migration file می‌دانیم که همه pattern ها TYPE1 دارند، اما فقط برخی TYPE2 دارند
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

    private ResponseEntity<FormSchemaDto> getFormSchemaByTemplateCodeInternal(String templateCode) {
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
}

