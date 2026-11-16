package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.UserTaxpayerEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for the many-to-many user/taxpayer association.
 * A user may manage multiple taxpayers and a taxpayer may be shared.
 */
public interface UserTaxpayerRepository extends BaseRepository<UserTaxpayerEntity, UserTaxpayerEntity.Pk> {
    List<UserTaxpayerEntity> findAllByTaxpayerId(UUID taxpayerId);
    List<UserTaxpayerEntity> findAllByUserId(UUID userId);
    Optional<UserTaxpayerEntity> findByUserIdAndTaxpayerId(UUID userId, UUID taxpayerId);
}
