package ir.tax.trustedtaxpayers.beans.dto.tax;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * درخواست ارسال صورت‌حساب‌های مالیاتی
 * Tax Invoice Send Request
 */
@Data
public class TaxInvoiceRequest {

    /**
     * لیست صورت‌حساب‌های مالیاتی
     * List of tax invoices
     */
    @Valid
    @NotNull(message = "Invoices list cannot be null")
    @NotEmpty(message = "Invoices list cannot be empty")
    private List<InvoiceDto> invoices;

    /**
     * ارسال با کلید شرکت معتمد یا مودی
     * Send with TSP key or taxpayer key
     * true = TSP key, false = taxpayer key
     */
    private boolean useTspKey = true;
}

