/*
package ir.tax.trustedtaxpayers.application.service;

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

import java.util.Collections;
import java.util.List;
import java.util.UUID;

*/
/**
 * Service for loading invoices by Buyer and Seller
 * این سرویس برای لود کردن صورتحساب‌ها بر اساس خریدار و فروشنده استفاده می‌شود.
 * 
 * ویژگی‌ها:
 * - Cache برای performance بهتر
 * - Pagination support
 * - Eager loading برای جلوگیری از N+1
 * - فیلتر بر اساس Buyer ID و Seller ID
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 *//*

@Service
public class InvoiceByBuyerAndSellerService {
    
    private static final Logger logger = LoggerFactory.getLogger(InvoiceByBuyerAndSellerService.class);
    
    private final ElectronicInvoiceHeaderRepository invoiceRepository;
    private final CacheService cacheService;
    
    public InvoiceByBuyerAndSellerService(
            ElectronicInvoiceHeaderRepository invoiceRepository,
            CacheService cacheService) {
        this.invoiceRepository = invoiceRepository;
        this.cacheService = cacheService;
    }

    */
/**
     * لود کردن صورتحساب‌ها بر اساس Seller Taxpayer ID با pagination
     * 
     * @param sellerTaxpayerId شناسه مودی فروشنده
     * @param page شماره صفحه (0-based)
     * @param size تعداد آیتم در هر صفحه
     * @param sortBy فیلد برای sort
     * @param direction جهت sort (ASC/DESC)
     * @return Page از صورتحساب‌ها
     *//*

    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> loadInvoicesBySeller(
            UUID sellerTaxpayerId,
            Integer page,
            Integer size,
            String sortBy,
            String direction) {
        
        if (sellerTaxpayerId == null) {
            logger.warn("SellerTaxpayerId is null, returning empty page");
            return Page.empty();
        }
        
        // استفاده از cache برای performance بهتر
        String cacheKey = buildCacheKey("seller", sellerTaxpayerId, page, size, sortBy, direction);
        
        Page<ElectronicInvoiceHeaderEntity> invoices = cacheService.get("invoices-by-seller", 
            cacheKey, () -> {
                // ساخت Pageable
                Sort sort = buildSort(sortBy, direction);
                int validPage = page != null && page >= 0 ? page : 0;
                int validSize = size != null && size > 0 ? Math.min(size, 100) : 20;
                Pageable pageable = PageRequest.of(validPage, validSize, sort);
                
                return invoiceRepository.findBySellerTaxpayerId(sellerTaxpayerId, pageable);
            });
        
        logger.debug("Loaded {} invoices for sellerTaxpayerId={}, page={}, size={}", 
            invoices.getTotalElements(), sellerTaxpayerId, page, size);
        
        return invoices;
    }

    */
/**
     * لود کردن صورتحساب‌ها بر اساس Buyer Taxpayer ID با pagination
     * 
     * @param buyerTaxpayerId شناسه مودی خریدار
     * @param page شماره صفحه (0-based)
     * @param size تعداد آیتم در هر صفحه
     * @param sortBy فیلد برای sort
     * @param direction جهت sort (ASC/DESC)
     * @return Page از صورتحساب‌ها
     *//*

    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> loadInvoicesByBuyer(
            UUID buyerTaxpayerId,
            Integer page,
            Integer size,
            String sortBy,
            String direction) {
        
        if (buyerTaxpayerId == null) {
            logger.warn("BuyerTaxpayerId is null, returning empty page");
            return Page.empty();
        }
        
        // استفاده از cache برای performance بهتر
        String cacheKey = buildCacheKey("buyer", buyerTaxpayerId, page, size, sortBy, direction);
        
        Page<ElectronicInvoiceHeaderEntity> invoices = cacheService.get("invoices-by-buyer", 
            cacheKey, () -> {
                // ساخت Pageable
                Sort sort = buildSort(sortBy, direction);
                int validPage = page != null && page >= 0 ? page : 0;
                int validSize = size != null && size > 0 ? Math.min(size, 100) : 20;
                Pageable pageable = PageRequest.of(validPage, validSize, sort);
                
                return invoiceRepository.findByBuyerTaxpayerId(buyerTaxpayerId, pageable);
            });
        
        logger.debug("Loaded {} invoices for buyerTaxpayerId={}, page={}, size={}", 
            invoices.getTotalElements(), buyerTaxpayerId, page, size);
        
        return invoices;
    }

    */
