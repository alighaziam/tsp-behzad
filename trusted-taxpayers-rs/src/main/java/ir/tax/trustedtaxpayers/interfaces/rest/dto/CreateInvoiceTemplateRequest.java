package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "درخواست ایجاد الگوی آماده صورتحساب")
public record CreateInvoiceTemplateRequest(
        @Schema(description = "نام الگو", example = "صورتحساب فروش ماهانه", required = true)
        @NotBlank(message = "نام الگو الزامی است")
        String templateName,
        @Schema(description = "شماره الگو", example = "1", required = true)
        @NotNull(message = "شماره الگو الزامی است")
        Integer patternNumber,
        @Schema(description = "داده‌های Header (JSON)", required = true)
        @NotBlank(message = "داده‌های Header الزامی است")
        String headerData,
        @Schema(description = "داده‌های Items (JSON)", required = true)
        @NotBlank(message = "داده‌های Items الزامی است")
        String itemsData,
        @Schema(description = "توضیحات")
        String description
) {
}

