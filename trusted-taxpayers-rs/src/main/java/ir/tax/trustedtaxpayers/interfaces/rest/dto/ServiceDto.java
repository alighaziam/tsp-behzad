package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

@Schema(description = "اطلاعات خدمات سیستم")
public record ServiceDto(
        @Schema(description = "شناسه یکتا") UUID id,
        @Schema(description = "کد خدمات") String code,
        @Schema(description = "نام خدمات") String name,
        @Schema(description = "توضیحات") String description,
        @Schema(description = "دسته‌بندی (INVOICE, REPORT, API, SUPPORT, MANAGEMENT)") String category,
        @Schema(description = "فعال است") Boolean active
) {
}

