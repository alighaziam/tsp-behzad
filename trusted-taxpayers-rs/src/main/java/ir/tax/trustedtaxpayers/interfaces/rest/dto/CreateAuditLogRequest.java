package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record CreateAuditLogRequest(
        UUID actorUserId,
        @Size(max = 100) String entityName,
        String entityId,
        @Size(max = 50) String action,
        String details
) {}


