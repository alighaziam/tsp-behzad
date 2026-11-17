package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.cache.CacheEvict;
import ir.tax.trustedtaxpayers.application.cache.Cacheable;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTemplateTypeEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTypeEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceHeaderRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateTemplateTypeRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateTypeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Loads invoices using the normalized TEMPLATE <-> TEMPLATE_TYPE association.
 */
@Service
public class InvoiceByTemplateTypeService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceByTemplateTypeService.class);
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "createdOn", "updatedOn", "taxId", "indatim", "tbill", "templateCode"
    );

    private final ElectronicInvoiceHeaderRepository invoiceRepository;
    private final TemplateTypeRepository templateTypeRepository;
    private final TemplateTemplateTypeRepository templateTemplateTypeRepository;

    public InvoiceByTemplateTypeService(
            ElectronicInvoiceHeaderRepository invoiceRepository,
            TemplateTypeRepository templateTypeRepository,
            TemplateTemplateTypeRepository templateTemplateTypeRepository) {
        this.invoiceRepository = invoiceRepository;
        this.templateTypeRepository = templateTypeRepository;
        this.templateTemplateTypeRepository = templateTemplateTypeRepository;
    }

    @Cacheable(cacheName = "invoices-by-template-type", keyPrefix = "template-type-page")
    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> loadInvoicesByTemplateType(
            String templateTypeCode, Integer page, Integer size, String sortBy, String direction) {
        List<String> templateCodes = resolveActiveTemplateCodes(templateTypeCode);
        if (templateCodes.isEmpty()) {
            return Page.empty();
        }
        int safePage = page == null ? 0 : Math.max(0, page);
        int safeSize = size == null || size < 1 ? 20 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize, buildSort(sortBy, direction));
        Page<ElectronicInvoiceHeaderEntity> invoices =
                invoiceRepository.findByTemplateCodeIn(templateCodes, pageable);
        logger.debug("Loaded {} invoices for templateTypeCode={}, page={}, size={}",
                invoices.getTotalElements(), templateTypeCode, safePage, safeSize);
        return invoices;
    }

    @Cacheable(cacheName = "invoices-by-template-type-list", keyPrefix = "template-type")
    @Transactional(readOnly = true)
    public List<ElectronicInvoiceHeaderEntity> loadInvoicesByTemplateType(String templateTypeCode) {
        List<String> templateCodes = resolveActiveTemplateCodes(templateTypeCode);
        return templateCodes.isEmpty()
                ? List.of()
                : invoiceRepository.findByTemplateCodeIn(templateCodes);
    }

    @Cacheable(cacheName = "invoice-count-by-template-type", keyPrefix = "count")
    @Transactional(readOnly = true)
    public long countInvoicesByTemplateType(String templateTypeCode) {
        List<String> templateCodes = resolveActiveTemplateCodes(templateTypeCode);
        return templateCodes.isEmpty() ? 0 : invoiceRepository.countByTemplateCodeIn(templateCodes);
    }

    private List<String> resolveActiveTemplateCodes(String templateTypeCode) {
        if (templateTypeCode == null || templateTypeCode.isBlank()) {
            return List.of();
        }
        TemplateTypeEntity templateType = templateTypeRepository.findByCode(templateTypeCode.trim())
                .filter(type -> Boolean.TRUE.equals(type.getIsActive()))
                .orElse(null);
        if (templateType == null) {
            return List.of();
        }
        return templateTemplateTypeRepository.findByTemplateTypeId(templateType.getId()).stream()
                .map(TemplateTemplateTypeEntity::getTemplate)
                .filter(template -> template != null && Boolean.TRUE.equals(template.getIsActive()))
                .map(TemplateEntity::getCode)
                .filter(code -> code != null && !code.isBlank())
                .distinct()
                .toList();
    }

    private Sort buildSort(String sortBy, String direction) {
        String field = sortBy != null && ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "createdOn";
        Sort.Direction sortDirection = "DESC".equalsIgnoreCase(direction)
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(sortDirection, field);
    }

    @CacheEvict(cacheName = "invoices-by-template-type", allEntries = true)
    @CacheEvict(cacheName = "invoices-by-template-type-list", allEntries = true)
    @CacheEvict(cacheName = "invoice-count-by-template-type", allEntries = true)
    public void invalidateCache(String templateTypeCode) {
        logger.debug("Cache invalidated for templateTypeCode: {}", templateTypeCode);
    }

    @CacheEvict(cacheName = "invoices-by-template-type", allEntries = true)
    @CacheEvict(cacheName = "invoices-by-template-type-list", allEntries = true)
    @CacheEvict(cacheName = "invoice-count-by-template-type", allEntries = true)
    public void invalidateAllCache() {
        logger.info("All invoice by template type caches invalidated");
    }
}
