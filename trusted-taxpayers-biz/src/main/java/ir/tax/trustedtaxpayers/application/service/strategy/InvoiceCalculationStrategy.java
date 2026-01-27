package ir.tax.trustedtaxpayers.application.service.strategy;

import ir.tax.trustedtaxpayers.beans.model.Invoice;
import ir.tax.trustedtaxpayers.beans.model.InvoiceType;

public interface InvoiceCalculationStrategy {
    boolean supports(InvoiceType type);
    Invoice calculate(Invoice invoice);
}




