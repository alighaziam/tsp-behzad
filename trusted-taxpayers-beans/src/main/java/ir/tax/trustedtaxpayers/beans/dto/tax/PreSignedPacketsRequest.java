package ir.tax.trustedtaxpayers.beans.dto.tax;

import ir.gov.tax.tpis.sdk.transfer.dto.PacketDto;
import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * درخواست ارسال بسته‌های از پیش امضا شده
 * Pre-signed Packets Send Request
 */
@Data
public class PreSignedPacketsRequest {

    /**
     * لیست بسته‌های از پیش امضا شده
     * List of pre-signed packets
     */
    @Valid
    @NotNull(message = "Packets list cannot be null")
    @NotEmpty(message = "Packets list cannot be empty")
    private List<PacketDto> packets;

    /**
     * توکن احراز هویت (اختیاری)
     * Authentication token (optional)
     */
    private String token;
}

