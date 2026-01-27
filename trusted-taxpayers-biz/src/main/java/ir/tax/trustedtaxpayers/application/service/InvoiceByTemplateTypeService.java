package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.cache.CacheEvict;
import ir.tax.trustedtaxpayers.application.cache.Cacheable;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceHeaderRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

/**
 * سرویس برای لود کردن صورتحساب‌ها بر اساس نوع الگو (TemplateType)
 * 
 * <p>این سرویس برای لود کردن صورتحساب‌ها بر اساس نوع الگو (TemplateType) استفاده می‌شود.</p>
 * 
 * <p>ویژگی‌ها:</p>
 * <ul>
 *   <li>Cache برای عملکرد بهتر</li>
 *   <li>پشتیبانی از صفحه‌بندی</li>
 *   <li>Eager loading برای جلوگیری از N+1</li>
 *   <li>فیلتر بر اساس کد TemplateType</li>
 * </ul>
 * 
 * @author Ghazi-a
 * @since 1.0
 */
@Service
public class InvoiceByTemplateTypeService {
    
    private static final Logger logger = LoggerFactory.getLogger(InvoiceByTemplateTypeService.class);
    
    private final ElectronicInvoiceHeaderRepository invoiceRepository;
    private final TemplateRepository templateRepository;
    
    public InvoiceByTemplateTypeService(
            ElectronicInvoiceHeaderRepository invoiceRepository,
            TemplateRepository templateRepository) {
        this.invoiceRepository = invoiceRepository;
        this.templateRepository = templateRepository;
    }

    /**
     * لود کردن صورتحساب‌ها بر اساس TemplateType code با pagination
     * 
     * @param templateTypeCode کد نوع الگو (مثل P01, P02, ...)
     * @param page شماره صفحه (0-based)
     * @param size تعداد آیتم در هر صفحه
     * @param sortBy فیلد برای sort
     * @param direction جهت sort (ASC/DESC)
     * @return Page از صورتحساب‌ها
     */
    @Cacheable(cacheName = "invoices-by-template-type", keyPrefix = "template-type-page")
    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> loadInvoicesByTemplateType(
            String templateTypeCode,
            Integer page,
            Integer size,
            String sortBy,
            String direction) {
        
        if (templateTypeCode == null || templateTypeCode.isBlank()) {
            logger.warn("TemplateTypeCode is null or empty, returning empty page");
            return Page.empty();
        }
        
        // توجه: findByTemplateTypeCodeAndIsActiveTrue دیگر وجود ندارد چون templateTypeCode از TemplateEntity حذف شده است
        // این سرویس نیاز به بازنویسی دارد تا بدون templateTypeCode در TemplateEntity کار کند
        // فعلاً لیست خالی برمی‌گردانیم چون دیگر نمی‌توان templateTypeCode را از TemplateEntity جستجو کرد
        List<TemplateEntity> templates = Collections.emptyList();
        
        if (templates.isEmpty()) {
            logger.debug("No active templates found for templateTypeCode: {}", templateTypeCode);
            return Page.empty();
        }
        
        // استخراج template codes
        List<String> templateCodes = templates.stream()
            .map(TemplateEntity::getCode)
            .toList();
        
        // ساخت Pageable
        Sort sort = buildSort(sortBy, direction);
        int validPage = page != null && page >= 0 ? page : 0;
        int validSize = size != null && size > 0 ? Math.min(size, 100) : 20;
        Pageable pageable = PageRequest.of(validPage, validSize, sort);
        
        Page<ElectronicInvoiceHeaderEntity> invoices = findInvoicesByTemplateCodes(templateCodes, pageable);
        
        logger.debug("Loaded {} invoices for templateTypeCode={}, page={}, size={}", 
            invoices.getTotalElements(), templateTypeCode, page, size);
        
        return invoices;
    }

