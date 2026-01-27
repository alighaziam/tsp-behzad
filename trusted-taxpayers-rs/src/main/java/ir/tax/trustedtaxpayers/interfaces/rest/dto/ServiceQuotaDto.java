package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "سهمیه و محدودیت خدمات")
public record ServiceQuotaDto(
        @Schema(description = "شناسه مودی") UUID taxpayerId,
        @Schema(description = "شناسه خدمات") UUID serviceId,
        @Schema(description = "کد خدمات") String serviceCode,
        @Schema(description = "نام خدمات") String serviceName,
        @Schema(description = "محدودیت") BigDecimal limitValue,
        @Schema(description = "واحد محدودیت") String limitUnit,
        @Schema(description = "استفاده شده") Long used,
        @Schema(description = "باقیمانده") Long remaining,
        @Schema(description = "درصد استفاده") Double usagePercentage,
        @Schema(description = "وضعیت") String status,
        @Schema(description = "آیا محدودیت نامحدود است") Boolean isUnlimited
) {
}

