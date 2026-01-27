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

@Entity
@Table(name = "TAXPAYER_SUBSCRIPTIONS")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TaxpayerSubscriptionEntity extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "TAXPAYER_ID", nullable = false, updatable = false)
    private TaxpayerEntity taxpayer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "PLAN_ID", nullable = false, updatable = false)
    private SubscriptionPlanEntity plan;

    @Column(name = "START_DATE", nullable = false)
    private LocalDate startDate;

    @Column(name = "END_DATE", nullable = false)
    private LocalDate endDate;

    @Column(name = "STATUS", length = 20, nullable = false)
    private String status;

    @Column(name = "REMAINING_INVOICES")
    private Integer remainingInvoices;

    @Column(name = "PAYMENT_REFERENCE", length = 200)
    private String paymentReference;

    @Column(name = "NOTES", length = 1000)
    private String notes;

    @Column(name = "TAXPAYER_ID", insertable = false, updatable = false)
    @Convert(converter = RawUuidConverter.class)
    private UUID taxpayerId;

    @Column(name = "PLAN_ID", insertable = false, updatable = false)
    @Convert(converter = RawUuidConverter.class)
    private UUID planId;
}

