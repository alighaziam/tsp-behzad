package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.PatternRequestClientEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PatternRequestClientRepository extends JpaRepository<PatternRequestClientEntity, UUID> {

    Optional<PatternRequestClientEntity> findByRequestId(UUID requestId);

    boolean existsByRequestId(UUID requestId);
}
