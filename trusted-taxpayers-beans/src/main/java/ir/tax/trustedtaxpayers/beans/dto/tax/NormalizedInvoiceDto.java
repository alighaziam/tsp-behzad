package ir.tax.trustedtaxpayers.beans.dto.tax;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class NormalizedInvoiceDto {
    private String invoiceNumber;
    private String issueDate; // ISO date (YYYY-MM-DD)
    private String sellerTaxId;
    private String buyerTaxId;
    private String currency;
    private BigDecimal totalAmount;
    private List<NormalizedInvoiceItemDto> items;
}