/**
     * لود کردن صورتحساب‌ها بر اساس Seller و Buyer Taxpayer ID با pagination
     * 
     * @param sellerTaxpayerId شناسه مودی فروشنده
     * @param buyerTaxpayerId شناسه مودی خریدار
     * @param page شماره صفحه (0-based)
     * @param size تعداد آیتم در هر صفحه
     * @param sortBy فیلد برای sort
     * @param direction جهت sort (ASC/DESC)
     * @return Page از صورتحساب‌ها
     *//*

    @Transactional(readOnly = true)
    public Page<ElectronicInvoiceHeaderEntity> loadInvoicesByBuyerAndSeller(
            UUID sellerTaxpayerId,
            UUID buyerTaxpayerId,
            Integer page,
            Integer size,
            String sortBy,
            String direction) {
        
        if (sellerTaxpayerId == null && buyerTaxpayerId == null) {
            logger.warn("Both sellerTaxpayerId and buyerTaxpayerId are null, returning empty page");
            return Page.empty();
        }
        
        // اگر فقط seller داریم، از متد مربوط به seller استفاده می‌کنیم
        if (buyerTaxpayerId == null) {
            return loadInvoicesBySeller(sellerTaxpayerId, page, size, sortBy, direction);
        }
        
        // اگر فقط buyer داریم، از متد مربوط به buyer استفاده می‌کنیم
        if (sellerTaxpayerId == null) {
            return loadInvoicesByBuyer(buyerTaxpayerId, page, size, sortBy, direction);
        }
        
        // اگر هر دو داریم، از متد ترکیبی استفاده می‌کنیم
        String cacheKey = buildCacheKey("both", sellerTaxpayerId, buyerTaxpayerId, page, size, sortBy, direction);
        
        Page<ElectronicInvoiceHeaderEntity> invoices = cacheService.get("invoices-by-buyer-and-seller", 
            cacheKey, () -> {
                // ساخت Pageable
                Sort sort = buildSort(sortBy, direction);
                int validPage = page != null && page >= 0 ? page : 0;
                int validSize = size != null && size > 0 ? Math.min(size, 100) : 20;
                Pageable pageable = PageRequest.of(validPage, validSize, sort);
                
                return invoiceRepository.findBySellerTaxpayerIdAndBuyerTaxpayerId(
                    sellerTaxpayerId, buyerTaxpayerId, pageable);
            });
        
        logger.debug("Loaded {} invoices for sellerTaxpayerId={}, buyerTaxpayerId={}, page={}, size={}", 
            invoices.getTotalElements(), sellerTaxpayerId, buyerTaxpayerId, page, size);
        
        return invoices;
    }

    */
/**
     * لود کردن صورتحساب‌ها بر اساس Seller Taxpayer ID (بدون pagination)
     * 
     * @param sellerTaxpayerId شناسه مودی فروشنده
     * @return لیست صورتحساب‌ها
     *//*

    @Transactional(readOnly = true)
    public List<ElectronicInvoiceHeaderEntity> loadInvoicesBySeller(UUID sellerTaxpayerId) {
        if (sellerTaxpayerId == null) {
            return Collections.emptyList();
        }
        
        // استفاده از cache
        String cacheKey = "list:seller:" + sellerTaxpayerId;
        return cacheService.get("invoices-by-seller-list", cacheKey, () -> {
            Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
            return invoiceRepository.findBySellerTaxpayerId(sellerTaxpayerId, pageable).getContent();
        });
    }

    */
/**
     * لود کردن صورتحساب‌ها بر اساس Buyer Taxpayer ID (بدون pagination)
     * 
     * @param buyerTaxpayerId شناسه مودی خریدار
     * @return لیست صورتحساب‌ها
     *//*

    @Transactional(readOnly = true)
    public List<ElectronicInvoiceHeaderEntity> loadInvoicesByBuyer(UUID buyerTaxpayerId) {
        if (buyerTaxpayerId == null) {
            return Collections.emptyList();
        }
        
        // استفاده از cache
        String cacheKey = "list:buyer:" + buyerTaxpayerId;
        return cacheService.get("invoices-by-buyer-list", cacheKey, () -> {
            Pageable pageable = PageRequest.of(0, Integer.MAX_VALUE);
            return invoiceRepository.findByBuyerTaxpayerId(buyerTaxpayerId, pageable).getContent();
        });
    }

    */
/**
     * تعداد صورتحساب‌ها بر اساس Seller Taxpayer ID
     * 
     * @param sellerTaxpayerId شناسه مودی فروشنده
     * @return تعداد صورتحساب‌ها
     *//*

    @Transactional(readOnly = true)
    public long countInvoicesBySeller(UUID sellerTaxpayerId) {
        if (sellerTaxpayerId == null) {
            return 0;
        }
        
        // Cache key برای count
        String cacheKey = "count:seller:" + sellerTaxpayerId;
        Long count = cacheService.get("invoice-count-by-seller", cacheKey, () -> 
            invoiceRepository.countBySellerTaxpayerId(sellerTaxpayerId)
        );
        
        return count != null ? count : 0;
    }

    */
