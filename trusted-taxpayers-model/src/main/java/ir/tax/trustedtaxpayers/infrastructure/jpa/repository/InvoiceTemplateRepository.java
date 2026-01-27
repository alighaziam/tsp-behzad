package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.InvoiceTemplateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceTemplateRepository extends JpaRepository<InvoiceTemplateEntity, UUID> {

    /**
     * پیدا کردن تمام template های یک کاربر برای یک مودی
     */
    @Query("SELECT t FROM InvoiceTemplateEntity t WHERE t.user.id = :userId " +
            "AND t.taxpayer.id = :taxpayerId AND t.isActive = true ORDER BY t.createdOn DESC")
    List<InvoiceTemplateEntity> findByUserIdAndTaxpayerId(
            @Param("userId") UUID userId,
            @Param("taxpayerId") UUID taxpayerId);

    /**
     * پیدا کردن template های یک کاربر بر اساس pattern number
     */
    @Query("SELECT t FROM InvoiceTemplateEntity t WHERE t.user.id = :userId " +
            "AND t.taxpayer.id = :taxpayerId AND t.patternNumber = :patternNumber " +
            "AND t.isActive = true ORDER BY t.createdOn DESC")
    List<InvoiceTemplateEntity> findByUserIdAndTaxpayerIdAndPatternNumber(
            @Param("userId") UUID userId,
            @Param("taxpayerId") UUID taxpayerId,
            @Param("patternNumber") Integer patternNumber);

    /**
     * پیدا کردن یک template خاص
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM InvoiceTemplateEntity t " +
            "WHERE t.id = :id AND t.user.id = :userId AND t.taxpayer.id = :taxpayerId")
    boolean existsByIdAndUserIdAndTaxpayerId(
            @Param("id") UUID id,
            @Param("userId") UUID userId,
            @Param("taxpayerId") UUID taxpayerId);
}
