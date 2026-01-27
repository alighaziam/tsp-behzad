package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxpayerTspContractEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TaxpayerTspContractRepository extends BaseRepository<TaxpayerTspContractEntity, UUID> {

    @Query("select c from TaxpayerTspContractEntity c " +
            "where c.taxpayerId = :taxpayerId and c.isActive = true and (c.expiresAt is null or c.expiresAt >= current_date)")
    Optional<TaxpayerTspContractEntity> findActiveByTaxpayer(@Param("taxpayerId") UUID taxpayerId);

    List<TaxpayerTspContractEntity> findByTaxpayerId(UUID taxpayerId);

    List<TaxpayerTspContractEntity> findByIsActiveTrueAndExpiresAtBetween(LocalDate startInclusive, LocalDate endInclusive);

    List<TaxpayerTspContractEntity> findByIsActiveTrueAndExpiresAtBefore(LocalDate endExclusive);
}


