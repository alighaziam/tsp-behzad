package ir.tax.trustedtaxpayers.application.service.strategy.impl;

import ir.tax.trustedtaxpayers.beans.model.Invoice;
import ir.tax.trustedtaxpayers.beans.model.InvoiceType;

import java.math.BigDecimal;

public class Type7ExportStrategy extends AbstractInvoiceCalculationStrategy {
    @Override
    public boolean supports(InvoiceType type) {
        return type == InvoiceType.TYPE7_EXPORT;
    }

    @Override
    public Invoice calculate(Invoice invoice) {
        BigDecimal subtotal = calculateSubtotal(invoice);
        BigDecimal discount = calculateDiscount(invoice);
        BigDecimal shipping = defaultShipping(invoice);

        // Exports are typically VAT zero-rated
        BigDecimal vat = BigDecimal.ZERO;
        BigDecimal total = subtotal.subtract(discount).add(shipping);
        BigDecimal prepayment = defaultPrepayment(invoice);
        BigDecimal rounding = defaultRounding(invoice);
        BigDecimal payable = total.subtract(prepayment).add(rounding);
        return buildResult(invoice, subtotal, discount, vat, shipping, total, prepayment, rounding, payable);
    }
}


