package ir.tax.trustedtaxpayers.application.facade;

import ir.tax.trustedtaxpayers.application.dto.FormSchemaDto;
import ir.tax.trustedtaxpayers.application.service.*;
import ir.tax.trustedtaxpayers.core.exception.ResourceNotFoundException;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.*;
import ir.tax.trustedtaxpayers.interfaces.dto.ElectronicInvoiceDtos;
import ir.tax.trustedtaxpayers.interfaces.mapper.ElectronicInvoiceMapper;
import ir.tax.trustedtaxpayers.interfaces.rest.facade.dto.ElectronicInvoiceFacadeRequest;
import ir.tax.trustedtaxpayers.interfaces.rest.facade.dto.ElectronicInvoiceFacadeResponse;
import ir.tax.trustedtaxpayers.interfaces.rest.facade.dto.ElectronicInvoiceFacet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * فاساد لایه کاربردی برای تجمیع سرویس‌های پراکنده صورتحساب الکترونیکی.
 * <p>
 * Provides a single orchestrated access point that loads data from {@code Template}, {@code TemplateRule},
 * {@code TemplateType}, {@code ElectronicInvoiceHeader}, {@code ElectronicInvoiceItem} and
 * {@code ElectronicInvoiceSystem} services. Optionally, the service returns dynamic form schemas
 * that can be used by the UI to construct forms at runtime based on the requested facets.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElectronicInvoiceFacadeService {

    private final TemplateService templateService;
    private final TemplateRuleService templateRuleService;
    private final TemplateTypeService templateTypeService;
    private final ElectronicInvoiceHeaderService headerService;
    private final ElectronicInvoiceItemService itemService;
    private final ElectronicInvoicePaymentService paymentService;
    private final ElectronicInvoiceSystemService systemService;
    private final FormSchemaService formSchemaService;
    private final ElectronicInvoiceMapper mapper;

    /**
     * تجمیع داده‌ها بر اساس سرویس‌های انتخاب شده.
     * <p>
     * Aggregates domain data and optional form schemas according to the requested facets.
     *
     * @param request توصیف سرویس‌های موردنیاز
     * @return پاسخ فاساد شامل داده‌ها و شِماهای فرم
     */
    @Transactional(readOnly = true)
    public ElectronicInvoiceFacadeResponse aggregate(ElectronicInvoiceFacadeRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("ElectronicInvoiceFacadeRequest must not be null");
        }
        Set<ElectronicInvoiceFacet> facets = resolveFacets(request);

        TemplateEntity templateEntity = null;
        TemplateTypeEntity templateTypeEntity = null;
        ElectronicInvoiceHeaderEntity headerEntity = null;

        ElectronicInvoiceDtos.TemplateDto templateDto = null;
        ElectronicInvoiceDtos.TemplateTypeDto templateTypeDto = null;
        List<ElectronicInvoiceDtos.TemplateRuleDto> templateRuleDtos = null;
        ElectronicInvoiceDtos.ElectronicInvoiceHeaderDetailDto headerDetailDto = null;
        List<ElectronicInvoiceDtos.ElectronicInvoiceItemDto> itemDtos = null;
        ElectronicInvoiceDtos.ElectronicInvoiceSystemDto systemDto = null;

        Map<String, FormSchemaDto> schemaMap = request.includeFormSchemas()
                ? new LinkedHashMap<>()
                : null;

        // Template facet
        if (facets.contains(ElectronicInvoiceFacet.TEMPLATE)) {
            templateEntity = resolveTemplateEntity(request, null);
            templateDto = mapper.toTemplateDto(templateEntity);
            registerSchema(schemaMap, TemplateEntity.class);
        }

        // Template type facet
        if (facets.contains(ElectronicInvoiceFacet.TEMPLATE_TYPE)) {
            templateTypeEntity = resolveTemplateTypeEntity(request, templateEntity);
            templateTypeDto = mapper.toTemplateTypeDto(templateTypeEntity);
            registerSchema(schemaMap, TemplateTypeEntity.class);
        }

        // Template rules facet
        if (facets.contains(ElectronicInvoiceFacet.TEMPLATE_RULES)) {
            TemplateEntity ensuredTemplate = templateEntity != null
                    ? templateEntity
                    : resolveTemplateEntity(request, null);

            templateRuleDtos = loadTemplateRules(ensuredTemplate.getCode(), request.onlyActiveRules());
            registerSchema(schemaMap, TemplateRuleEntity.class);
        }

        boolean needsHeader = facets.contains(ElectronicInvoiceFacet.HEADER)
                || facets.contains(ElectronicInvoiceFacet.ITEMS)
                || facets.contains(ElectronicInvoiceFacet.SYSTEM);

        List<ElectronicInvoiceDtos.ElectronicInvoiceItemDto> itemsForHeader = null;
        List<ElectronicInvoiceDtos.ElectronicInvoicePaymentDto> paymentsForHeader = List.of();

        if (needsHeader) {
            headerEntity = resolveHeaderEntity(request, null, true);
            registerSchema(schemaMap, ElectronicInvoiceHeaderEntity.class);
        }

        if (facets.contains(ElectronicInvoiceFacet.ITEMS)) {
            ElectronicInvoiceHeaderEntity ensuredHeader = headerEntity != null
                    ? headerEntity
                    : resolveHeaderEntity(request, null, true);
            UUID headerId = ensuredHeader.getId();
            itemDtos = itemService.findByHeaderId(headerId).stream()
                    .map(mapper::toItemDto)
                    .toList();
            itemsForHeader = itemDtos;
            registerSchema(schemaMap, ElectronicInvoiceItemEntity.class);
        }

        if (facets.contains(ElectronicInvoiceFacet.SYSTEM)) {
            ElectronicInvoiceHeaderEntity ensuredHeader = headerEntity != null
                    ? headerEntity
                    : resolveHeaderEntity(request, null, true);
            systemDto = systemService.findByHeaderId(ensuredHeader.getId())
                    .map(mapper::toSystemDto)
                    .orElse(null);
            registerSchema(schemaMap, ElectronicInvoiceSystemEntity.class);
        }

        if (facets.contains(ElectronicInvoiceFacet.HEADER)) {
            ElectronicInvoiceHeaderEntity ensuredHeader = headerEntity != null
                    ? headerEntity
                    : resolveHeaderEntity(request, null, true);

            List<ElectronicInvoiceDtos.ElectronicInvoiceItemDto> resolvedItems = itemsForHeader != null
                    ? itemsForHeader
                    : itemService.findByHeaderId(ensuredHeader.getId()).stream()
                    .map(mapper::toItemDto)
                    .toList();

            paymentsForHeader = paymentService.findByHeaderId(ensuredHeader.getId()).stream()
                    .map(mapper::toPaymentDto)
                    .toList();
            registerSchema(schemaMap, ElectronicInvoicePaymentEntity.class);

            ElectronicInvoiceDtos.ElectronicInvoiceSystemDto resolvedSystem = systemDto != null
                    ? systemDto
                    : systemService.findByHeaderId(ensuredHeader.getId())
                    .map(mapper::toSystemDto)
                    .orElse(null);

            headerDetailDto = mapper.toDetailDto(ensuredHeader, resolvedItems, paymentsForHeader, resolvedSystem);
        }

        Map<String, FormSchemaDto> unmodifiableSchemas = schemaMap != null
                ? Collections.unmodifiableMap(schemaMap)
                : null;

        return ElectronicInvoiceFacadeResponse.builder()
                .template(templateDto)
                .templateType(templateTypeDto)
                .templateRules(templateRuleDtos)
                .header(headerDetailDto)
                .items(itemDtos)
                .system(systemDto)
                .formSchemas(unmodifiableSchemas)
                .build();
    }

    private Set<ElectronicInvoiceFacet> resolveFacets(ElectronicInvoiceFacadeRequest request) {
        if (request.facets() == null || request.facets().isEmpty()) {
            throw new IllegalArgumentException("facets must not be empty");
        }
        return EnumSet.copyOf(request.facets());
    }

    private TemplateEntity resolveTemplateEntity(ElectronicInvoiceFacadeRequest request, TemplateEntity fallback) {
        if (fallback != null) {
            return fallback;
        }
        if (StringUtils.hasText(request.templateCode())) {
            return templateService.findByCode(request.templateCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + request.templateCode()));
        }
        ElectronicInvoiceHeaderEntity header = resolveHeaderEntity(request, null, false);
        if (header != null && StringUtils.hasText(header.getTemplateCode())) {
            return templateService.findByCode(header.getTemplateCode())
                    .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + header.getTemplateCode()));
        }
        throw new IllegalArgumentException("templateCode or related header information is required to fetch template data");
    }

    private TemplateTypeEntity resolveTemplateTypeEntity(ElectronicInvoiceFacadeRequest request, TemplateEntity templateEntity) {
        // templateTypeCode باید در درخواست ارائه شود
        if (!StringUtils.hasText(request.templateTypeCode())) {
            throw new IllegalArgumentException("templateTypeCode باید در درخواست ارائه شود");
        }

        return templateTypeService.findByCode(request.templateTypeCode())
                .orElseThrow(() -> new ResourceNotFoundException("Template type not found: " + request.templateTypeCode()));
    }

    private List<ElectronicInvoiceDtos.TemplateRuleDto> loadTemplateRules(String templateCode, boolean onlyActive) {
        List<TemplateRuleEntity> entities = onlyActive
                ? templateRuleService.findByTemplateCodeAndActive(templateCode, "Y")
                : templateRuleService.findByTemplateCode(templateCode);
        return entities.stream()
                .map(mapper::toTemplateRuleDto)
                .toList();
    }

    private ElectronicInvoiceHeaderEntity resolveHeaderEntity(ElectronicInvoiceFacadeRequest request,
                                                              ElectronicInvoiceHeaderEntity fallback,
                                                              boolean required) {
        if (fallback != null) {
            return fallback;
        }
        if (request.headerId() != null) {
            return headerService.findById(request.headerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice header not found: " + request.headerId()));
        }
       /* if (StringUtils.hasText(request.taxId())) {
            return headerService.findByTaxId(request.taxId())
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice header not found for taxId: " + request.taxId()));
        }*/
        if (required) {
            throw new IllegalArgumentException("headerId or taxId is required when requesting invoice facets");
        }
        return null;
    }

    private void registerSchema(Map<String, FormSchemaDto> schemaMap, Class<?> entityClass) {
        if (schemaMap == null || schemaMap.containsKey(entityClass.getSimpleName())) {
            return;
        }
        try {
            FormSchemaDto schema = formSchemaService.getFormSchema(entityClass);
            schemaMap.put(entityClass.getSimpleName(), schema);
        } catch (Exception ex) {
            log.debug("Failed to generate form schema for {}: {}", entityClass.getSimpleName(), ex.getMessage());
        }
    }
}