/**
     * تعداد صورتحساب‌ها بر اساس Buyer Taxpayer ID
     * 
     * @param buyerTaxpayerId شناسه مودی خریدار
     * @return تعداد صورتحساب‌ها
     *//*

    @Transactional(readOnly = true)
    public long countInvoicesByBuyer(UUID buyerTaxpayerId) {
        if (buyerTaxpayerId == null) {
            return 0;
        }
        
        // Cache key برای count
        String cacheKey = "count:buyer:" + buyerTaxpayerId;
        Long count = cacheService.get("invoice-count-by-buyer", cacheKey, () -> 
            invoiceRepository.countByBuyerTaxpayerId(buyerTaxpayerId)
        );
        
        return count != null ? count : 0;
    }

    */
/**
     * تعداد صورتحساب‌ها بر اساس Seller و Buyer Taxpayer ID
     * 
     * @param sellerTaxpayerId شناسه مودی فروشنده
     * @param buyerTaxpayerId شناسه مودی خریدار
     * @return تعداد صورتحساب‌ها
     *//*

    @Transactional(readOnly = true)
    public long countInvoicesByBuyerAndSeller(UUID sellerTaxpayerId, UUID buyerTaxpayerId) {
        if (sellerTaxpayerId == null && buyerTaxpayerId == null) {
            return 0;
        }
        
        if (buyerTaxpayerId == null) {
            return countInvoicesBySeller(sellerTaxpayerId);
        }
        
        if (sellerTaxpayerId == null) {
            return countInvoicesByBuyer(buyerTaxpayerId);
        }
        
        // Cache key برای count
        String cacheKey = "count:both:" + sellerTaxpayerId + ":" + buyerTaxpayerId;
        Long count = cacheService.get("invoice-count-by-buyer-and-seller", cacheKey, () -> 
            invoiceRepository.countBySellerTaxpayerIdAndBuyerTaxpayerId(sellerTaxpayerId, buyerTaxpayerId)
        );
        
        return count != null ? count : 0;
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
     * ساخت Cache Key برای seller یا buyer
     *//*

    private String buildCacheKey(String type, UUID id, Integer page, Integer size, 
                                 String sortBy, String direction) {
        return String.format("%s:%s:page:%d:size:%d:sort:%s:dir:%s", 
            type, id, page, size, sortBy, direction);
    }

    */
/**
     * ساخت Cache Key برای both (seller و buyer)
     *//*

    private String buildCacheKey(String type, UUID sellerId, UUID buyerId, Integer page, Integer size, 
                                 String sortBy, String direction) {
        return String.format("%s:%s:%s:page:%d:size:%d:sort:%s:dir:%s", 
            type, sellerId, buyerId, page, size, sortBy, direction);
    }

    */
/**
     * Invalidate cache برای یک Seller
     *//*

    public void invalidateSellerCache(UUID sellerTaxpayerId) {
        cacheService.evictAll("invoices-by-seller");
        cacheService.evictAll("invoices-by-seller-list");
        cacheService.evictAll("invoice-count-by-seller");
        logger.debug("Cache invalidated for sellerTaxpayerId: {}", sellerTaxpayerId);
    }

    */
/**
     * Invalidate cache برای یک Buyer
     *//*

    public void invalidateBuyerCache(UUID buyerTaxpayerId) {
        cacheService.evictAll("invoices-by-buyer");
        cacheService.evictAll("invoices-by-buyer-list");
        cacheService.evictAll("invoice-count-by-buyer");
        logger.debug("Cache invalidated for buyerTaxpayerId: {}", buyerTaxpayerId);
    }

    */
/**
     * Invalidate cache برای Buyer و Seller
     *//*

    public void invalidateBuyerAndSellerCache(UUID sellerTaxpayerId, UUID buyerTaxpayerId) {
        invalidateSellerCache(sellerTaxpayerId);
        invalidateBuyerCache(buyerTaxpayerId);
        cacheService.evictAll("invoices-by-buyer-and-seller");
        cacheService.evictAll("invoice-count-by-buyer-and-seller");
        logger.debug("Cache invalidated for sellerTaxpayerId: {}, buyerTaxpayerId: {}", 
            sellerTaxpayerId, buyerTaxpayerId);
    }

    */
/**
     * Invalidate تمام cache ها
     *//*

    public void invalidateAllCache() {
        cacheService.evictAll("invoices-by-seller");
        cacheService.evictAll("invoices-by-seller-list");
        cacheService.evictAll("invoice-count-by-seller");
        cacheService.evictAll("invoices-by-buyer");
        cacheService.evictAll("invoices-by-buyer-list");
        cacheService.evictAll("invoice-count-by-buyer");
        cacheService.evictAll("invoices-by-buyer-and-seller");
        cacheService.evictAll("invoice-count-by-buyer-and-seller");
        logger.info("All invoice by buyer and seller caches invalidated");
    }
}


*/
