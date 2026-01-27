package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record UserActivityDto(
        UUID id,
        OffsetDateTime performedAt,
        String action,
        String performedBy,
        String ipAddress,
        Map<String, Object> metadata
) {
}

