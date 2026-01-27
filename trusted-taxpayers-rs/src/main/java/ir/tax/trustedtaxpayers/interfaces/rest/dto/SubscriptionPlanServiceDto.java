package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "اطلاعات ارتباط بین طرح اشتراک و خدمات")
public record SubscriptionPlanServiceDto(
        @Schema(description = "شناسه یکتا") UUID id,
        @Schema(description = "شناسه طرح اشتراک") UUID planId,
        @Schema(description = "کد طرح اشتراک") String planCode,
        @Schema(description = "نام طرح اشتراک") String planName,
        @Schema(description = "شناسه خدمات") UUID serviceId,
        @Schema(description = "کد خدمات") String serviceCode,
        @Schema(description = "نام خدمات") String serviceName,
        @Schema(description = "دسته‌بندی خدمات") String serviceCategory,
        @Schema(description = "فعال است") Boolean isEnabled,
        @Schema(description = "مقدار محدودیت") BigDecimal limitValue,
        @Schema(description = "واحد محدودیت (MONTHLY, YEARLY, UNLIMITED)") String limitUnit,
        @Schema(description = "یادداشت") String notes
) {
}

