package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerTaxProductEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository برای TaxpayerTaxProductEntity
 */
public interface TaxpayerTaxProductRepository extends BaseRepository<TaxpayerTaxProductEntity, TaxpayerTaxProductEntity.Pk> {
    
    /**
     * یافتن تمام محصولات مالیاتی یک مودی مالیاتی
     * 
     * @param taxpayerId شناسه مودی مالیاتی
     * @return لیست رابطه‌های TaxpayerTaxProduct
     */
    @Query("SELECT ttp FROM TaxpayerTaxProductEntity ttp WHERE ttp.taxpayerId = :taxpayerId")
    List<TaxpayerTaxProductEntity> findByTaxpayerId(@Param("taxpayerId") UUID taxpayerId);
    
    /**
     * یافتن تمام مودیان مالیاتی یک محصول مالیاتی
     * 
     * @param taxProductId شناسه محصول مالیاتی
     * @return لیست رابطه‌های TaxpayerTaxProduct
     */
    @Query("SELECT ttp FROM TaxpayerTaxProductEntity ttp WHERE ttp.taxProductId = :taxProductId")
    List<TaxpayerTaxProductEntity> findByTaxProductId(@Param("taxProductId") UUID taxProductId);
    
    /**
     * بررسی وجود رابطه بین مودی مالیاتی و محصول مالیاتی
     * 
     * @param taxpayerId شناسه مودی مالیاتی
     * @param taxProductId شناسه محصول مالیاتی
     * @return وجود یا عدم وجود رابطه
     */
    @Query("SELECT ttp FROM TaxpayerTaxProductEntity ttp WHERE ttp.taxpayerId = :taxpayerId AND ttp.taxProductId = :taxProductId")
    Optional<TaxpayerTaxProductEntity> findByTaxpayerIdAndTaxProductId(
            @Param("taxpayerId") UUID taxpayerId, 
            @Param("taxProductId") UUID taxProductId);
}

