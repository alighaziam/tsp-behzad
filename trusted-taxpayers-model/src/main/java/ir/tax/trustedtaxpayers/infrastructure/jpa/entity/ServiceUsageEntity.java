package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "SERVICE_USAGE")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceUsageEntity extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "TAXPAYER_ID", nullable = false, updatable = false)
    private TaxpayerEntity taxpayer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "SUBSCRIPTION_ID", nullable = false, updatable = false)
    private TaxpayerSubscriptionEntity subscription;

    @ManyToOne(optional = false)
    @JoinColumn(name = "SERVICE_ID", nullable = false, updatable = false)
    private ServiceEntity service;

    @Column(name = "SERVICE_CODE", length = 50, nullable = false)
    private String serviceCode;

    @Column(name = "USAGE_DATE", nullable = false)
    private LocalDate usageDate;

    @Column(name = "USAGE_COUNT", nullable = false)
    @Builder.Default
    private Integer usageCount = 1;

    @Column(name = "USAGE_TYPE", length = 50, nullable = false)
    private String usageType;

    @Column(name = "REFERENCE_ID")
    @Convert(converter = RawUuidConverter.class)
    private UUID referenceId;

    @Column(name = "REFERENCE_TYPE", length = 50)
    private String referenceType;

    @Column(name = "NOTES", length = 1000)
    private String notes;
}

