package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "الگوی آماده صورتحساب")
public record InvoiceTemplateDto(
        @Schema(description = "شناسه الگو", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(description = "شناسه کاربر", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID userId,
        @Schema(description = "شناسه مودی", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID taxpayerId,
        @Schema(description = "نام الگو", example = "صورتحساب فروش ماهانه")
        String templateName,
        @Schema(description = "شماره الگو", example = "1")
        Integer patternNumber,
        @Schema(description = "داده‌های Header (JSON)")
        String headerData,
        @Schema(description = "داده‌های Items (JSON)")
        String itemsData,
        @Schema(description = "توضیحات")
        String description,
        @Schema(description = "تاریخ ایجاد")
        OffsetDateTime createdOn,
        @Schema(description = "تاریخ به‌روزرسانی")
        OffsetDateTime updatedOn
) {
}

