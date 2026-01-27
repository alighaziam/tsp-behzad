package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.enums.PatternRequestStatus;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.PatternRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PatternRequestRepository extends JpaRepository<PatternRequestEntity, UUID>,
        JpaSpecificationExecutor<PatternRequestEntity> {

    Optional<PatternRequestEntity> findByReferenceNumber(String referenceNumber);

    List<PatternRequestEntity> findByStatusAndCreatedOnBetween(PatternRequestStatus status,
                                                               OffsetDateTime from,
                                                               OffsetDateTime to);

    List<PatternRequestEntity> findTop100ByDeferredSubmissionTrueAndDeferredApprovedTrueAndStatusOrderByCreatedOnAsc(
            PatternRequestStatus status);
}
