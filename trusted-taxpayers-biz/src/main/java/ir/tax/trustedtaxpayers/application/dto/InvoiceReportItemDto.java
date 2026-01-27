package ir.tax.trustedtaxpayers.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record InvoiceReportItemDto(
        UUID headerId,
        String taxId,
        String invoiceNumber,
        String templateCode,
        Integer patternNumber,
        Integer invoiceSubject,
        String invoiceSubjectLabel,
        String sellerTaxpayerNo,
        String sellerName,
        String buyerTaxpayerNo,
        String buyerName,
        BigDecimal totalAmount,
        OffsetDateTime createdOn,
        OffsetDateTime statusTimestamp,
        String status,
        String rejectionCode,
        String rejectionDescription,
        String referenceNumber
) {
}



