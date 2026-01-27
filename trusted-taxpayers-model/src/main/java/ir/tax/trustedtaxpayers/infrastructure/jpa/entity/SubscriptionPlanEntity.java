package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "SUBSCRIPTION_PLANS")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPlanEntity extends BaseEntity {

    @Column(name = "CODE", length = 50, nullable = false, unique = true)
    private String code;

    @Column(name = "NAME", length = 200, nullable = false)
    private String name;

    @Column(name = "DESCRIPTION", length = 1000)
    private String description;

    @Column(name = "PRICE", nullable = false, precision = 18, scale = 2)
    private BigDecimal price;

    @Column(name = "CURRENCY", length = 10, nullable = false)
    private String currency;

    @Column(name = "DURATION_DAYS", nullable = false)
    private Integer durationDays;

    @Column(name = "MAX_INVOICES")
    private Integer maxInvoices;

    @Column(name = "FEATURES", columnDefinition = "CLOB")
    private String features;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 50)
    private List<SubscriptionPlanServiceEntity> services;
}

