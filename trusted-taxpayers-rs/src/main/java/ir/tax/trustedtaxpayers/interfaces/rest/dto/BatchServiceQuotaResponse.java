package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

@Schema(description = "پاسخ سهمیه دسته‌ای خدمات")
public record BatchServiceQuotaResponse(
        @Schema(description = "لیست سهمیه‌های خدمات") List<ServiceQuotaDto> quotas,
        @Schema(description = "تعداد کل") Integer totalCount
) {
}

