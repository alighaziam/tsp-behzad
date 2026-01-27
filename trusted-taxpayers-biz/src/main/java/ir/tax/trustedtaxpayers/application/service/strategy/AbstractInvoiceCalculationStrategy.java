package ir.tax.trustedtaxpayers.application.service.strategy.impl;

import ir.tax.trustedtaxpayers.beans.model.Invoice;
import ir.tax.trustedtaxpayers.beans.model.InvoiceItem;
import ir.tax.trustedtaxpayers.application.service.strategy.InvoiceCalculationStrategy;

import java.math.BigDecimal;

public abstract class AbstractInvoiceCalculationStrategy implements InvoiceCalculationStrategy {

    protected BigDecimal calculateSubtotal(Invoice invoice) {
        return invoice.getItems().stream()
                .map(InvoiceItem::getLineSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    protected BigDecimal calculateDiscount(Invoice invoice) {
        return invoice.getItems().stream()
                .map(InvoiceItem::getLineDiscount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    protected BigDecimal calculateVat(Invoice invoice) {
        return invoice.getItems().stream()
                .map(InvoiceItem::getVatAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    protected boolean hasBuyer(Invoice invoice) {
        return invoice.getBuyerId() != null;
    }

    protected BigDecimal defaultShipping(Invoice invoice) {
        return invoice.getShippingCost() == null ? BigDecimal.ZERO : invoice.getShippingCost();
    }

    protected BigDecimal defaultPrepayment(Invoice invoice) {
        return invoice.getPrepaymentAmount() == null ? BigDecimal.ZERO : invoice.getPrepaymentAmount();
    }

    protected BigDecimal defaultRounding(Invoice invoice) {
        return invoice.getRoundingAmount() == null ? BigDecimal.ZERO : invoice.getRoundingAmount();
    }

    protected Invoice buildResult(Invoice invoice, BigDecimal subtotal, BigDecimal discount, BigDecimal vat,
                                  BigDecimal shipping, BigDecimal total, BigDecimal prepayment, BigDecimal rounding, BigDecimal payable) {
        return new Invoice(
                invoice.getId(), invoice.getInvoiceNo(), invoice.getFiscalType(), invoice.getInvoiceType(),
                invoice.getIssueDate(), invoice.getCurrency(), invoice.getTaxpayerId(), invoice.getBuyerId(),
                subtotal, discount, vat, shipping, total, prepayment, rounding, payable,
                invoice.getStatus(), invoice.getItems()
        );
    }
}



