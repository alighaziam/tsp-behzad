package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.TemplateService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping({"/api/templates", "/api/v1/templates"})
@Tag(name = "Template Management", description = "مدیریت قالب‌های صورتحساب\n\nراهنمای سریع:\n- Base: /api/templates\n- همه: GET /\n- جستجو: POST /search\n- اتوکامپلیت: GET /autocomplete\n- هدرها: Content-Type: application/json و Authorization: Bearer <JWT>\n")
public class TemplateController extends BaseController<TemplateEntity, UUID> {
    
    private final TemplateService templateService;
    
    public TemplateController(
            TemplateService templateService,
            TemplateRepository templateRepository) {
        super(templateService, templateRepository);
        this.templateService = templateService;
    }
    
    @Operation(
            summary = "دریافت قالب بر اساس کد",
            description = "دریافت اطلاعات قالب بر اساس کد قالب",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "قالب با موفقیت برگردانده شد",
                            content = @Content(schema = @Schema(implementation = TemplateEntity.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "قالب یافت نشد"
                    )
            }
    )
    @GetMapping("/code/{templateCode}")
    public ResponseEntity<TemplateEntity> getTemplateByCode(
            @PathVariable
            @Schema(description = "کد قالب", example = "SALE_TEMPLATE_1_TYPE1")
            String templateCode) {
        return templateService.findByCode(templateCode)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }


}


