package ir.tax.trustedtaxpayers.application.service.strategy.impl;

import ir.tax.trustedtaxpayers.beans.model.Invoice;
import ir.tax.trustedtaxpayers.beans.model.InvoiceType;

import java.math.BigDecimal;
import java.util.EnumSet;

public class Type8to16TemplateStrategy extends AbstractInvoiceCalculationStrategy {
    private static final EnumSet<InvoiceType> SUPPORTED = EnumSet.of(
            InvoiceType.TYPE8_TEMPLATE,
            InvoiceType.TYPE9_TEMPLATE,
            InvoiceType.TYPE10_TEMPLATE,
            InvoiceType.TYPE11_TEMPLATE,
            InvoiceType.TYPE12_TEMPLATE,
            InvoiceType.TYPE13_TEMPLATE,
            InvoiceType.TYPE14_TEMPLATE,
            InvoiceType.TYPE15_TEMPLATE,
            InvoiceType.TYPE16_TEMPLATE
    );

    @Override
    public boolean supports(InvoiceType type) {
        return SUPPORTED.contains(type);
    }

    @Override
    public Invoice calculate(Invoice invoice) {
        BigDecimal subtotal = calculateSubtotal(invoice);
        BigDecimal discount = calculateDiscount(invoice);
        BigDecimal vat = calculateVat(invoice);
        BigDecimal shipping = defaultShipping(invoice);
        BigDecimal total = subtotal.subtract(discount).add(vat).add(shipping);
        BigDecimal prepayment = defaultPrepayment(invoice);
        BigDecimal rounding = defaultRounding(invoice);
        BigDecimal payable = total.subtract(prepayment).add(rounding);
        return buildResult(invoice, subtotal, discount, vat, shipping, total, prepayment, rounding, payable);
    }
}


