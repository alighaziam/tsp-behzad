package ir.tax.trustedtaxpayers.infrastructure.jpa.repository;

import ir.tax.trustedtaxpayers.core.repository.BaseRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends BaseRepository<AuditLogEntity, UUID> {
    List<AuditLogEntity> findByActorUserId(UUID actorUserId);
    List<AuditLogEntity> findByEntityName(String entityName);
    List<AuditLogEntity> findByAction(String action);
    List<AuditLogEntity> findByEntityNameAndEntityId(String entityName, String entityId);
    
    @Query("SELECT a FROM AuditLogEntity a WHERE a.createdOn BETWEEN :start AND :end")
    List<AuditLogEntity> findByEventTimeBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);
    
    // Search in entityName and action (case-insensitive)
    // Note: details (CLOB) search is excluded to avoid H2 compatibility issues
    @Query("SELECT a FROM AuditLogEntity a WHERE " +
           "LOWER(a.entityName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(a.action) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<AuditLogEntity> searchIgnoreCase(@Param("query") String query);
}



