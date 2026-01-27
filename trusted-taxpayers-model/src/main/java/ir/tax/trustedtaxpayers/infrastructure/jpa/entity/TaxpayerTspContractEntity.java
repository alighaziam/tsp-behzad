package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "TAXPAYER_TSP_CONTRACTS")
@AttributeOverrides({
        @AttributeOverride(name = "createdOn", column = @Column(name = "CREATED_ON")),
        @AttributeOverride(name = "updatedOn", column = @Column(name = "UPDATED_ON")),
        @AttributeOverride(name = "createdBy", column = @Column(name = "CREATED_BY")),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "UPDATED_BY"))
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TaxpayerTspContractEntity extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "TAXPAYER_ID", nullable = false, updatable = false)
    private TaxpayerEntity taxpayer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "TSP_ID", nullable = false, updatable = false)
    private TrustedServiceProviderEntity provider;

    @Column(name = "CONTRACT_NUMBER", length = 100)
    private String contractNumber;

    @Column(name = "CONTRACT_TITLE", length = 200)
    private String contractTitle;

    @Column(name = "SIGNED_AT", nullable = false)
    private LocalDate signedAt;

    @Column(name = "EXPIRES_AT")
    private LocalDate expiresAt;

    @Column(name = "STATUS", length = 20, nullable = false)
    private String status;

    @Column(name = "NOTES", length = 1000)
    private String notes;

    @Column(name = "TAXPAYER_ID", insertable = false, updatable = false)
    @Convert(converter = RawUuidConverter.class)
    private UUID taxpayerId;

    @Column(name = "TSP_ID", insertable = false, updatable = false)
    @Convert(converter = RawUuidConverter.class)
    private UUID providerId;
}


