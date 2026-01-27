package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

@Schema(description = "درخواست بررسی دسترسی دسته‌ای به خدمات")
public record BatchServiceAccessRequest(
        @Schema(description = "لیست شناسه‌های مودی", required = true)
        @NotEmpty(message = "لیست شناسه‌های مودی نمی‌تواند خالی باشد")
        List<UUID> taxpayerIds,
        
        @Schema(description = "کد خدمات", required = true)
        @NotNull(message = "کد خدمات نمی‌تواند خالی باشد")
        String serviceCode
) {
}

