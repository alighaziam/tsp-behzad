package ir.tax.trustedtaxpayers.application.service.strategy.impl;

import ir.tax.trustedtaxpayers.beans.model.Invoice;
import ir.tax.trustedtaxpayers.beans.model.InvoiceType;

import java.math.BigDecimal;
import ir.tax.trustedtaxpayers.application.config.VatConfigProperties;

public class Type5UtilityStrategy extends AbstractInvoiceCalculationStrategy {
    private VatConfigProperties vatConfig;
    public Type5UtilityStrategy() {}
    public Type5UtilityStrategy(VatConfigProperties vatConfig) { this.vatConfig = vatConfig; }
    @Override
    public boolean supports(InvoiceType type) {
        return type == InvoiceType.TYPE5_UTILITY;
    }

    @Override
    public Invoice calculate(Invoice invoice) {
        BigDecimal subtotal = calculateSubtotal(invoice);
        BigDecimal discount = calculateDiscount(invoice);
        BigDecimal vat = calculateVat(invoice);
        BigDecimal shipping = BigDecimal.ZERO; // Utilities usually no shipping

        // TYPE5_UTILITY: Utility bills (water, electricity, gas)
        if (hasBuyer(invoice)) {
            // B2B Utility: Standard VAT
            BigDecimal total = subtotal.subtract(discount).add(vat);
            BigDecimal prepayment = defaultPrepayment(invoice);
            BigDecimal rounding = defaultRounding(invoice);
            BigDecimal payable = total.subtract(prepayment).add(rounding);
            return buildResult(invoice, subtotal, discount, vat, shipping, total, prepayment, rounding, payable);
        } else {
            // B2C Utility: Often reduced VAT or special rates
            int rate = vatConfig != null ? vatConfig.getUtilities().getB2cRate() : 6;
            BigDecimal consumerVat = subtotal.multiply(new BigDecimal(String.valueOf(rate))) // configurable
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
            BigDecimal total = subtotal.subtract(discount).add(consumerVat);
            BigDecimal prepayment = defaultPrepayment(invoice);
            BigDecimal rounding = defaultRounding(invoice);
            BigDecimal payable = total.subtract(prepayment).add(rounding);
            return buildResult(invoice, subtotal, discount, consumerVat, shipping, total, prepayment, rounding, payable);
        }
    }
}



