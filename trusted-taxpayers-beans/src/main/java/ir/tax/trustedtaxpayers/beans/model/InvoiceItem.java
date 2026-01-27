package ir.tax.trustedtaxpayers.beans.model;

import java.math.BigDecimal;
import java.util.UUID;

public class InvoiceItem {
    private UUID id;
    private int lineNo;
    private String itemCode;
    private String itemName;
    private String unit;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineSubtotal;
    private BigDecimal lineDiscount;
    private BigDecimal vatRate;
    private BigDecimal vatAmount;
    private BigDecimal lineTotal;

    public InvoiceItem(UUID id, int lineNo, String itemCode, String itemName, String unit, BigDecimal quantity, BigDecimal unitPrice, BigDecimal lineSubtotal, BigDecimal lineDiscount, BigDecimal vatRate, BigDecimal vatAmount, BigDecimal lineTotal) {
        this.id = id;
        this.lineNo = lineNo;
        this.itemCode = itemCode;
        this.itemName = itemName;
        this.unit = unit;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.lineSubtotal = lineSubtotal;
        this.lineDiscount = lineDiscount;
        this.vatRate = vatRate;
        this.vatAmount = vatAmount;
        this.lineTotal = lineTotal;
    }

    public UUID getId() { return id; }
    public int getLineNo() { return lineNo; }
    public String getItemCode() { return itemCode; }
    public String getItemName() { return itemName; }
    public String getUnit() { return unit; }
    public BigDecimal getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getLineSubtotal() { return lineSubtotal; }
    public BigDecimal getLineDiscount() { return lineDiscount; }
    public BigDecimal getVatRate() { return vatRate; }
    public BigDecimal getVatAmount() { return vatAmount; }
    public BigDecimal getLineTotal() { return lineTotal; }
}


