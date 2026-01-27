package ir.tax.trustedtaxpayers.beans.dto.tax;

/**
 * Author: Behzad Farajpour
 * Project: TSP
 * Email: farajpour-b@hamfekrangroup.ir
 */

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class GenerateInvoiceHtmlRequest {

    @Valid
    @NotNull(message = "Invoices list cannot be null")
    @NotEmpty(message = "Invoices list cannot be empty")
    private List<InvoiceDto> invoices;
    private String sellerName;
    private String buyerName;
}

