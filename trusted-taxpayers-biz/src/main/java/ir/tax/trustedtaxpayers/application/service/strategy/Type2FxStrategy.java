package ir.tax.trustedtaxpayers.application.service.strategy.impl;

import ir.tax.trustedtaxpayers.beans.model.Invoice;
import ir.tax.trustedtaxpayers.beans.model.InvoiceType;

import java.math.BigDecimal;

public class Type2FxStrategy extends AbstractInvoiceCalculationStrategy {
    @Override
    public boolean supports(InvoiceType type) {
        return type == InvoiceType.TYPE2_FX;
    }

    @Override
    public Invoice calculate(Invoice invoice) {
        BigDecimal subtotal = calculateSubtotal(invoice);
        BigDecimal discount = calculateDiscount(invoice);
        BigDecimal vat = calculateVat(invoice);
        BigDecimal shipping = defaultShipping(invoice);

        // TYPE2_FX: Foreign exchange transactions, different rules
        if (hasBuyer(invoice)) {
            // B2B FX: Full VAT
            BigDecimal total = subtotal.subtract(discount).add(vat).add(shipping);
            BigDecimal prepayment = defaultPrepayment(invoice);
            BigDecimal rounding = defaultRounding(invoice);
            BigDecimal payable = total.subtract(prepayment).add(rounding);
            return buildResult(invoice, subtotal, discount, vat, shipping, total, prepayment, rounding, payable);
        } else {
            // B2C FX: May be exempt or reduced rate
            BigDecimal total = subtotal.subtract(discount).add(shipping); // VAT exempt for consumers
            BigDecimal prepayment = defaultPrepayment(invoice);
            BigDecimal rounding = defaultRounding(invoice);
            BigDecimal payable = total.subtract(prepayment).add(rounding);
            return buildResult(invoice, subtotal, discount, BigDecimal.ZERO, shipping, total, prepayment, rounding, payable);
        }
    }
}



