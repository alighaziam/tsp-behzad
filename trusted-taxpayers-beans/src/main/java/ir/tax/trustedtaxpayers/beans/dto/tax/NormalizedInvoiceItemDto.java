package ir.tax.trustedtaxpayers.beans.dto.tax;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class NormalizedInvoiceItemDto {
    private String itemName;
    private BigDecimal itemQty;
    private BigDecimal itemUnitPrice;
}


