package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerBuyerEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository برای TaxpayerBuyerEntity
 */
public interface TaxpayerBuyerRepository extends BaseRepository<TaxpayerBuyerEntity, TaxpayerBuyerEntity.Pk> {
    
    /**
     * یافتن تمام خریداران یک مودی مالیاتی
     * 
     * @param taxpayerId شناسه مودی مالیاتی (فروشنده)
     * @return لیست رابطه‌های TaxpayerBuyer
     */
    @Query("SELECT tb FROM TaxpayerBuyerEntity tb WHERE tb.taxpayerId = :taxpayerId")
    List<TaxpayerBuyerEntity> findByTaxpayerId(@Param("taxpayerId") UUID taxpayerId);
    
    /**
     * یافتن تمام فروشندگان یک خریدار
     * 
     * @param buyerId شناسه خریدار
     * @return لیست رابطه‌های TaxpayerBuyer
     */
    @Query("SELECT tb FROM TaxpayerBuyerEntity tb WHERE tb.buyerId = :buyerId")
    List<TaxpayerBuyerEntity> findByBuyerId(@Param("buyerId") UUID buyerId);
    
    /**
     * بررسی وجود رابطه بین مودی مالیاتی و خریدار
     * 
     * @param taxpayerId شناسه مودی مالیاتی (فروشنده)
     * @param buyerId شناسه خریدار
     * @return وجود یا عدم وجود رابطه
     */
    @Query("SELECT tb FROM TaxpayerBuyerEntity tb WHERE tb.taxpayerId = :taxpayerId AND tb.buyerId = :buyerId")
    Optional<TaxpayerBuyerEntity> findByTaxpayerIdAndBuyerId(
            @Param("taxpayerId") UUID taxpayerId, 
            @Param("buyerId") UUID buyerId);
}






