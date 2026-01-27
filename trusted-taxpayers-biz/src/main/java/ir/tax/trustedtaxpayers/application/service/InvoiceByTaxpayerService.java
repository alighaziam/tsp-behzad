package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ElectronicInvoiceHeaderRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Service for loading invoices by Taxpayer (مودی)
 * این سرویس برای لود کردن صورتحساب‌ها بر اساس مودی مالیاتی استفاده می‌شود.
 * 
 * ویژگی‌ها:
 * - Cache برای performance بهتر
 * - Pagination support
 * - Eager loading برای جلوگیری از N+1
 * - فیلتر بر اساس Taxpayer ID یا Taxpayer No
 * - گزارش صورتحساب‌ها به عنوان فروشنده و/یا خریدار
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Service
public class InvoiceByTaxpayerService {
    
    private static final Logger logger = LoggerFactory.getLogger(InvoiceByTaxpayerService.class);
    
    private final ElectronicInvoiceHeaderRepository invoiceRepository;
    private final TaxpayerRepository taxpayerRepository;
    private final CacheService cacheService;
    
    public InvoiceByTaxpayerService(
            ElectronicInvoiceHeaderRepository invoiceRepository,
            TaxpayerRepository taxpayerRepository,
            CacheService cacheService) {
        this.invoiceRepository = invoiceRepository;
        this.taxpayerRepository = taxpayerRepository;
        this.cacheService = cacheService;
    }

    /**
     * لود کردن صورتحساب‌ها بر اساس Taxpayer ID (به عنوان فروشنده یا خریدار) با pagination
     * 
     * @param taxpayerId شناسه مودی مالیاتی
     * @param page شماره صفحه (0-based)
     * @param size تعداد آیتم در هر صفحه
     * @param sortBy فیلد برای sort
     * @param direction جهت sort (ASC/DESC)
     * @return Page از صورتحساب‌ها
     */
    /*@Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> loadInvoicesByTaxpayer(
            UUID taxpayerId,
            Integer page,
            Integer size,
            String sortBy,
            String direction) {
        
        if (taxpayerId == null) {
            logger.warn("TaxpayerId is null, returning empty page");
            return Page.empty();
        }
        
        // استفاده از cache برای performance بهتر
        String cacheKey = buildCacheKey("taxpayer", taxpayerId, page, size, sortBy, direction);
        
        Page<ElectronicInvoiceHeaderEntity> invoices = cacheService.get("invoices-by-taxpayer", 
            cacheKey, () -> {
                Sort sort = buildSort(sortBy, direction);
                int validPage = page != null && page >= 0 ? page : 0;
                int validSize = size != null && size > 0 ? Math.min(size, 100) : 20;
                Pageable pageable = PageRequest.of(validPage, validSize, sort);
                
                return invoiceRepository.findBySellerTaxpayerIdOrBuyerTaxpayerId(
                        taxpayerId,
                        taxpayerId,
                        pageable
                );
            });
        
        logger.debug("Loaded {} invoices for taxpayerId={}, page={}, size={}", 
            invoices.getTotalElements(), taxpayerId, page, size);
        
        return invoices;
    }*/

    /**
     * لود کردن صورتحساب‌ها بر اساس Taxpayer No (شماره اقتصادی) با pagination
     * ابتدا مودی را پیدا می‌کند، سپس صورتحساب‌ها را برمی‌گرداند
     * 
     * @param taxpayerNo شماره اقتصادی مودی
     * @param page شماره صفحه (0-based)
     * @param size تعداد آیتم در هر صفحه
     * @param sortBy فیلد برای sort
     * @param direction جهت sort (ASC/DESC)
     * @return Page از صورتحساب‌ها
     */
    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> loadInvoicesByTaxpayerNo(
            String taxpayerNo,
            Integer page,
            Integer size,
            String sortBy,
            String direction) {
        
        if (taxpayerNo == null || taxpayerNo.isBlank()) {
            logger.warn("TaxpayerNo is null or empty, returning empty page");
            return Page.empty();
        }
        
        // پیدا کردن مودی بر اساس شماره اقتصادی
        TaxpayerEntity taxpayer = taxpayerRepository.findByTaxId(taxpayerNo)
            .orElse(null);
        
        if (taxpayer == null) {
            logger.warn("Taxpayer not found for taxpayerNo: {}", taxpayerNo);
            return Page.empty();
        }
        
       /* return loadInvoicesByTaxpayer(taxpayer.getId(), page, size, sortBy, direction);*/
        return null;
    }

