package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxpayerSubscriptionRepository extends JpaRepository<TaxpayerSubscriptionEntity, UUID> {
    List<TaxpayerSubscriptionEntity> findByTaxpayerId(UUID taxpayerId);
    
    @Query("SELECT s FROM TaxpayerSubscriptionEntity s WHERE s.taxpayerId = :taxpayerId AND s.status = 'ACTIVE' AND s.endDate >= :today ORDER BY s.endDate DESC")
    Optional<TaxpayerSubscriptionEntity> findActiveByTaxpayerId(@Param("taxpayerId") UUID taxpayerId, @Param("today") LocalDate today);
    
    @Query("SELECT s FROM TaxpayerSubscriptionEntity s WHERE s.status = 'ACTIVE' AND s.endDate BETWEEN :startDate AND :endDate")
    List<TaxpayerSubscriptionEntity> findExpiringSubscriptions(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT s FROM TaxpayerSubscriptionEntity s WHERE s.status = 'ACTIVE' AND s.endDate < :today")
    List<TaxpayerSubscriptionEntity> findExpiredSubscriptions(@Param("today") LocalDate today);
    
    @Query("SELECT s FROM TaxpayerSubscriptionEntity s WHERE s.status = 'ACTIVE' AND s.endDate >= :today ORDER BY s.endDate DESC")
    List<TaxpayerSubscriptionEntity> findActiveSubscriptions(@Param("today") LocalDate today);
}

