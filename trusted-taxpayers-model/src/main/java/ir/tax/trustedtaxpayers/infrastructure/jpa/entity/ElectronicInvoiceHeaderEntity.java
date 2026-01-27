package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.beans.model.*;
import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import ir.tax.trustedtaxpayers.enums.FlightType;
import ir.tax.trustedtaxpayers.enums.InvoiceStatus;
import ir.tax.trustedtaxpayers.infrastructure.jpa.converter.*;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;

/**
 * Entity سربرگ صورتحساب الکترونیکی
 * 
 * <p>این Entity شامل تمام اطلاعات سربرگ صورتحساب الکترونیکی بر اساس استاندارد RC_IITP_IS_V7.6 است.</p>
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Entity
@Table(name = "ELECTRONIC_INVOICE_HEADER")
@Data
@EqualsAndHashCode(callSuper = true) // پارامترهای exclude را پاک کنید
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
/*@BatchSize(size = 50)*/
public class ElectronicInvoiceHeaderEntity extends BaseEntity {

    @Column(name = "TAX_ID", nullable = false,  length = 100)
    private String taxId;

    @Column(name = "TEMPLATE_CODE", length = 100)
    private String templateCode;

    @Column(name = "TEMPLATE_SUBTYPE", length = 100)
    private String templateSubtype;

    // Basic Invoice Information
    @Column(name = "INTY")
    @Convert(converter = InvoiceTypeConverter.class)
    private InvoiceType inty; // 1=Sale, 2=Return, 3=Corrective

    @Column(name = "INNO",  length = 100)
    private String inno;

    @Column(name = "INDATIM" )
    private Long indatim; // Unix Time

    @Column(name = "M2_INDATI")
    private Long m2Indati; // Unix Time

    @Column(name = "INP")
    private Integer inp; // 1-13

    @Column(name = "INS")
    @Convert(converter = InvoiceSubjectConverter.class)
    private InvoiceSubject ins; // 1=اصلی, 2=اصلاحی, 3=ابطالی, 4=برگشت

    // Reference Invoice
    @Column(name = "IRTAXID", length = 100)
    private String irtaxid;

    // Seller Information
    @Column(name = "TINS",  length = 100)
    private String tins;

    @Column(name = "SBC", length = 100)
    private String sbc;

    // Buyer Information
    @Column(name = "TOB")
    @Convert(converter = PersonTypeConverter.class)
    private PersonType tob; // 1=حقیقی, 2=حقوقی, 3=مشارکت مدنی, 4=اتباع غیر ایرانی

    @Column(name = "BID", length = 12)
    private String bid;

    @Column(name = "TINB", length = 14)
    private String tinb;

    @Column(name = "BBC", length = 4)
    private String bbc;

    @Column(name = "BPC", length = 10)
    private String bpc;

    @Column(name = "BPN", length = 9)
    private String bpn;

    // Flight Information
    @Column(name = "FT")
    @Convert(converter = FlightTypeConverter.class)
    private FlightType ft; // 1=داخلی, 2=خارجی

    // Customs Information
    @Column(name = "SCLN", length = 100)
    private String scln;

    @Column(name = "SCC", length = 100)
    private String scc;

    @Column(name = "CDCN", length = 100)
    private String cdcn;

    @Column(name = "CDCD")
    private Integer cdcd; // Unix Time - 5 digits

    // Contract Information
    @Column(name = "CRN", length = 100)
    private String crn;

    // Subscription/Bill Information
    @Column(name = "BILLID", length = 100)
    private String billid;

    @Column(name = "BILLING_ID", length = 100)
    private String billingId;

    // Financial Totals
    @Column(name = "TPRDIS", precision = 18, scale = 0)
    private BigDecimal tprdis;

    @Column(name = "TDIS", precision = 18, scale = 0)
    private BigDecimal tdis;

    @Column(name = "TADIS", precision = 18, scale = 0)
    private BigDecimal tadis;

    @Column(name = "TVAM", precision = 18, scale = 0)
    private BigDecimal tvam;

    @Column(name = "TODAM", precision = 18, scale = 0)
    private BigDecimal todam;

    @Column(name = "TBILL", precision = 18, scale = 0)
    private BigDecimal tbill;

    @Column(name = "TVOP", precision = 18, scale = 0)
    private BigDecimal tvop;

    @Column(name = "TAX17", precision = 18, scale = 0)
    private BigDecimal tax17;

    // Weight and Value Totals
    @Column(name = "TONW", precision = 15, scale = 8)
    private BigDecimal tonw;

    @Column(name = "TORV", precision = 18, scale = 0)
    private BigDecimal torv;

    @Column(name = "TOCV", precision = 14, scale = 4)
    private BigDecimal tocv;

    // Settlement Information
    @Column(name = "SETM")
    @Convert(converter = SettlementMethodConverter.class)
    private SettlementMethod setm; // 1=نقدی, 2=نسیه, 3=نقدی/نسیه

    @Column(name = "CAP", precision = 18, scale = 0)
    private BigDecimal cap;

    @Column(name = "INSP", precision = 18, scale = 0)
    private BigDecimal insp;

    // Agency Information
    @Column(name = "TINC", length = 100)
    private String tinc;

    // Insurance Information
    @Column(name = "IN_INSURANCE_ID", length = 100)
    private String inInsuranceId;

    @Column(name = "AN", length = 100)
    private String an;

    // Exchange Sale Notice
    @Column(name = "ASN", length = 100)
    private String asn;

    @Column(name = "ASD")
    private Long asd; // Unix Time

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TEMPLATE_CODE", insertable = false, updatable = false)
    private TemplateEntity template;

    // Relations to Taxpayers (Seller and Buyer)
    /*@ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JoinColumn(name = "SELLER_TAXPAYER_ID", nullable = false)
    private TaxpayerEntity sellerTaxpayer;

    @ManyToOne(fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JoinColumn(name = "BUYER_TAXPAYER_ID")
    private TaxpayerEntity buyerTaxpayer;*/

    // Invoice Status
    @Column(name = "STATUS", length = 100)
    @Enumerated(EnumType.STRING)
    private InvoiceStatus status = InvoiceStatus.READY_TO_SEND;

    // Helper methods for enum conversion (only for fields that are still Integer)
    
    /**
     * دریافت InvoicePattern به صورت enum
     */
    public InvoicePattern getInpEnum() {
        return inp != null ? InvoicePattern.fromValue(inp) : null;
    }

    /**
     * تنظیم InvoicePattern از enum
     */
    public void setInpEnum(InvoicePattern pattern) {
        this.inp = pattern != null ? pattern.getValue() : null;
    }
}



