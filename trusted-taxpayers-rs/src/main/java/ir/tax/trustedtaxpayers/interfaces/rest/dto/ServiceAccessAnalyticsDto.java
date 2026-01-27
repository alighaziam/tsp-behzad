package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Schema(description = "تحلیل استفاده از دسترسی به خدمات")
public record ServiceAccessAnalyticsDto(
        @Schema(description = "کد خدمات") String serviceCode,
        @Schema(description = "نام خدمات") String serviceName,
        @Schema(description = "تاریخ شروع") LocalDate startDate,
        @Schema(description = "تاریخ پایان") LocalDate endDate,
        @Schema(description = "دوره (DAILY, WEEKLY, MONTHLY)") String period,
        @Schema(description = "روند استفاده (تاریخ -> تعداد)") Map<String, Long> trends,
        @Schema(description = "استفاده کل") Long totalUsage
) {
}

