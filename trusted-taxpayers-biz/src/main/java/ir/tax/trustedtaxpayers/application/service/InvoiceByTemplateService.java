/*
package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.cache.CacheEvict;
import ir.tax.trustedtaxpayers.application.cache.Cacheable;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceHeaderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

*/
/**
 * Service for loading invoices by Template
 * این سرویس برای لود کردن صورتحساب‌ها بر اساس الگو (Template) استفاده می‌شود.
 * 
 * ویژگی‌ها:
 * - Cache برای performance بهتر
 * - Pagination support
 * - Eager loading برای جلوگیری از N+1
 * - فیلتر بر اساس Template code
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 *//*

@Service
public class InvoiceByTemplateService {
    
    private static final Logger logger = LoggerFactory.getLogger(InvoiceByTemplateService.class);
    
    private final ElectronicInvoiceHeaderRepository invoiceRepository;
    
    public InvoiceByTemplateService(ElectronicInvoiceHeaderRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    */
/**
     * لود کردن صورتحساب‌ها بر اساس Template code با pagination
     * 
     * @param templateCode کد الگو (مثل T01RETAIL, T01WHOLE, ...)
     * @param page شماره صفحه (0-based)
     * @param size تعداد آیتم در هر صفحه
     * @param sortBy فیلد برای sort
     * @param direction جهت sort (ASC/DESC)
     * @return Page از صورتحساب‌ها
     *//*

    @Cacheable(cacheName = "invoices-by-template", keyPrefix = "template-page")
    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> loadInvoicesByTemplate(
            String templateCode,
            Integer page,
            Integer size,
            String sortBy,
            String direction) {
        
        if (templateCode == null || templateCode.isBlank()) {
            logger.warn("TemplateCode is null or empty, returning empty page");
            return Page.empty();
        }
        
        // ساخت Pageable
        Sort sort = buildSort(sortBy, direction);
        int validPage = page != null && page >= 0 ? page : 0;
        int validSize = size != null && size > 0 ? Math.min(size, 100) : 20;
        Pageable pageable = PageRequest.of(validPage, validSize, sort);
        
        Page<ElectronicInvoiceHeaderEntity> invoices = invoiceRepository.findByTemplateCode(templateCode, pageable);
        
        logger.debug("Loaded {} invoices for templateCode={}, page={}, size={}", 
            invoices.getTotalElements(), templateCode, page, size);
        
        return invoices;
    }

    */
/**
     * لود کردن صورتحساب‌ها بر اساس Template code (بدون pagination)
     * 
     * @param templateCode کد الگو
     * @return لیست صورتحساب‌ها
     *//*

    @Cacheable(cacheName = "invoices-by-template-list", keyPrefix = "template")
    @Transactional(readOnly = true)
    public List<ElectronicInvoiceHeaderEntity> loadInvoicesByTemplate(String templateCode) {
        if (templateCode == null || templateCode.isBlank()) {
            return List.of();
        }
        
        // Using paginated version instead of deprecated method
        return invoiceRepository.findByTemplateCode(templateCode, Pageable.unpaged()).getContent();
    }

    */
/**
     * تعداد صورتحساب‌ها بر اساس Template code
     * 
     * @param templateCode کد الگو
     * @return تعداد صورتحساب‌ها
     *//*

    @Cacheable(cacheName = "invoice-count-by-template", keyPrefix = "count")
    @Transactional(readOnly = true)
    public long countInvoicesByTemplate(String templateCode) {
        if (templateCode == null || templateCode.isBlank()) {
            return 0;
        }
        
        return invoiceRepository.countByTemplateCode(templateCode);
    }

    */
/**
     * لود کردن صورتحساب‌ها بر اساس Template code و inty
     * 
     * @param templateCode کد الگو
     * @param inty نوع صورتحساب
     * @param page شماره صفحه
     * @param size تعداد آیتم
     * @param sortBy فیلد sort
     * @param direction جهت sort
     * @return Page از صورتحساب‌ها
     *//*

    @Cacheable(cacheName = "invoices-by-template-inty", keyPrefix = "template-inty-page")
    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> loadInvoicesByTemplateAndInty(
            String templateCode,
            Integer inty,
            Integer page,
            Integer size,
            String sortBy,
            String direction) {
        
        if (templateCode == null || templateCode.isBlank() || inty == null) {
            logger.warn("TemplateCode or inty is null, returning empty page");
            return Page.empty();
        }
        
        Sort sort = buildSort(sortBy, direction);
        int validPage = page != null && page >= 0 ? page : 0;
        int validSize = size != null && size > 0 ? Math.min(size, 100) : 20;
        Pageable pageable = PageRequest.of(validPage, validSize, sort);
        
        Page<ElectronicInvoiceHeaderEntity> invoices = invoiceRepository.findByTemplateCodeAndInty(templateCode, inty, pageable);
        
        logger.debug("Loaded {} invoices for templateCode={}, inty={}, page={}, size={}", 
            invoices.getTotalElements(), templateCode, inty, page, size);
        
        return invoices;
    }

    */
/**
     * ساخت Sort object
     *//*

    private Sort buildSort(String sortBy, String direction) {
        Sort.Direction sortDirection = Sort.Direction.ASC;
        if (direction != null && direction.trim().equalsIgnoreCase("DESC")) {
            sortDirection = Sort.Direction.DESC;
        }
        
        String validSortBy = sortBy != null && !sortBy.isBlank() ? sortBy : "createdOn";
        return Sort.by(sortDirection, validSortBy);
    }

    */
/**
     * Invalidate cache برای یک Template
     *//*

    @CacheEvict(cacheName = "invoices-by-template", allEntries = true)
    @CacheEvict(cacheName = "invoices-by-template-list", allEntries = true)
    @CacheEvict(cacheName = "invoice-count-by-template", allEntries = true)
    @CacheEvict(cacheName = "invoices-by-template-inty", allEntries = true)
    public void invalidateCache(String templateCode) {
        logger.debug("Cache invalidated for templateCode: {}", templateCode);
    }

    */
/**
     * Invalidate تمام cache ها
     *//*

    @CacheEvict(cacheName = "invoices-by-template", allEntries = true)
    @CacheEvict(cacheName = "invoices-by-template-list", allEntries = true)
    @CacheEvict(cacheName = "invoice-count-by-template", allEntries = true)
    @CacheEvict(cacheName = "invoices-by-template-inty", allEntries = true)
    public void invalidateAllCache() {
        logger.info("All invoice by template caches invalidated");
    }
}


*/
