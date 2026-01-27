package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "گزارش دسترسی و استفاده از خدمات")
public record ServiceAccessReportDto(
        @Schema(description = "شناسه مودی") UUID taxpayerId,
        @Schema(description = "نام مودی") String taxpayerName,
        @Schema(description = "شناسه اشتراک") UUID subscriptionId,
        @Schema(description = "نام پلن اشتراک") String planName,
        @Schema(description = "تاریخ شروع گزارش") LocalDate startDate,
        @Schema(description = "تاریخ پایان گزارش") LocalDate endDate,
        @Schema(description = "لیست آمار استفاده از خدمات") List<ServiceUsageStatisticsDto> serviceUsage,
        @Schema(description = "تعداد کل دسترسی‌های موفق") Long totalSuccessfulAccess,
        @Schema(description = "تعداد کل دسترسی‌های ناموفق") Long totalFailedAccess,
        @Schema(description = "تعداد کل استفاده از خدمات") Long totalServiceUsage
) {
}

