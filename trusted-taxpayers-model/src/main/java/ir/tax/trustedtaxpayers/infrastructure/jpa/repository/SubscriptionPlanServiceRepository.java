package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.SubscriptionPlanServiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionPlanServiceRepository extends JpaRepository<SubscriptionPlanServiceEntity, UUID> {
    
    List<SubscriptionPlanServiceEntity> findByPlanId(UUID planId);
    
    List<SubscriptionPlanServiceEntity> findByServiceId(UUID serviceId);
    
    boolean existsByPlanIdAndServiceId(UUID planId, UUID serviceId);
    
    Optional<SubscriptionPlanServiceEntity> findByPlanIdAndServiceId(UUID planId, UUID serviceId);
}

