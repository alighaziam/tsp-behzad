package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "آمار استفاده از خدمات")
public record ServiceUsageStatisticsDto(
        @Schema(description = "شناسه مودی") UUID taxpayerId,
        @Schema(description = "شناسه خدمات") UUID serviceId,
        @Schema(description = "کد خدمات") String serviceCode,
        @Schema(description = "نام خدمات") String serviceName,
        @Schema(description = "تاریخ شروع") LocalDate startDate,
        @Schema(description = "تاریخ پایان") LocalDate endDate,
        @Schema(description = "تعداد کل استفاده") Long totalUsage,
        @Schema(description = "محدودیت") BigDecimal limitValue,
        @Schema(description = "واحد محدودیت") String limitUnit,
        @Schema(description = "درصد استفاده") Double usagePercentage,
        @Schema(description = "باقیمانده") Long remaining,
        @Schema(description = "وضعیت (OK, WARNING, EXCEEDED)") String status
) {
}