    /**
     * لود کردن تمام صورتحساب‌ها بر اساس Taxpayer ID (به عنوان فروشنده و خریدار)
     * این متد لیست کامل را بدون pagination برمی‌گرداند
     * 
     * @param taxpayerId شناسه مودی مالیاتی
     * @return لیست صورتحساب‌ها
     */
    @Transactional(readOnly = true)
    public List<ElectronicInvoiceHeaderEntity> loadAllInvoicesByTaxpayer(UUID taxpayerId) {
        if (taxpayerId == null) {
            return Collections.emptyList();
        }
        
        // استفاده از cache
        String cacheKey = "list:taxpayer:" + taxpayerId;
        /*return cacheService.get("invoices-by-taxpayer-list", cacheKey, () -> {
            return new ArrayList<>(
                    invoiceRepository.findBySellerTaxpayerIdOrBuyerTaxpayerId(taxpayerId, taxpayerId)
            );
        });*/
        return null;
    }

    /**
     * لود کردن تمام صورتحساب‌ها بر اساس Taxpayer No
     * 
     * @param taxpayerNo شماره اقتصادی مودی
     * @return لیست صورتحساب‌ها
     */
    @Transactional(readOnly = true)
    public List<ElectronicInvoiceHeaderEntity> loadAllInvoicesByTaxpayerNo(String taxpayerNo) {
        if (taxpayerNo == null || taxpayerNo.isBlank()) {
            return Collections.emptyList();
        }
        
        TaxpayerEntity taxpayer = taxpayerRepository.findByTaxId(taxpayerNo)
            .orElse(null);
        
        if (taxpayer == null) {
            return Collections.emptyList();
        }
        
        return loadAllInvoicesByTaxpayer(taxpayer.getId());
    }

    /**
     * لود کردن صورتحساب‌ها بر اساس Taxpayer ID به عنوان فروشنده با pagination
     * 
     * @param taxpayerId شناسه مودی مالیاتی
     * @param page شماره صفحه
     * @param size تعداد آیتم
     * @param sortBy فیلد sort
     * @param direction جهت sort
     * @return Page از صورتحساب‌ها
     */
    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> loadInvoicesAsSeller(
            UUID taxpayerId,
            Integer page,
            Integer size,
            String sortBy,
            String direction) {
        
        if (taxpayerId == null) {
            logger.warn("TaxpayerId is null, returning empty page");
            return Page.empty();
        }
        
        String cacheKey = buildCacheKey("seller", taxpayerId, page, size, sortBy, direction);
        
        /*Page<ElectronicInvoiceHeaderEntity> invoices = cacheService.get("invoices-by-taxpayer-as-seller",
            cacheKey, () -> {
                Sort sort = buildSort(sortBy, direction);
                int validPage = page != null && page >= 0 ? page : 0;
                int validSize = size != null && size > 0 ? Math.min(size, 100) : 20;
                Pageable pageable = PageRequest.of(validPage, validSize, sort);
                
                return invoiceRepository.findBySellerTaxpayerId(taxpayerId, pageable);
            });*/
        
        /*logger.debug("Loaded {} invoices as seller for taxpayerId={}, page={}, size={}",
            invoices.getTotalElements(), taxpayerId, page, size);
        
        return invoices;*/

        return null;
    }

    /**
     * لود کردن صورتحساب‌ها بر اساس Taxpayer ID به عنوان خریدار با pagination
     * 
     * @param taxpayerId شناسه مودی مالیاتی
     * @param page شماره صفحه
     * @param size تعداد آیتم
     * @param sortBy فیلد sort
     * @param direction جهت sort
     * @return Page از صورتحساب‌ها
     */
    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> loadInvoicesAsBuyer(
            UUID taxpayerId,
            Integer page,
            Integer size,
            String sortBy,
            String direction) {
        
        if (taxpayerId == null) {
            logger.warn("TaxpayerId is null, returning empty page");
            return Page.empty();
        }
        
        String cacheKey = buildCacheKey("buyer", taxpayerId, page, size, sortBy, direction);
        
        Page<ElectronicInvoiceHeaderEntity> invoices = cacheService.get("invoices-by-taxpayer-as-buyer", 
            cacheKey, () -> {
                Sort sort = buildSort(sortBy, direction);
                int validPage = page != null && page >= 0 ? page : 0;
                int validSize = size != null && size > 0 ? Math.min(size, 100) : 20;
                Pageable pageable = PageRequest.of(validPage, validSize, sort);
                
               /* return invoiceRepository.findByBuyerTaxpayerId(taxpayerId, pageable);*/
                    return null;
            });
        
        logger.debug("Loaded {} invoices as buyer for taxpayerId={}, page={}, size={}", 
            invoices.getTotalElements(), taxpayerId, page, size);
        
        return invoices;
    }

