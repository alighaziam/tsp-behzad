package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "اطلاعات طرح اشتراک همراه با خدمات")
public record SubscriptionPlanWithServicesDto(
        @Schema(description = "شناسه یکتا") UUID id,
        @Schema(description = "کد طرح") String code,
        @Schema(description = "نام طرح") String name,
        @Schema(description = "توضیحات") String description,
        @Schema(description = "قیمت") BigDecimal price,
        @Schema(description = "واحد پول") String currency,
        @Schema(description = "مدت زمان (روز)") Integer durationDays,
        @Schema(description = "حداکثر تعداد صورتحساب") Integer maxInvoices,
        @Schema(description = "ویژگی‌ها") String features,
        @Schema(description = "فعال است") Boolean active,
        @Schema(description = "لیست خدمات") List<SubscriptionPlanServiceDto> services
) {
}

