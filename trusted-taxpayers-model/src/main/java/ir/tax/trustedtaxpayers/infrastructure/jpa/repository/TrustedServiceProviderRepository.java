package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TrustedServiceProviderEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TrustedServiceProviderRepository extends BaseRepository<TrustedServiceProviderEntity, UUID> {

    Optional<TrustedServiceProviderEntity> findByCode(String code);

    List<TrustedServiceProviderEntity> findByIsActiveTrueOrderByNameAsc();
}