    /**
     * تعداد صورتحساب‌ها بر اساس Taxpayer ID (به عنوان فروشنده و خریدار)
     * 
     * @param taxpayerId شناسه مودی مالیاتی
     * @return تعداد کل صورتحساب‌ها
     */
    @Transactional(readOnly = true)
    public long countInvoicesByTaxpayer(UUID taxpayerId) {
        if (taxpayerId == null) {
            return 0;
        }
        
        String cacheKey = "count:taxpayer:" + taxpayerId;
        /*Long count = cacheService.get("invoice-count-by-taxpayer", cacheKey, () ->
            invoiceRepository.countBySellerTaxpayerId(taxpayerId)
                    + invoiceRepository.countByBuyerTaxpayerId(taxpayerId)
        );*/
        Long count = 1L;
        
        return count != null ? count : 0;
    }

    /**
     * تعداد صورتحساب‌ها بر اساس Taxpayer No
     * 
     * @param taxpayerNo شماره اقتصادی مودی
     * @return تعداد کل صورتحساب‌ها
     */
    @Transactional(readOnly = true)
    public long countInvoicesByTaxpayerNo(String taxpayerNo) {
        if (taxpayerNo == null || taxpayerNo.isBlank()) {
            return 0;
        }
        
        TaxpayerEntity taxpayer = taxpayerRepository.findByTaxId(taxpayerNo)
            .orElse(null);
        
        if (taxpayer == null) {
            return 0;
        }
        
        return countInvoicesByTaxpayer(taxpayer.getId());
    }

    /**
     * تعداد صورتحساب‌ها به عنوان فروشنده
     * 
     * @param taxpayerId شناسه مودی مالیاتی
     * @return تعداد صورتحساب‌ها
     */
    @Transactional(readOnly = true)
    public long countInvoicesAsSeller(UUID taxpayerId) {
        if (taxpayerId == null) {
            return 0;
        }
        
        String cacheKey = "count:seller:" + taxpayerId;
        /*Long count = cacheService.get("invoice-count-by-taxpayer-as-seller", cacheKey, () ->
            invoiceRepository.countBySellerTaxpayerId(taxpayerId)
        );*/
        Long count =1L;
        
        return count != null ? count : 0;
    }

    /**
     * تعداد صورتحساب‌ها به عنوان خریدار
     * 
     * @param taxpayerId شناسه مودی مالیاتی
     * @return تعداد صورتحساب‌ها
     */
    @Transactional(readOnly = true)
    public long countInvoicesAsBuyer(UUID taxpayerId) {
        if (taxpayerId == null) {
            return 0;
        }
        
        String cacheKey = "count:buyer:" + taxpayerId;
        /*Long count = cacheService.get("invoice-count-by-taxpayer-as-buyer", cacheKey, () ->
            invoiceRepository.countByBuyerTaxpayerId(taxpayerId)
        );*/
        Long count =1L;
        return count != null ? count : 0;
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
     * ساخت Cache Key
     */
    private String buildCacheKey(String type, UUID id, Integer page, Integer size, 
                                 String sortBy, String direction) {
        return String.format("%s:%s:page:%d:size:%d:sort:%s:dir:%s", 
            type, id, page, size, sortBy, direction);
    }

    /**
     * Invalidate cache برای یک Taxpayer
     */
    public void invalidateCache(UUID taxpayerId) {
        cacheService.evictAll("invoices-by-taxpayer");
        cacheService.evictAll("invoices-by-taxpayer-list");
        cacheService.evictAll("invoice-count-by-taxpayer");
        cacheService.evictAll("invoices-by-taxpayer-as-seller");
        cacheService.evictAll("invoices-by-taxpayer-as-buyer");
        cacheService.evictAll("invoice-count-by-taxpayer-as-seller");
        cacheService.evictAll("invoice-count-by-taxpayer-as-buyer");
        logger.debug("Cache invalidated for taxpayerId: {}", taxpayerId);
    }

    /**
     * Invalidate cache برای یک Taxpayer No
     */
    public void invalidateCacheByTaxpayerNo(String taxpayerNo) {
        if (taxpayerNo == null || taxpayerNo.isBlank()) {
            return;
        }
        
        TaxpayerEntity taxpayer = taxpayerRepository.findByTaxId(taxpayerNo).orElse(null);
        if (taxpayer != null) {
            invalidateCache(taxpayer.getId());
        }
    }

    /**
     * Invalidate تمام cache ها
     */
    public void invalidateAllCache() {
        cacheService.evictAll("invoices-by-taxpayer");
        cacheService.evictAll("invoices-by-taxpayer-list");
        cacheService.evictAll("invoice-count-by-taxpayer");
        cacheService.evictAll("invoices-by-taxpayer-as-seller");
        cacheService.evictAll("invoices-by-taxpayer-as-buyer");
        cacheService.evictAll("invoice-count-by-taxpayer-as-seller");
        cacheService.evictAll("invoice-count-by-taxpayer-as-buyer");
        logger.info("All invoice by taxpayer caches invalidated");
    }
}


