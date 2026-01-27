package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.TemplateService;
import ir.tax.trustedtaxpayers.application.service.TemplateTemplateTypeService;
import ir.tax.trustedtaxpayers.biz.service.TemplateFieldService;
import ir.tax.trustedtaxpayers.biz.service.TemplateTemplateFieldService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateFieldEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTemplateFieldEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTemplateTypeEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTypeEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateFieldRepository;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.TemplateFieldWithRelationDto;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.TemplateFieldsResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping({"/api/template-fields", "/api/v1/template-fields"})
@Tag(name = "Template Field Management", description = "مدیریت فیلدهای قالب")
public class TemplateFieldController extends BaseController<TemplateFieldEntity, UUID> {
    
    private final TemplateTemplateFieldService templateTemplateFieldService;
    private final TemplateService templateService;
    private final TemplateFieldRepository templateFieldRepository;
    private final TemplateTemplateTypeService templateTemplateTypeService;
    
    public TemplateFieldController(
            TemplateFieldService templateFieldService,
            TemplateFieldRepository templateFieldRepository,
            TemplateTemplateFieldService templateTemplateFieldService,
            TemplateService templateService,
            TemplateTemplateTypeService templateTemplateTypeService) {
        super(templateFieldService, templateFieldRepository);
        this.templateTemplateFieldService = templateTemplateFieldService;
        this.templateService = templateService;
        this.templateFieldRepository = templateFieldRepository;
        this.templateTemplateTypeService = templateTemplateTypeService;
    }
    
    @Override
    protected List<String> getAutocompleteSearchFields() {
        return List.of("code", "title");
    }
    
    @Operation(
            summary = "دریافت فیلدهای یک قالب بر اساس شناسه",
            description = "دریافت لیست فیلدهای مرتبط با یک قالب خاص بر اساس شناسه قالب",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست فیلدها با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = TemplateFieldEntity.class))
                    )
            }
    )
    @GetMapping("/template/{templateId}")
    public ResponseEntity<List<TemplateFieldEntity>> getFieldsByTemplateId(
            @PathVariable
            @Schema(description = "شناسه قالب", example = "123e4567-e89b-12d3-a456-426614174000")
            UUID templateId) {
        List<TemplateFieldEntity> fields = templateTemplateFieldService.getFieldsByTemplateId(templateId);
        return ResponseEntity.ok(fields);
    }
    
    @Operation(
            summary = "دریافت فیلدهای یک قالب بر اساس کد",
            description = "دریافت لیست فیلدهای مرتبط با یک قالب خاص بر اساس کد قالب",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست فیلدها با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = TemplateFieldsResponseDto.class))
                    )
            }
    )
    @GetMapping("/template/code/{templateCode}")
    public ResponseEntity<TemplateFieldsResponseDto> getFieldsByTemplateCode(
            @PathVariable
            @Schema(description = "کد قالب", example = "SALE_TEMPLATE_1_TYPE1")
            String templateCode) {
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
                    
                    List<TemplateFieldWithRelationDto> result = fields.stream()
                            .map(field -> {
                                TemplateTemplateFieldEntity templateField = templateFieldMap.get(field.getId());
                                return TemplateFieldWithRelationDto.from(
                                        field,
                                        templateField != null ? templateField.getFieldRequirementType() : null,
                                        templateField != null ? templateField.getEnable() : null
                                );
                            })
                            .collect(Collectors.toList());
                    
                    // دریافت TemplateTypeEntity ها از جدول واسط
                    List<TemplateTemplateTypeEntity> templateTemplateTypes = templateTemplateTypeService.findByTemplateId(template.getId());
                    List<TemplateTypeEntity> templateTypes = templateTemplateTypes.stream()
                            .map(TemplateTemplateTypeEntity::getTemplateType)
                            .collect(Collectors.toList());
                    
                    TemplateFieldsResponseDto response = new TemplateFieldsResponseDto(result, templateTypes);
                    return ResponseEntity.ok(response);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}







