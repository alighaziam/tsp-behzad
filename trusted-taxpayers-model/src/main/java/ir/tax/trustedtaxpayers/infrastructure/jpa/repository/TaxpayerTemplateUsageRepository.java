package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerTemplateUsageEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerTemplateUsageId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaxpayerTemplateUsageRepository extends JpaRepository<TaxpayerTemplateUsageEntity, TaxpayerTemplateUsageId> {
    
    List<TaxpayerTemplateUsageEntity> findByTaxpayerIdOrderByLastUsedAtDesc(UUID taxpayerId, Pageable pageable);
}

