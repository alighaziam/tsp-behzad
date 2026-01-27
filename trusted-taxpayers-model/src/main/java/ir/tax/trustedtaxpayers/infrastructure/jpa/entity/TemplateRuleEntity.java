package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity قاعده Template
 * 
 * <p>این Entity شامل قواعد اعتبارسنجی برای هر Template است.</p>
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Entity
@Table(name = "TEMPLATE_RULE")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class TemplateRuleEntity extends BaseEntity {
    @Column(name = "TEMPLATE_CODE", nullable = false, length = 10)
    private String templateCode;

    @Column(name = "FIELD_NAME", nullable = false, length = 100)
    private String fieldName;

    @Column(name = "RULE_TYPE", nullable = false, length = 30)
    private String ruleType;

    @Column(name = "RULE_PARAMS", length = 4000)
    private String ruleParams;

    @Column(name = "EXPLANATION", length = 4000)
    private String explanation;

    @Column(name = "ACTIVE", nullable = false, length = 1)
    private String active;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TEMPLATE_CODE", insertable = false, updatable = false)
    private TemplateEntity template;
}



