package ir.tax.trustedtaxpayers.beans.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class Invoice {
    private UUID id;
    private String invoiceNo;
    private String fiscalType;
    private String invoiceType;
    private OffsetDateTime issueDate;
    private String currency;
    private UUID taxpayerId;
    private UUID buyerId;
    private BigDecimal subtotalAmount;
    private BigDecimal discountAmount;
    private BigDecimal taxAmount;
    private BigDecimal shippingCost;
    private BigDecimal totalAmount;
    private BigDecimal prepaymentAmount;
    private BigDecimal roundingAmount;
    private BigDecimal payableAmount;
    private String status;
    private List<InvoiceItem> items;

    public Invoice(UUID id, String invoiceNo, String fiscalType, String invoiceType, OffsetDateTime issueDate, String currency, UUID taxpayerId, UUID buyerId, BigDecimal subtotalAmount, BigDecimal discountAmount, BigDecimal taxAmount, BigDecimal shippingCost, BigDecimal totalAmount, BigDecimal prepaymentAmount, BigDecimal roundingAmount, BigDecimal payableAmount, String status, List<InvoiceItem> items) {
        this.id = id;
        this.invoiceNo = invoiceNo;
        this.fiscalType = fiscalType;
        this.invoiceType = invoiceType;
        this.issueDate = issueDate;
        this.currency = currency;
        this.taxpayerId = taxpayerId;
        this.buyerId = buyerId;
        this.subtotalAmount = subtotalAmount;
        this.discountAmount = discountAmount;
        this.taxAmount = taxAmount;
        this.shippingCost = shippingCost;
        this.totalAmount = totalAmount;
        this.prepaymentAmount = prepaymentAmount;
        this.roundingAmount = roundingAmount;
        this.payableAmount = payableAmount;
        this.status = status;
        this.items = items;
    }

    public UUID getId() { return id; }
    public String getInvoiceNo() { return invoiceNo; }
    public String getFiscalType() { return fiscalType; }
    public String getInvoiceType() { return invoiceType; }
    public OffsetDateTime getIssueDate() { return issueDate; }
    public String getCurrency() { return currency; }
    public UUID getTaxpayerId() { return taxpayerId; }
    public UUID getBuyerId() { return buyerId; }
    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getShippingCost() { return shippingCost; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getPrepaymentAmount() { return prepaymentAmount; }
    public BigDecimal getRoundingAmount() { return roundingAmount; }
    public BigDecimal getPayableAmount() { return payableAmount; }
    public String getStatus() { return status; }
    public List<InvoiceItem> getItems() { return items; }
}


