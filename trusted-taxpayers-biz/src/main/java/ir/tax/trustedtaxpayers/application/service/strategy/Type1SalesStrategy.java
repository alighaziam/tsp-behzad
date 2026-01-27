package ir.tax.trustedtaxpayers.application.service.strategy.impl;

import ir.tax.trustedtaxpayers.beans.model.Invoice;
import ir.tax.trustedtaxpayers.beans.model.InvoiceType;

import java.math.BigDecimal;
import ir.tax.trustedtaxpayers.application.config.VatConfigProperties;
import java.math.RoundingMode;

public class Type1SalesStrategy extends AbstractInvoiceCalculationStrategy {
    private VatConfigProperties vatConfig;

    public Type1SalesStrategy() {}
    public Type1SalesStrategy(VatConfigProperties vatConfig) { this.vatConfig = vatConfig; }
    @Override
    public boolean supports(InvoiceType type) {
        return type == InvoiceType.TYPE1_SALES;
    }

    @Override
    public Invoice calculate(Invoice invoice) {
        BigDecimal subtotal = calculateSubtotal(invoice);
        BigDecimal discount = calculateDiscount(invoice);
        BigDecimal vat = calculateVat(invoice);
        BigDecimal shipping = defaultShipping(invoice);

        // TYPE1_SALES: B2B has full VAT, B2C may have reduced/zero VAT on some items
        if (hasBuyer(invoice)) {
            // B2B: Standard calculation
            BigDecimal total = subtotal.subtract(discount).add(vat).add(shipping);
            BigDecimal prepayment = defaultPrepayment(invoice);
            BigDecimal rounding = calculateRounding(total.subtract(prepayment));
            BigDecimal payable = total.subtract(prepayment).add(rounding);
            return buildResult(invoice, subtotal, discount, vat, shipping, total, prepayment, rounding, payable);
        } else {
            // B2C: May apply consumer tax rules
            BigDecimal taxable = subtotal.subtract(discount);
            int rate = vatConfig != null ? vatConfig.getType1().getB2cRate() : 9;
            BigDecimal consumerVat = taxable.multiply(new BigDecimal(String.valueOf(rate))) // configurable rate
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            BigDecimal total = taxable.add(consumerVat).add(shipping);
            BigDecimal prepayment = defaultPrepayment(invoice);
            BigDecimal rounding = calculateRounding(total.subtract(prepayment));
            BigDecimal payable = total.subtract(prepayment).add(rounding);
            return buildResult(invoice, subtotal, discount, consumerVat, shipping, total, prepayment, rounding, payable);
        }
    }

    private BigDecimal calculateRounding(BigDecimal amount) {
        // Round to nearest 1000 (Iranian currency convention)
        return amount.setScale(-3, RoundingMode.HALF_UP).subtract(amount);
    }
}



