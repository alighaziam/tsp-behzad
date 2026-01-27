package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record PurchaseSubscriptionRequest(
        @NotNull UUID planId,
        String paymentReference,
        String notes
) {
}

