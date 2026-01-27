package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "خلاصه دسترسی به خدمات")
public record ServiceAccessSummaryDto(
        @Schema(description = "تعداد کل دسترسی‌ها") Long totalAccess,
        @Schema(description = "تعداد دسترسی‌های موفق") Long successfulAccess,
        @Schema(description = "تعداد دسترسی‌های ناموفق") Long failedAccess,
        @Schema(description = "نرخ موفقیت") Double successRate,
        @Schema(description = "نرخ شکست") Double failureRate
) {
}

