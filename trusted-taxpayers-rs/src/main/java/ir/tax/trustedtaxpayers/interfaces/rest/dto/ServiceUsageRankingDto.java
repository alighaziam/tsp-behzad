package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "رتبه‌بندی استفاده از خدمات")
public record ServiceUsageRankingDto(
        @Schema(description = "کد خدمات") String serviceCode,
        @Schema(description = "نام خدمات") String serviceName,
        @Schema(description = "تعداد استفاده") Long usageCount,
        @Schema(description = "رتبه") Integer rank
) {
}

