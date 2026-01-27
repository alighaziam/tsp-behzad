package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * تنظیمات پیش‌فرض صورتحساب هر کاربر/مودی.
 */
@Entity
@Table(
        name = "USER_INVOICE_PREFERENCES",
        uniqueConstraints = {
                @UniqueConstraint(name = "UX_USER_PREF_USER_TAXPAYER", columnNames = {"USER_ID", "TAXPAYER_ID"})
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserInvoicePreferenceEntity extends BaseEntity {

    @Column(name = "USER_ID", nullable = false)
    @Convert(converter = RawUuidConverter.class)
    private UUID userId;

    @Column(name = "TAXPAYER_ID", nullable = false)
    @Convert(converter = RawUuidConverter.class)
    private UUID taxpayerId;

    @Column(name = "DEFAULT_TEMPLATE_CODE", length = 10)
    private String defaultTemplateCode;

    @Column(name = "DEFAULT_TEMPLATE_TYPE_CODE", length = 10)
    private String defaultTemplateTypeCode;

    @Column(name = "DEFAULT_PATTERN_NUMBER")
    private Integer defaultPatternNumber;

    @Column(name = "DEFAULT_INS")
    private Integer defaultIns;

    @Column(name = "LAST_USED_TEMPLATE_CODE", length = 10)
    private String lastUsedTemplateCode;

    @Column(name = "FIRST_LOGIN_COMPLETED", nullable = false)
    private boolean firstLoginCompleted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", insertable = false, updatable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TAXPAYER_ID", insertable = false, updatable = false)
    private TaxpayerEntity taxpayer;
}

