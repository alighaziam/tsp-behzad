package ir.tax.trustedtaxpayers.application.service.strategy.impl;

import ir.tax.trustedtaxpayers.beans.model.Invoice;
import ir.tax.trustedtaxpayers.beans.model.InvoiceItem;
import ir.tax.trustedtaxpayers.beans.model.InvoiceType;
import ir.tax.trustedtaxpayers.application.service.strategy.InvoiceCalculationStrategy;

import java.math.BigDecimal;

public class DefaultInvoiceCalculationStrategy implements InvoiceCalculationStrategy {
    @Override
    public boolean supports(InvoiceType type) {
        return true; // fallback for all types
    }

    @Override
    public Invoice calculate(Invoice invoice) {
        BigDecimal subtotal = invoice.getItems().stream()
                .map(InvoiceItem::getLineSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = invoice.getItems().stream()
                .map(InvoiceItem::getLineDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vat = invoice.getItems().stream()
                .map(InvoiceItem::getVatAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal shipping = invoice.getShippingCost() == null ? BigDecimal.ZERO : invoice.getShippingCost();
        BigDecimal total = subtotal.subtract(discount).add(vat).add(shipping);
        BigDecimal prepay = invoice.getPrepaymentAmount() == null ? BigDecimal.ZERO : invoice.getPrepaymentAmount();
        BigDecimal rounding = invoice.getRoundingAmount() == null ? BigDecimal.ZERO : invoice.getRoundingAmount();
        BigDecimal payable = total.subtract(prepay).add(rounding);

        return new Invoice(
                invoice.getId(),
                invoice.getInvoiceNo(),
                invoice.getFiscalType(),
                invoice.getInvoiceType(),
                invoice.getIssueDate(),
                invoice.getCurrency(),
                invoice.getTaxpayerId(),
                invoice.getBuyerId(),
                subtotal,
                discount,
                vat,
                shipping,
                total,
                prepay,
                rounding,
                payable,
                invoice.getStatus(),
                invoice.getItems()
        );
    }
}




