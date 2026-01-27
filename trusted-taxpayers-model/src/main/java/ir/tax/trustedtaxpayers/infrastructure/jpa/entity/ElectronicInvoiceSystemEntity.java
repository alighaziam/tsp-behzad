package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ELECTRONIC_INVOICE_SYSTEM")
@Getter // استفاده از Getter/Setter به جای Data برای امنیت بیشتر
@Setter
@ToString(exclude = "header")
@EqualsAndHashCode(exclude = "header", callSuper = false)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@AttributeOverride(name = "id", column = @Column(name = "HEADER_ID", nullable = false, updatable = false))
public class ElectronicInvoiceSystemEntity extends BaseEntity {

    /*@Id
    @JdbcTypeCode(java.sql.Types.VARBINARY)
    @Column(name = "HEADER_ID", nullable = false)
    private UUID id;*/

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "HEADER_ID", insertable = false, updatable = false)
    @MapsId
    private ElectronicInvoiceHeaderEntity header;

    @Column(name = "SIGN", length = 250)
    private String sign;

    @Column(name = "CERT", length = 250)
    private String cert;

    @Column(name = "HASH", length = 128)
    private String hash;

    @Column(name = "STS", length = 20)
    private String sts; // DRAFT, SUBMITTED, ACCEPTED, REJECTED, CANCELLED, PENDING

    @Column(name = "REFERENCE_NUMBER", length = 50)
    private String referenceNumber;

    @Column(name = "SRVDT")
    private OffsetDateTime srvdt;

    @Column(name = "REJCODE", length = 20)
    private String rejcode;

    @Column(name = "REJDESC", length = 400)
    private String rejdesc;
}


