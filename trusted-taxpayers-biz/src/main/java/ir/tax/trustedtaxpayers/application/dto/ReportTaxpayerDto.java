package ir.tax.trustedtaxpayers.application.dto;

import java.util.UUID;

public record ReportTaxpayerDto(
        UUID id,
        String taxpayerNo,
        String name
) {
}



