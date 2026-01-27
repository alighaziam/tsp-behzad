package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Schema(description = "داشبورد دسترسی به خدمات")
public record ServiceAccessDashboardDto(
        @Schema(description = "تاریخ گزارش") LocalDate reportDate,
        @Schema(description = "آمار کلی") ServiceAccessSummaryDto summary,
        @Schema(description = "پراستفاده‌ترین خدمات") List<ServiceUsageRankingDto> topServices,
        @Schema(description = "پراستفاده‌ترین مودیان") List<TaxpayerUsageRankingDto> topTaxpayers,
        @Schema(description = "توزیع استفاده بر اساس ساعت") Map<String, Long> usageByHour,
        @Schema(description = "نرخ موفقیت دسترسی") Double successRate,
        @Schema(description = "نرخ شکست دسترسی") Double failureRate,
        @Schema(description = "تعداد خدمات فعال") Long activeServicesCount,
        @Schema(description = "تعداد اشتراک‌های فعال") Long activeSubscriptionsCount
) {
}

