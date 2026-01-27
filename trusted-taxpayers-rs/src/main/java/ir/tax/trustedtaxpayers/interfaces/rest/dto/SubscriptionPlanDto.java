package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SubscriptionPlanDto(
        UUID id,
        String code,
        String name,
        String description,
        BigDecimal price,
        String currency,
        Integer durationDays,
        Integer maxInvoices,
        String features,
        Boolean active
) {
}

