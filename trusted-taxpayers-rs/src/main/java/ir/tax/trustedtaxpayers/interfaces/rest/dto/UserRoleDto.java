package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record UserRoleDto(UUID userId, UUID roleId, OffsetDateTime assignedAt) {}



