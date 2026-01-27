package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateInvoiceItemRequest(
        UUID invoiceId,
        UUID productId,
        @NotBlank @Size(max = 100) String itemCode,
        @NotBlank @Size(max = 300) String itemName,
        @NotBlank @Size(max = 50) String unit,
        @NotNull BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineDiscount,
        BigDecimal vatRate
) {}


