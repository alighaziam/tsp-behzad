package ir.tax.trustedtaxpayers.beans.dto.tax;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * درخواست ارسال صورت‌حساب با کلید شرکت معتمد
 * TSP Invoice Send Request (with TSP key)
 */
@Data
public class TspInvoiceRequest {

    /**
     * لیست wrapper های صورت‌حساب (شامل fiscalId)
     * List of invoice wrappers (including fiscalId)
     */
    @Valid
    @NotNull(message = "Invoice wrappers list cannot be null")
    @NotEmpty(message = "Invoice wrappers list cannot be empty")
    private List<InvoiceDtoWrapper> invoiceWrappers;
}

