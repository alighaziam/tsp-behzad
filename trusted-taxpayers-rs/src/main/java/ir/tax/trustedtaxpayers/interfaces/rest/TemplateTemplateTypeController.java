package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.TemplateTemplateTypeService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTemplateTypeEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * کنترلر مدیریت رابطه بین Template و TemplateType
 * 
 * @author Ghazi-a
 */
@RestController
@RequestMapping({"/api/template-template-types", "/api/v1/template-template-types"})
@Tag(name = "Template TemplateType Mapping", description = "مدیریت رابطه بین Template و TemplateType\n\nراهنمای سریع:\n- Base: /api/template-template-types\n- همه: GET /\n- بر اساس Template: GET /by-template/{templateId}\n- بر اساس TemplateType: GET /by-template-type/{templateTypeId}\n- ایجاد: POST /\n- حذف: DELETE /{id}\n")
public class TemplateTemplateTypeController {
    
    private final TemplateTemplateTypeService service;
    
    public TemplateTemplateTypeController(TemplateTemplateTypeService service) {
        this.service = service;
    }
    
    @Operation(
            summary = "دریافت تمام روابط",
            description = "دریافت لیست تمام روابط بین Template و TemplateType",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست روابط با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema())
                    )
            }
    )
    @GetMapping
    public ResponseEntity<List<TemplateTemplateTypeEntity>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }
    
    @Operation(
            summary = "دریافت رابطه بر اساس ID",
            description = "دریافت یک رابطه خاص بر اساس شناسه",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "رابطه با موفقیت یافت شد"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "رابطه یافت نشد"
                    )
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<TemplateTemplateTypeEntity> findById(@PathVariable UUID id) {
        return service.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    
    @Operation(
            summary = "دریافت روابط یک Template",
            description = "دریافت تمام روابط مربوط به یک Template",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست روابط با موفقیت برگردانده شد"
                    )
            }
    )
    @GetMapping("/by-template/{templateId}")
    public ResponseEntity<List<TemplateTemplateTypeEntity>> findByTemplateId(@PathVariable UUID templateId) {
        return ResponseEntity.ok(service.findByTemplateId(templateId));
    }
    
    @Operation(
            summary = "دریافت روابط یک TemplateType",
            description = "دریافت تمام روابط مربوط به یک TemplateType",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "لیست روابط با موفقیت برگردانده شد"
                    )
            }
    )
    @GetMapping("/by-template-type/{templateTypeId}")
    public ResponseEntity<List<TemplateTemplateTypeEntity>> findByTemplateTypeId(@PathVariable UUID templateTypeId) {
        return ResponseEntity.ok(service.findByTemplateTypeId(templateTypeId));
    }
    
    @Operation(
            summary = "ایجاد رابطه جدید",
            description = "ایجاد رابطه جدید بین Template و TemplateType",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "رابطه با موفقیت ایجاد شد"
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "اطلاعات ورودی نامعتبر یا رابطه تکراری"
                    )
            }
    )
    @PostMapping
    public ResponseEntity<TemplateTemplateTypeEntity> create(
            @RequestParam UUID templateId,
            @RequestParam UUID templateTypeId) {
        try {
            TemplateTemplateTypeEntity created = service.create(templateId, templateTypeId);
            return ResponseEntity.ok(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(
            summary = "حذف رابطه",
            description = "حذف یک رابطه بر اساس شناسه",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "رابطه با موفقیت حذف شد"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "رابطه یافت نشد"
                    )
            }
    )
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        if (service.findById(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        service.deleteById(id);
        return ResponseEntity.noContent().build();
    }
    
    @Operation(
            summary = "حذف روابط یک Template",
            description = "حذف تمام روابط مربوط به یک Template",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "روابط با موفقیت حذف شدند"
                    )
            }
    )
    @DeleteMapping("/by-template/{templateId}")
    public ResponseEntity<Void> deleteByTemplateId(@PathVariable UUID templateId) {
        service.deleteByTemplateId(templateId);
        return ResponseEntity.noContent().build();
    }
    
    @Operation(
            summary = "حذف روابط یک TemplateType",
            description = "حذف تمام روابط مربوط به یک TemplateType",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "روابط با موفقیت حذف شدند"
                    )
            }
    )
    @DeleteMapping("/by-template-type/{templateTypeId}")
    public ResponseEntity<Void> deleteByTemplateTypeId(@PathVariable UUID templateTypeId) {
        service.deleteByTemplateTypeId(templateTypeId);
        return ResponseEntity.noContent().build();
    }
    
    @Operation(
            summary = "حذف رابطه خاص",
            description = "حذف رابطه خاص بین Template و TemplateType",
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "رابطه با موفقیت حذف شد"
                    )
            }
    )
    @DeleteMapping("/by-template/{templateId}/by-template-type/{templateTypeId}")
    public ResponseEntity<Void> deleteByTemplateIdAndTemplateTypeId(
            @PathVariable UUID templateId,
            @PathVariable UUID templateTypeId) {
        service.deleteByTemplateIdAndTemplateTypeId(templateId, templateTypeId);
        return ResponseEntity.noContent().build();
    }
}







