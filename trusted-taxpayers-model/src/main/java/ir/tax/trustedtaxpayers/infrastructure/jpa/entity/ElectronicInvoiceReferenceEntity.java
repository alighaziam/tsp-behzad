package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Entity مرجع صورتحساب الکترونیکی
 * 
 * <p>این Entity شامل اطلاعات صورتحساب‌های مرجع است.</p>
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Entity
@Table(name = "ELECTRONIC_INVOICE_REFERENCE")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ElectronicInvoiceReferenceEntity extends BaseEntity {

    @Column(name = "HEADER_ID", nullable = false)
    @Convert(converter = RawUuidConverter.class)
    private UUID headerId;

    @Column(name = "IRTAXID", length = 22)
    private String irtaxid;

    @Column(name = "INREF", length = 50)
    private String inref;

    @Column(name = "REFTYP", length = 10)
    private String reftyp;

    @Column(name = "ASNO", length = 50)
    private String asno;

    @Column(name = "ASDT")
    private LocalDate asdt;

    @Column(name = "BSRN", length = 30)
    private String bsrn;

    @Column(name = "SOVAT", length = 50)
    private String sovat;

    @Column(name = "BPN", length = 50)
    private String bpn;

    @Column(name = "AN", length = 50)
    private String an;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HEADER_ID", insertable = false, updatable = false)
    private ElectronicInvoiceHeaderEntity header;
}



