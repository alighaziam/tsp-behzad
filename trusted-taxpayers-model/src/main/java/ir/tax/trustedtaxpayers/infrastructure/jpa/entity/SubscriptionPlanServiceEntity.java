package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "SUBSCRIPTION_PLAN_SERVICES")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanServiceEntity extends BaseEntity {

    @ManyToOne(optional = false)
    @JoinColumn(name = "PLAN_ID", nullable = false, updatable = false)
    private SubscriptionPlanEntity plan;

    @ManyToOne(optional = false)
    @JoinColumn(name = "SERVICE_ID", nullable = false, updatable = false)
    private ServiceEntity service;

    @Column(name = "LIMIT_VALUE", precision = 18, scale = 0)
    private BigDecimal limitValue;

    @Column(name = "LIMIT_UNIT", length = 50)
    private String limitUnit;

    @Column(name = "NOTES", length = 1000)
    private String notes;
}

