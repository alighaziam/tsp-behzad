package ir.tax.trustedtaxpayers.application.service.strategy.impl;

import ir.tax.trustedtaxpayers.beans.model.Invoice;
import ir.tax.trustedtaxpayers.beans.model.InvoiceType;

import java.math.BigDecimal;

public class Type6AirStrategy extends AbstractInvoiceCalculationStrategy {
    @Override
    public boolean supports(InvoiceType type) {
        return type == InvoiceType.TYPE6_AIR;
    }

    @Override
    public Invoice calculate(Invoice invoice) {
        BigDecimal subtotal = calculateSubtotal(invoice);
        BigDecimal discount = calculateDiscount(invoice);
        BigDecimal vat = calculateVat(invoice);
        BigDecimal shipping = BigDecimal.ZERO; // tickets: no shipping

        // Air travel often VAT-exempt for consumers, standard for businesses
        if (hasBuyer(invoice)) {
            BigDecimal total = subtotal.subtract(discount).add(vat);
            BigDecimal prepayment = defaultPrepayment(invoice);
            BigDecimal rounding = defaultRounding(invoice);
            BigDecimal payable = total.subtract(prepayment).add(rounding);
            return buildResult(invoice, subtotal, discount, vat, shipping, total, prepayment, rounding, payable);
        } else {
            BigDecimal total = subtotal.subtract(discount);
            BigDecimal prepayment = defaultPrepayment(invoice);
            BigDecimal rounding = defaultRounding(invoice);
            BigDecimal payable = total.subtract(prepayment).add(rounding);
            return buildResult(invoice, subtotal, discount, BigDecimal.ZERO, shipping, total, prepayment, rounding, payable);
        }
    }
}


