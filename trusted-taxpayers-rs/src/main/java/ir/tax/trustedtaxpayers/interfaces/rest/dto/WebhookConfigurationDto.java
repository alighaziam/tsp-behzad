package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@Schema(description = "پیکربندی Webhook برای دسترسی به خدمات")
public record WebhookConfigurationDto(
        @Schema(description = "شناسه یکتا") UUID id,
        @Schema(description = "شناسه مودی (اختیاری)") UUID taxpayerId,
        @Schema(description = "URL webhook", required = true) @NotBlank String webhookUrl,
        @Schema(description = "نوع رویداد (QUOTA_ALERT, LIMIT_EXCEEDED, ACCESS_GRANTED, ACCESS_DENIED)", required = true) 
        @NotNull List<String> eventTypes,
        @Schema(description = "کد خدمات (اختیاری)") String serviceCode,
        @Schema(description = "فعال است؟") Boolean isActive,
        @Schema(description = "Secret برای تأیید webhook") String secret,
        @Schema(description = "یادداشت‌ها") String notes
) {
}

