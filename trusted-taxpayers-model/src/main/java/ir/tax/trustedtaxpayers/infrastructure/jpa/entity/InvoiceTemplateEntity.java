package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * الگوهای آماده صورتحساب که کاربران می‌توانند ذخیره و استفاده کنند.
 * Invoice templates that users can save and reuse.
 */
@Entity
@Table(name = "INVOICE_TEMPLATES")
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceTemplateEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TAXPAYER_ID", nullable = false)
    private TaxpayerEntity taxpayer;

    @Column(name = "TEMPLATE_NAME", nullable = false, length = 200)
    private String templateName;

    @Column(name = "PATTERN_NUMBER", nullable = false)
    private Integer patternNumber;

    @Lob
    @Column(name = "HEADER_DATA", columnDefinition = "CLOB")
    private String headerData; // JSON string

    @Lob
    @Column(name = "ITEMS_DATA", columnDefinition = "CLOB")
    private String itemsData; // JSON string

    @Column(name = "DESCRIPTION", length = 500)
    private String description;
}

