package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record UserDashboardResponseDto(
        @Schema(description = "ترجیحات ثبت‌شده برای کاربر")
        List<UserInvoicePreferenceDto> preferences,
        @Schema(description = "لیست آخرین صورتحساب‌ها")
        List<InvoiceSummaryDto> recentInvoices
) {
}

