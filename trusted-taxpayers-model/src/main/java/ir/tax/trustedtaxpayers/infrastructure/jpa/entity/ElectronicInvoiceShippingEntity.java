package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.beans.model.ShippingType;
import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import ir.tax.trustedtaxpayers.infrastructure.jpa.converter.ShippingTypeConverter;
import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entity اطلاعات بارنامه و حمل
 * 
 * <p>این Entity شامل اطلاعات کامل بارنامه و حمل برای صورتحساب‌های الکترونیکی است.</p>
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Entity
@Table(name = "ELECTRONIC_INVOICE_SHIPPING")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ElectronicInvoiceShippingEntity extends BaseEntity {

    @Column(name = "HEADER_ID", nullable = false)
    @Convert(converter = RawUuidConverter.class)
    private UUID headerId;

    // Waybill Information
    @Column(name = "LNO", length = 50)
    private String lno;

    @Column(name = "LRNO", length = 50)
    private String lrno;

    // Origin Information
    @Column(name = "OCU", length = 3)
    private String ocu;

    @Column(name = "OCI", length = 100)
    private String oci;

    // Destination Information
    @Column(name = "DCO", length = 3)
    private String dco;

    @Column(name = "DCI", length = 100)
    private String dci;

    // Sender/Receiver Information
    @Column(name = "TID", length = 12)
    private String tid;

    @Column(name = "RID", length = 12)
    private String rid;

    // Shipping Type
    @Column(name = "LT")
    @Convert(converter = ShippingTypeConverter.class)
    private ShippingType lt; // 1=جاده‌ای, 2=ریلی, 3=هوایی, 4=دریایی

    // Fleet and Driver
    @Column(name = "CNO", length = 50)
    private String cno;

    @Column(name = "DID", length = 12)
    private String did;

    // Cargo Information
    @Column(name = "SG", length = 400)
    private String sg;

    @Column(name = "SGID", length = 13)
    private String sgid;

    @Column(name = "SGT", length = 400)
    private String sgt;

    // Customs Declaration
    @Column(name = "CDCN", length = 20)
    private String cdcn;

    @Column(name = "CDCD", length = 20)
    private String cdcd;

    // Container Information
    @Column(name = "CNID", length = 50)
    private String cnid;

    // Weight and Value Totals
    @Column(name = "TPWD", precision = 18, scale = 3)
    private BigDecimal tpwd;

    @Column(name = "TONW", precision = 18, scale = 3)
    private BigDecimal tonw;

    @Column(name = "TORV", precision = 18, scale = 0)
    private BigDecimal torv;

    @Column(name = "TOCV", precision = 18, scale = 2)
    private BigDecimal tocv;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HEADER_ID", insertable = false, updatable = false)
    private ElectronicInvoiceHeaderEntity header;
}



