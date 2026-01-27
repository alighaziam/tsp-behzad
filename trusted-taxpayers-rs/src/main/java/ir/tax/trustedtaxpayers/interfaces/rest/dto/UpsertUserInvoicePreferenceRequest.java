package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpsertUserInvoicePreferenceRequest(
        @NotNull
        @Schema(description = "شناسه مودی مرتبط با کاربر", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID taxpayerId,

        @Schema(description = "کد Template انتخابی", example = "T01_RETAIL")
        String defaultTemplateCode,

        @Schema(description = "کد Type Template", example = "P01")
        String defaultTemplateTypeCode,

        @Schema(description = "شماره Pattern پیش‌فرض", example = "1")
        Integer defaultPatternNumber,

        @Schema(description = "موضوع صورتحساب پیش‌فرض (1=اصلی,2=اصلاحی,3=ابطالی,4=برگشت)", example = "1")
        Integer defaultIns,

        @Schema(description = "کد آخرین Template استفاده‌شده", example = "T01_RETAIL")
        String lastUsedTemplateCode,

        @Schema(description = "آیا پیکربندی اولیه کامل شده است؟", example = "true")
        Boolean firstLoginCompleted
) {
}

