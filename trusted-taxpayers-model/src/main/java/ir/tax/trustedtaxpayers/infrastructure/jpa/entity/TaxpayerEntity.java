package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

/**
 * Entity مودی مالیاتی
 * 
 * <p>این Entity شامل اطلاعات مودی مالیاتی (فروشنده) است.</p>
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Entity
@Table(name = "TAXPAYER")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@BatchSize(size = 50)
public class TaxpayerEntity extends BaseEntity {


    @Column(name = "TAX_ID", nullable = false, unique = true, length = 50)
    private String taxId;

    @Column(name = "NAME", nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "TYPE_BASE_INFO_ID", referencedColumnName = "ID")
    private BaseInfoEntity type;

    @Column(name = "COUNTRY", length = 100)
    private String country;

    @Column(name = "PROVINCE", length = 100)
    private String province;

    @Column(name = "CITY", length = 100)
    private String city;

    @Column(name = "ADDRESS")
    private String address;

    @Column(name = "VAT_REGISTER_NO", length = 50)
    private String vatRegisterNo;

    @Column(name = "NATIONAL_ID", length = 20)
    private String nationalId;

    @Column(name = "POSTAL_CODE", length = 10)
    private String postalCode;

    @Column(name = "PASSPORT_NO", length = 20)
    private String passportNo;
}



