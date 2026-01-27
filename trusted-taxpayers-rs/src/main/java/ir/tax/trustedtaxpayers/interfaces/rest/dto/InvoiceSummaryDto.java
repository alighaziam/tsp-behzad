package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record InvoiceSummaryDto(
        @Schema(description = "شناسه سربرگ صورتحساب", example = "AbCdEfGhIjKlMnOpQrStUvWx")
        String headerId,
        @Schema(description = "شماره مالیاتی صورتحساب", example = "AB12CD34EF56GH78IJ90KL")
        String taxId,
        @Schema(description = "کد Template استفاده‌شده", example = "T01_RETAIL")
        String templateCode,
        @Schema(description = "کد Template Type", example = "P01")
        String templateTypeCode,
        @Schema(description = "شماره الگو (Pattern)", example = "1")
        Integer patternNumber,
        @Schema(description = "موضوع صورتحساب (ins)", example = "1")
        Integer invoiceSubject,
        @Schema(description = "عنوان موضوع صورتحساب", example = "اصلی")
        String invoiceSubjectLabel,
        @Schema(description = "مبلغ کل صورتحساب", example = "3500000")
        BigDecimal totalAmount,
        @Schema(description = "زمان صدور (Epoch millis)", example = "1709951400000")
        Long issuedAt,
        @Schema(description = "وضعیت ارسال در سامانه مودیان", example = "SENT")
        String status
) {
}

