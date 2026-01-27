package ir.tax.trustedtaxpayers.beans.dto.tax;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * درخواست ارسال با کلید مودی
 * Taxpayer Key Send Request
 */
@Data
public class TaxpayerKeySendRequest {

    /**
     * لیست صورت‌حساب‌ها
     * List of invoices
     */
    @Valid
    @NotNull(message = "Invoices list cannot be null")
    @NotEmpty(message = "Invoices list cannot be empty")
    private List<InvoiceDto> invoices;

    /**
     * شناسه حافظه مالیاتی مودی
     * Taxpayer fiscal memory ID
     */
    @NotBlank(message = "Fiscal ID cannot be blank")
    private String fiscalId;

    /**
     * مسیر فایل کلید خصوصی مودی
     * Path to taxpayer's private key file
     */
    @NotBlank(message = "Private key path cannot be blank")
    private String privateKeyPath;

    /**
     * رمز عبور کلید خصوصی (اختیاری)
     * Private key password (optional)
     */
    private String privateKeyPassword;
}

