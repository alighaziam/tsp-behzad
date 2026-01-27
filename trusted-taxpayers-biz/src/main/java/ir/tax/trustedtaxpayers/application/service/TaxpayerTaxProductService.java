package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.core.service.BaseServiceImpl;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxProductEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerTaxProductEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxProductRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerTaxProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * سرویس برای مدیریت رابطه بین Taxpayer و TaxProduct
 */
@Service
public class TaxpayerTaxProductService extends BaseServiceImpl<TaxpayerTaxProductEntity, TaxpayerTaxProductEntity.Pk> {
    
    private final TaxpayerTaxProductRepository taxpayerTaxProductRepository;
    private final TaxProductRepository taxProductRepository;
    
    public TaxpayerTaxProductService(TaxpayerTaxProductRepository repository, TaxProductRepository taxProductRepository) {
        super(repository);
        this.taxpayerTaxProductRepository = repository;
        this.taxProductRepository = taxProductRepository;
    }
    
    /**
     * یافتن تمام محصولات مالیاتی یک مودی مالیاتی
     * 
     * @param taxpayerId شناسه مودی مالیاتی
     * @return لیست محصولات مالیاتی
     */
    @Transactional(readOnly = true)
    public List<TaxProductEntity> findTaxProductsByTaxpayerId(UUID taxpayerId) {
        List<TaxpayerTaxProductEntity> relations = taxpayerTaxProductRepository.findByTaxpayerId(taxpayerId);
        
        // استخراج لیست شناسه‌های محصولات مالیاتی
        List<UUID> taxProductIds = relations.stream()
                .map(TaxpayerTaxProductEntity::getTaxProductId)
                .collect(Collectors.toList());
        
        // واکشی محصولات مالیاتی
        if (taxProductIds.isEmpty()) {
            return List.of();
        }
        
        return taxProductRepository.findAllById(taxProductIds);
    }
    
    /**
     * ایجاد رابطه بین مودی مالیاتی و محصول مالیاتی
     * 
     * @param taxpayerId شناسه مودی مالیاتی
     * @param taxProductId شناسه محصول مالیاتی
     * @return رابطه ایجاد شده
     */
    @Transactional
    public TaxpayerTaxProductEntity createRelation(UUID taxpayerId, UUID taxProductId) {
        // بررسی وجود رابطه قبلی
        var existing = taxpayerTaxProductRepository.findByTaxpayerIdAndTaxProductId(taxpayerId, taxProductId);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // ایجاد رابطه جدید
        TaxpayerTaxProductEntity entity = TaxpayerTaxProductEntity.builder()
                .taxpayerId(taxpayerId)
                .taxProductId(taxProductId)
                .build();
        
        return save(entity);
    }
    
    /**
     * ایجاد روابط متعدد بین مودی مالیاتی و لیستی از محصولات مالیاتی (bulk)
     * از فیلد transient taxProductIds در entity استفاده می‌کند
     * 
     * @param entity Entity حاوی taxpayerId و taxProductIds (transient)
     * @return لیست روابط ایجاد شده
     */
    @Transactional
    public List<TaxpayerTaxProductEntity> bulk(TaxpayerTaxProductEntity entity) {
        if (entity == null || entity.getTaxProductIds() == null || entity.getTaxProductIds().isEmpty()) {
            return List.of();
        }
        
        UUID taxpayerId = entity.getTaxpayerId();
        List<UUID> taxProductIds = entity.getTaxProductIds();
        
        List<TaxpayerTaxProductEntity> result = new java.util.ArrayList<>();
        for (UUID taxProductId : taxProductIds) {
            TaxpayerTaxProductEntity relation = createRelation(taxpayerId, taxProductId);
            result.add(relation);
        }
        return result;
    }
    
    /**
     * حذف رابطه بین مودی مالیاتی و محصول مالیاتی
     * 
     * @param taxpayerId شناسه مودی مالیاتی
     * @param taxProductId شناسه محصول مالیاتی
     */
    @Transactional
    public void deleteRelation(UUID taxpayerId, UUID taxProductId) {
        taxpayerTaxProductRepository.findByTaxpayerIdAndTaxProductId(taxpayerId, taxProductId)
                .ifPresent(entity -> {
                    TaxpayerTaxProductEntity.Pk pk = new TaxpayerTaxProductEntity.Pk(
                            entity.getTaxpayerId(), 
                            entity.getTaxProductId()
                    );
                    deleteById(pk);
                });
    }
}

