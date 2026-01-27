package ir.tax.trustedtaxpayers.beans.dto.tax;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * درخواست ارسال با کلید شرکت معتمد
 * TSP Send with Key Request
 */
@Data
public class TspSendWithKeyRequest {

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
}

