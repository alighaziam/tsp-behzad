package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ServiceUsageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceUsageRepository extends JpaRepository<ServiceUsageEntity, UUID> {
    
    @Query("SELECT su FROM ServiceUsageEntity su WHERE su.taxpayer.id = :taxpayerId ORDER BY su.usageDate DESC, su.createdOn DESC")
    List<ServiceUsageEntity> findByTaxpayerId(@Param("taxpayerId") UUID taxpayerId);
    
    @Query("SELECT su FROM ServiceUsageEntity su WHERE su.taxpayer.id = :taxpayerId AND su.usageDate = :usageDate ORDER BY su.createdOn DESC")
    List<ServiceUsageEntity> findByTaxpayerIdAndUsageDate(@Param("taxpayerId") UUID taxpayerId, @Param("usageDate") LocalDate usageDate);
    
    @Query("SELECT su FROM ServiceUsageEntity su WHERE su.taxpayer.id = :taxpayerId AND su.service.id = :serviceId ORDER BY su.usageDate DESC, su.createdOn DESC")
    List<ServiceUsageEntity> findByTaxpayerIdAndServiceId(@Param("taxpayerId") UUID taxpayerId, @Param("serviceId") UUID serviceId);
    
    @Query("SELECT su FROM ServiceUsageEntity su WHERE su.subscription.id = :subscriptionId ORDER BY su.usageDate DESC, su.createdOn DESC")
    List<ServiceUsageEntity> findBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);
    
    @Query("SELECT su FROM ServiceUsageEntity su WHERE su.serviceCode = :serviceCode ORDER BY su.usageDate DESC, su.createdOn DESC")
    List<ServiceUsageEntity> findByServiceCode(@Param("serviceCode") String serviceCode);
    
    @Query("SELECT SUM(su.usageCount) FROM ServiceUsageEntity su " +
           "WHERE su.taxpayer.id = :taxpayerId AND su.service.id = :serviceId " +
           "AND su.usageDate >= :startDate AND su.usageDate <= :endDate")
    Long getTotalUsageCount(@Param("taxpayerId") UUID taxpayerId, 
                           @Param("serviceId") UUID serviceId,
                           @Param("startDate") LocalDate startDate,
                           @Param("endDate") LocalDate endDate);
    
    @Query("SELECT su FROM ServiceUsageEntity su " +
           "WHERE su.taxpayer.id = :taxpayerId AND su.service.id = :serviceId " +
           "AND su.usageDate = :usageDate AND su.usageType = :usageType " +
           "AND (:referenceId IS NULL OR su.referenceId = :referenceId)")
    @Query("SELECT COALESCE(SUM(su.usageCount), 0) FROM ServiceUsageEntity su "
            + "WHERE su.usageDate >= :startDate AND su.usageDate <= :endDate")
    Long sumUsageBetween(@Param("startDate") LocalDate startDate,
                         @Param("endDate") LocalDate endDate);

    Optional<ServiceUsageEntity> findExistingUsage(@Param("taxpayerId") UUID taxpayerId,
                                                   @Param("serviceId") UUID serviceId,
                                                   @Param("usageDate") LocalDate usageDate,
                                                   @Param("usageType") String usageType,
                                                   @Param("referenceId") UUID referenceId);
}

