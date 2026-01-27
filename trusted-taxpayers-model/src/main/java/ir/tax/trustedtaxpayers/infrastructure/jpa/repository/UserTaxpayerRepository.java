package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.UserTaxpayerEntity;

import java.util.Optional;
import java.util.UUID;

public interface UserTaxpayerRepository extends BaseRepository<UserTaxpayerEntity, UserTaxpayerEntity.Pk> {
    Optional<UserTaxpayerEntity> findByTaxpayerId(UUID taxpayerId);
    Optional<UserTaxpayerEntity> findByUserId(UUID userId);
}














