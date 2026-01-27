package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.OutboxMessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface OutboxMessageRepository extends JpaRepository<OutboxMessageEntity, UUID> {

    @Query("select o from OutboxMessageEntity o where o.status = 'PENDING' and (o.nextAttemptAt is null or o.nextAttemptAt <= :now) order by o.createdAt asc")
    List<OutboxMessageEntity> findDuePending(@Param("now") OffsetDateTime now);

    OutboxMessageEntity findTopByAggregateIdAndEventTypeOrderByCreatedAtDesc(String aggregateId, String eventType);

    List<OutboxMessageEntity> findByEventTypeOrderByCreatedAtDesc(String eventType, Pageable pageable);
}


