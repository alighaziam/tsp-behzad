package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import java.time.LocalDate;
import java.util.UUID;

public record TaxpayerSubscriptionDto(
        UUID id,
        UUID taxpayerId,
        UUID planId,
        String planName,
        String planCode,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        Integer remainingInvoices,
        String paymentReference,
        String notes,
        Boolean active
) {
}

