package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.UserRoleEntity;

import java.util.List;
import java.util.UUID;

public interface UserRoleRepository extends BaseRepository<UserRoleEntity, UserRoleEntity.Pk> {
    List<UserRoleEntity> findByUserId(UUID userId);
    List<UserRoleEntity> findByRoleId(UUID roleId);
}



