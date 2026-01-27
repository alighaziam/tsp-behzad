package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO خریدار")
public record BuyerDto(
        @Schema(description = "شناسه یکتا") String id,
        @Schema(description = "نوع خریدار") String buyerType,
        @Schema(description = "شناسه ملی") String nationalId,
        @Schema(description = "نام خریدار") String name,
        @Schema(description = "شماره مودی منحصربه‌فرد") String taxpayerNo,
        @Schema(description = "وضعیت فعال بودن") boolean active) {}


