package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import ir.tax.trustedtaxpayers.enums.ProductTaxable;
import ir.tax.trustedtaxpayers.enums.TaxProductType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Entity محصول مالیاتی
 * 
 * <p>این Entity شامل اطلاعات محصولات مالیاتی و نوع مشمولیت مالیاتی آنها است.</p>
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Entity
@Table(name = "TAX_PRODUCT")
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
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
public class TaxProductEntity extends BaseEntity {

    @Column(name = "TAX_PRODUCT_ID", length = 500)
    private String taxProductId;

    @Column(name = "PRODUCT_TITLE", length = 1000)
    private String productTitle;

    @Column(name = "VAT")
    private Integer vat;

    @Enumerated(EnumType.STRING)
    @Column(name = "TAXABLE", length = 20, nullable = false)
    private ProductTaxable taxable;

    @Column(name = "RUN_DATE", nullable = false)
    private LocalDate runDate;

    @Column(name = "EXPIRATION")
    private LocalDate expiration;

    @Enumerated(EnumType.STRING)
    @Column(name = "TYPE", length = 50, nullable = false)
    private TaxProductType type;

    @Column(name = "PRICING_DESCRIPTION", length = 1000)
    private String pricingDescription;

    @Column(name = "TAX_CREATE_DATE")
    private LocalDate taxCreateDate;

    @Column(name = "TAX_LAST_EDIT_DATE")
    private LocalDate taxLastEditDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MEASUREMENT_UNIT_ID", nullable = true)
    private BaseInfoEntity measurementUnit;
}

