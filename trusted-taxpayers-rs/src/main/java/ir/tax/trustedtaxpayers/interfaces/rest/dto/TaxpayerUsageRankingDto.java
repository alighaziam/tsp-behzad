package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "رتبه‌بندی استفاده مودیان")
public record TaxpayerUsageRankingDto(
        @Schema(description = "شناسه مودی") String taxpayerId,
        @Schema(description = "نام مودی") String taxpayerName,
        @Schema(description = "تعداد استفاده") Long usageCount,
        @Schema(description = "درصد استفاده") Double usagePercentage,
        @Schema(description = "وضعیت") String status,
        @Schema(description = "رتبه") Integer rank
) {
}

