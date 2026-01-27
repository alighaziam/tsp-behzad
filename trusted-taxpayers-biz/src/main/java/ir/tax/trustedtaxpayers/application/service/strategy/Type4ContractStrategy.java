package ir.tax.trustedtaxpayers.application.service.strategy.impl;

import ir.tax.trustedtaxpayers.beans.model.Invoice;
import ir.tax.trustedtaxpayers.beans.model.InvoiceType;

import java.math.BigDecimal;

public class Type4ContractStrategy extends AbstractInvoiceCalculationStrategy {
    @Override
    public boolean supports(InvoiceType type) {
        return type == InvoiceType.TYPE4_CONTRACT;
    }

    @Override
    public Invoice calculate(Invoice invoice) {
        BigDecimal subtotal = calculateSubtotal(invoice);
        BigDecimal discount = calculateDiscount(invoice);
        BigDecimal vat = calculateVat(invoice);
        BigDecimal shipping = defaultShipping(invoice);

        // TYPE4_CONTRACT: Contract-based invoices
        if (hasBuyer(invoice)) {
            // B2B Contract: Full calculation with potential retention
            BigDecimal total = subtotal.subtract(discount).add(vat).add(shipping);
            BigDecimal prepayment = defaultPrepayment(invoice);
            BigDecimal rounding = defaultRounding(invoice);
            BigDecimal payable = total.subtract(prepayment).add(rounding);
            return buildResult(invoice, subtotal, discount, vat, shipping, total, prepayment, rounding, payable);
        } else {
            // B2C Contract: Rare but similar to B2B
            BigDecimal total = subtotal.subtract(discount).add(vat).add(shipping);
            BigDecimal prepayment = defaultPrepayment(invoice);
            BigDecimal rounding = defaultRounding(invoice);
            BigDecimal payable = total.subtract(prepayment).add(rounding);
            return buildResult(invoice, subtotal, discount, vat, shipping, total, prepayment, rounding, payable);
        }
    }
}



