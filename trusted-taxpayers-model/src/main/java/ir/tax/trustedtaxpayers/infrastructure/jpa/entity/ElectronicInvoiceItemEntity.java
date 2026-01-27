package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entity ردیف صورتحساب الکترونیکی
 * 
 * <p>این Entity شامل اطلاعات اقلام/خدمات در صورتحساب الکترونیکی است.</p>
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Entity
@Table(name = "ELECTRONIC_INVOICE_ITEM")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@BatchSize(size = 50)
public class ElectronicInvoiceItemEntity extends BaseEntity {

    @Column(name = "HEADER_ID", nullable = false)
    @Convert(converter = RawUuidConverter.class)
    private UUID headerId;

    @Column(name = "LINE_NO", nullable = false)
    private Integer lineNo;

    // Product/Service Information
    @Column(name = "SSTID", nullable = false, length = 13)
    private String sstid;

    @Column(name = "SSTT", length = 400)
    private String sstt;

    // Quantity and Measurement
    @Column(name = "AM", nullable = false, precision = 18, scale = 8)
    private BigDecimal am;

    @Column(name = "MU", nullable = false, length = 8)
    private String mu;

    @Column(name = "NW", precision = 15, scale = 8)
    private BigDecimal nw;

    // Pricing
    @Column(name = "FEE", nullable = false, precision = 18, scale = 8)
    private BigDecimal fee;

    @Column(name = "CFEE", precision = 14, scale = 4)
    private BigDecimal cfee;

    @Column(name = "CUT", length = 3)
    private String cut;

    @Column(name = "EXR", precision = 18, scale = 8)
    private BigDecimal exr;

    @Column(name = "CPR", precision = 18, scale = 8)
    private BigDecimal cpr;

    // Values
    @Column(name = "SSRV", precision = 18, scale = 0)
    private BigDecimal ssrv;

    @Column(name = "SSCV", precision = 14, scale = 4)
    private BigDecimal sscv;

    // Discounts
    @Column(name = "PRDIS", precision = 18, scale = 0)
    private BigDecimal prdis;

    @Column(name = "DIS", precision = 18, scale = 0)
    private BigDecimal dis;

    @Column(name = "ADIS", precision = 18, scale = 0)
    private BigDecimal adis;

    // VAT
    @Column(name = "VRA", precision = 18, scale = 8)
    private BigDecimal vra;

    @Column(name = "VAM", precision = 18, scale = 0)
    private BigDecimal vam;

    @Column(name = "VBA", precision = 18, scale = 0)
    private BigDecimal vba;

    @Column(name = "VOP", precision = 18, scale = 0)
    private BigDecimal vop;

    // Other Taxes
    @Column(name = "ODT", length = 400)
    private String odt;

    @Column(name = "ODR", precision = 18, scale = 8)
    private BigDecimal odr;

    @Column(name = "ODAM", precision = 18, scale = 0)
    private BigDecimal odam;

    // Other Legal Fees
    @Column(name = "OLT", length = 400)
    private String olt;

    @Column(name = "OLR", precision = 18, scale = 8)
    private BigDecimal olr;

    @Column(name = "OLAM", precision = 18, scale = 0)
    private BigDecimal olam;

    // Construction and Brokerage
    @Column(name = "CONSFEE", precision = 18, scale = 0)
    private BigDecimal consfee;

    @Column(name = "SPRO", precision = 18, scale = 0)
    private BigDecimal spro;

    @Column(name = "BROS", precision = 18, scale = 0)
    private BigDecimal bros;

    @Column(name = "TCPBS", precision = 18, scale = 0)
    private BigDecimal tcpbs;

    @Column(name = "BSRN", length = 50)
    private String bsrn;

    // Payment Share
    @Column(name = "COP", precision = 18, scale = 0)
    private BigDecimal cop;

    // Gold/Precious Metals
    @Column(name = "CUI", precision = 18, scale = 8)
    private BigDecimal cui;

    // Totals
    @Column(name = "TSSTAM", precision = 18, scale = 0)
    private BigDecimal tsstam;

    // Medical Products
    @Column(name = "MEDID", length = 50)
    private String medid;

    @Column(name = "ASPR", precision = 18, scale = 2)
    private BigDecimal aspr;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HEADER_ID", insertable = false, updatable = false)
    private ElectronicInvoiceHeaderEntity header;
}



