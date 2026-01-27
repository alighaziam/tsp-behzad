package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import java.time.LocalDate;

public record TaxpayerTspContractDto(
        String id,
        String taxpayerId,
        String providerId,
        String providerName,
        String contractNumber,
        String contractTitle,
        LocalDate signedAt,
        LocalDate expiresAt,
        String status,
        String notes,
        boolean active
) {
}


