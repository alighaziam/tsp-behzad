package ir.tax.trustedtaxpayers.application.dto;

import java.math.BigDecimal;

public record InvoiceStatusSummaryDto(
        String status,
        long count,
        BigDecimal totalAmount
) {
}



