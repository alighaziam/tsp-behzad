package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogDto(UUID id, OffsetDateTime eventTime, UUID actorUserId, String entityName,
                          String entityId, String action, String details) {}


