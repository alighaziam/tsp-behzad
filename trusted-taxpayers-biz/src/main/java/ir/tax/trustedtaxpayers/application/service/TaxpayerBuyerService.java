package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.core.service.BaseServiceImpl;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerBuyerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerBuyerRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * سرویس برای مدیریت رابطه بین Taxpayer (فروشنده) و Buyer (خریدار)
 */
@Service
public class TaxpayerBuyerService extends BaseServiceImpl<TaxpayerBuyerEntity, TaxpayerBuyerEntity.Pk> {
    
    private final TaxpayerBuyerRepository taxpayerBuyerRepository;
    private final TaxpayerRepository taxpayerRepository;
    
    public TaxpayerBuyerService(TaxpayerBuyerRepository repository, TaxpayerRepository taxpayerRepository) {
        super(repository);
        this.taxpayerBuyerRepository = repository;
        this.taxpayerRepository = taxpayerRepository;
    }
    
    /**
     * یافتن تمام خریداران یک مودی مالیاتی
     * 
     * @param taxpayerId شناسه مودی مالیاتی (فروشنده)
     * @return لیست خریداران (TaxpayerEntity)
     */
    @Transactional(readOnly = true)
    public List<TaxpayerEntity> findBuyersByTaxpayerId(UUID taxpayerId) {
        List<TaxpayerBuyerEntity> relations = taxpayerBuyerRepository.findByTaxpayerId(taxpayerId);
        List<UUID> buyerIds = relations.stream()
                .map(TaxpayerBuyerEntity::getBuyerId)
                .collect(Collectors.toList());
        if (buyerIds.isEmpty()) {
            return List.of();
        }
        return taxpayerRepository.findAllById(buyerIds);
    }
    
    /**
     * یافتن تمام فروشندگان یک خریدار
     * 
     * @param buyerId شناسه خریدار
     * @return لیست فروشندگان (TaxpayerEntity)
     */
    @Transactional(readOnly = true)
    public List<TaxpayerEntity> findTaxpayersByBuyerId(UUID buyerId) {
        List<TaxpayerBuyerEntity> relations = taxpayerBuyerRepository.findByBuyerId(buyerId);
        
        // استخراج لیست شناسه‌های فروشندگان
        List<UUID> taxpayerIds = relations.stream()
                .map(TaxpayerBuyerEntity::getTaxpayerId)
                .collect(Collectors.toList());
        
        // واکشی فروشندگان
        if (taxpayerIds.isEmpty()) {
            return List.of();
        }
        
        return taxpayerRepository.findAllById(taxpayerIds);
    }
    
    /**
     * ایجاد رابطه بین مودی مالیاتی (فروشنده) و خریدار
     * 
     * @param taxpayerId شناسه مودی مالیاتی (فروشنده)
     * @param buyerId شناسه خریدار
     * @return رابطه ایجاد شده
     */
    @Transactional
    public TaxpayerBuyerEntity createRelation(UUID taxpayerId, UUID buyerId) {
        // بررسی وجود رابطه قبلی
        var existing = taxpayerBuyerRepository.findByTaxpayerIdAndBuyerId(taxpayerId, buyerId);
        if (existing.isPresent()) {
            return existing.get();
        }
        
        // ایجاد رابطه جدید
        TaxpayerBuyerEntity entity = TaxpayerBuyerEntity.builder()
                .taxpayerId(taxpayerId)
                .buyerId(buyerId)
                .build();
        
        return save(entity);
    }
    
    /**
     * حذف رابطه بین مودی مالیاتی و خریدار
     * 
     * @param taxpayerId شناسه مودی مالیاتی (فروشنده)
     * @param buyerId شناسه خریدار
     */
    @Transactional
    public void deleteRelation(UUID taxpayerId, UUID buyerId) {
        taxpayerBuyerRepository.findByTaxpayerIdAndBuyerId(taxpayerId, buyerId)
                .ifPresent(entity -> {
                    TaxpayerBuyerEntity.Pk pk = new TaxpayerBuyerEntity.Pk(
                            entity.getTaxpayerId(), 
                            entity.getBuyerId()
                    );
                    deleteById(pk);
                });
    }
}