    /**
     * لود کردن صورتحساب‌ها بر اساس TemplateType code (بدون pagination)
     * 
     * @param templateTypeCode کد نوع الگو
     * @return لیست صورتحساب‌ها
     */
    @Cacheable(cacheName = "invoices-by-template-type-list", keyPrefix = "template-type")
    @Transactional(readOnly = true)
    public List<ElectronicInvoiceHeaderEntity> loadInvoicesByTemplateType(String templateTypeCode) {
        if (templateTypeCode == null || templateTypeCode.isBlank()) {
            return Collections.emptyList();
        }
        
        // توجه: findByTemplateTypeCodeAndIsActiveTrue دیگر وجود ندارد چون templateTypeCode از TemplateEntity حذف شده است
        // این سرویس نیاز به بازنویسی دارد تا بدون templateTypeCode در TemplateEntity کار کند
        // فعلاً لیست خالی برمی‌گردانیم چون دیگر نمی‌توان templateTypeCode را از TemplateEntity جستجو کرد
        List<TemplateEntity> templates = Collections.emptyList();
        
        if (templates.isEmpty()) {
            return Collections.emptyList();
        }
        
        // استخراج template codes
        List<String> templateCodes = templates.stream()
            .map(TemplateEntity::getCode)
            .toList();
        
        // استفاده از متد بهینه شده با IN clause
        // Using paginated version instead of deprecated method
        /*return invoiceRepository.findByTemplateCodeIn(templateCodes, Pageable.unpaged()).getContent();*/
        return null;
    }

   /* *//**
     * تعداد صورتحساب‌ها بر اساس TemplateType code
     * 
     * @param templateTypeCode کد نوع الگو
     * @return تعداد صورتحساب‌ها
     *//*
    @Cacheable(cacheName = "invoice-count-by-template-type", keyPrefix = "count")
    @Transactional(readOnly = true)
    public long countInvoicesByTemplateType(String templateTypeCode) {
        if (templateTypeCode == null || templateTypeCode.isBlank()) {
            return 0;
        }
        
        List<TemplateEntity> templates = templateRepository.findByTemplateTypeCodeAndIsActiveTrue(templateTypeCode);
        
        if (templates.isEmpty()) {
            return 0;
        }
        
        List<String> templateCodes = templates.stream()
            .map(TemplateEntity::getCode)
            .toList();
        
        if (templateCodes.size() == 1) {
            return invoiceRepository.countByTemplateCode(templateCodes.get(0));
        }
        
        // استفاده از متد بهینه شده
        return invoiceRepository.countByTemplateCodeIn(templateCodes);
    }
*/
    /**
     * پیدا کردن invoices بر اساس template codes با pagination
     * این متد از query بهینه با IN clause استفاده می‌کند
     */
    private Page<ElectronicInvoiceHeaderEntity> findInvoicesByTemplateCodes(
            List<String> templateCodes,
            Pageable pageable) {
        
        // اگر فقط یک template code داریم، از متد مستقیم استفاده می‌کنیم
        if (templateCodes.size() == 1) {
            /*return invoiceRepository.findByTemplateCode(templateCodes.get(0), pageable);*/
        }
        
        // برای چندین template code، از متد بهینه شده با IN clause استفاده می‌کنیم
       /* return invoiceRepository.findByTemplateCodeIn(templateCodes, pageable);*/
        return null;
    }

    /**
     * ساخت Sort object
     */
    private Sort buildSort(String sortBy, String direction) {
        Sort.Direction sortDirection = Sort.Direction.ASC;
        if (direction != null && direction.trim().equalsIgnoreCase("DESC")) {
            sortDirection = Sort.Direction.DESC;
        }
        
        String validSortBy = sortBy != null && !sortBy.isBlank() ? sortBy : "createdOn";
        return Sort.by(sortDirection, validSortBy);
    }

    /**
     * Invalidate cache برای یک TemplateType
     * استفاده از @CacheEvict با allEntries=true برای پاک کردن تمام cache entries
     */
    @CacheEvict(cacheName = "invoices-by-template-type", allEntries = true)
    @CacheEvict(cacheName = "invoices-by-template-type-list", allEntries = true)
    @CacheEvict(cacheName = "invoice-count-by-template-type", allEntries = true)
    public void invalidateCache(String templateTypeCode) {
        logger.debug("Cache invalidated for templateTypeCode: {}", templateTypeCode);
    }

    /**
     * Invalidate تمام cache ها
     */
    @CacheEvict(cacheName = "invoices-by-template-type", allEntries = true)
    @CacheEvict(cacheName = "invoices-by-template-type-list", allEntries = true)
    @CacheEvict(cacheName = "invoice-count-by-template-type", allEntries = true)
    public void invalidateAllCache() {
        logger.info("All invoice by template type caches invalidated");
    }
}


