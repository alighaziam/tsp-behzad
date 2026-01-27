package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;
import ir.tax.trustedtaxpayers.enums.ProductTaxable;
import ir.tax.trustedtaxpayers.enums.TaxProductType;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxProductEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository برای TaxProductEntity
 */
public interface TaxProductRepository extends BaseRepository<TaxProductEntity, UUID> {
    
    /**
     * یافتن محصولات مالیاتی بر اساس نوع مشمولیت مالیاتی
     * 
     * @param taxable نوع مشمولیت مالیاتی
     * @return لیست محصولات مالیاتی
     */
    @Query("SELECT t FROM TaxProductEntity t WHERE t.taxable = :taxable AND t.isActive = true")
    List<TaxProductEntity> findByTaxable(@Param("taxable") ProductTaxable taxable);
    
    /**
     * یافتن محصولات مالیاتی بر اساس نوع محصول
     * 
     * @param type نوع محصول مالیاتی
     * @return لیست محصولات مالیاتی
     */
    @Query("SELECT t FROM TaxProductEntity t WHERE t.type = :type AND t.isActive = true")
    List<TaxProductEntity> findByType(@Param("type") TaxProductType type);
    
    /**
     * یافتن محصولات مالیاتی بر اساس تاریخ اجرا
     * 
     * @param runDate تاریخ اجرا
     * @return لیست محصولات مالیاتی
     */
    @Query("SELECT t FROM TaxProductEntity t WHERE t.runDate = :runDate AND t.isActive = true")
    List<TaxProductEntity> findByRunDate(@Param("runDate") LocalDate runDate);
    
    /**
     * یافتن محصولات مالیاتی منقضی شده
     * 
     * @param currentDate تاریخ جاری
     * @return لیست محصولات مالیاتی منقضی شده
     */
    @Query("SELECT t FROM TaxProductEntity t WHERE t.expiration < :currentDate AND t.isActive = true")
    List<TaxProductEntity> findExpired(@Param("currentDate") LocalDate currentDate);
    
    /**
     * یافتن محصولات مالیاتی در حال انقضا
     * 
     * @param currentDate تاریخ جاری
     * @param expirationDate تاریخ انقضا (مثلاً 30 روز آینده)
     * @return لیست محصولات مالیاتی در حال انقضا
     */
    @Query("SELECT t FROM TaxProductEntity t WHERE t.expiration BETWEEN :currentDate AND :expirationDate AND t.isActive = true")
    List<TaxProductEntity> findExpiring(@Param("currentDate") LocalDate currentDate, 
                                        @Param("expirationDate") LocalDate expirationDate);
    
    /**
     * یافتن محصول مالیاتی بر اساس شناسه محصول مالیاتی (TAX_PRODUCT_ID)
     * 
     * @param taxProductId شناسه محصول مالیاتی
     * @return محصول مالیاتی موجود یا Optional.empty()
     */
    Optional<TaxProductEntity> findByTaxProductId(String taxProductId);

    /**
     * جستجوی محصولات مالیاتی بر اساس taxProductId یا title
     * برای استفاده در autocomplete
     *
     * @param query متن جستجو (می‌تواند بخشی از taxProductId یا title باشد)
     * @param pageable اطلاعات صفحه‌بندی
     * @return لیست محصولات مالیاتی
     */
    @Query("SELECT t FROM TaxProductEntity t WHERE " +
           "t.isActive = true AND " +
           "(LOWER(t.taxProductId) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(t.productTitle) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY t.productTitle ASC")
    List<TaxProductEntity> findByTaxProductIdContainingIgnoreCaseOrTitleContainingIgnoreCase(
            @Param("query") String query,
            Pageable pageable);
}

