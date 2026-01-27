package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.PatternClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PatternClientRepository extends JpaRepository<PatternClientEntity, UUID> {

    Optional<PatternClientEntity> findByClientKey(String clientKey);

    boolean existsByClientKey(String clientKey);
}
