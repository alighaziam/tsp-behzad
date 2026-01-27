package ir.tax.trustedtaxpayers.interfaces.rest;

import io.swagger.v3.oas.annotations.tags.Tag;
import ir.tax.trustedtaxpayers.application.service.TemplateTypeService;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTypeEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateTypeRepository;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * کنترلر مدیریت انواع قالب صورتحساب
 * 
 * @author Ghazi-a
 */
@RestController
@RequestMapping({"/api/template-types", "/api/v1/template-types"})
@Tag(name = "Template Type Management", description = "مدیریت انواع قالب صورتحساب\n\nراهنمای سریع:\n- Base: /api/template-types\n- همه: GET /\n- جستجو: POST /search\n- اتوکامپلیت: GET /autocomplete\n- هدرها: Content-Type: application/json و Authorization: Bearer <JWT>\n")
public class TemplateTypeController extends BaseController<TemplateTypeEntity, UUID> {
    public TemplateTypeController(
            TemplateTypeService templateTypeService,
            TemplateTypeRepository templateTypeRepository) {
        super(templateTypeService, templateTypeRepository);
    }
    
    @Override
    protected List<String> getAutocompleteSearchFields() {
        return List.of("code", "title", "description");
    }
}



