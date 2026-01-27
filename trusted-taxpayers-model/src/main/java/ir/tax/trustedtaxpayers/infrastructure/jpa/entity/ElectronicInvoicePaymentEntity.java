package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Entity پرداخت صورتحساب الکترونیکی
 * 
 * <p>این Entity شامل اطلاعات پرداخت‌های صورتحساب الکترونیکی است.</p>
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Entity
@Table(name = "ELECTRONIC_INVOICE_PAYMENT")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ElectronicInvoicePaymentEntity extends BaseEntity {

    @Column(name = "HEADER_ID", nullable = false)
    @Convert(converter = RawUuidConverter.class)
    private UUID headerId;

    @Column(name = "PMT", nullable = false, length = 20)
    private String pmt;

    @Column(name = "PV", nullable = false, precision = 18, scale = 0)
    private BigDecimal pv;

    @Column(name = "PDT", nullable = false)
    private OffsetDateTime pdt;

    @Column(name = "IINN", length = 50)
    private String iinn;

    @Column(name = "ACN", length = 50)
    private String acn;

    @Column(name = "TRMN", length = 50)
    private String trmn;

    @Column(name = "TRN", length = 50)
    private String trn;

    @Column(name = "PCN", length = 64)
    private String pcn;

    @Column(name = "PID", length = 12)
    private String pid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HEADER_ID", insertable = false, updatable = false)
    private ElectronicInvoiceHeaderEntity header;
}



