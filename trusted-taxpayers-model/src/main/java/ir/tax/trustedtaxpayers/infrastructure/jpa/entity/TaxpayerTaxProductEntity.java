package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Entity واسط بین Taxpayer و TaxProduct
 * 
 * <p>این Entity رابطه Many-to-Many بین مودی مالیاتی و محصول مالیاتی را برقرار می‌کند.</p>
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Entity
@Table(name = "TAXPAYER_TAX_PRODUCT", 
       uniqueConstraints = @UniqueConstraint(name = "UK_TAXPAYER_TAX_PRODUCT", 
                                            columnNames = {"TAXPAYER_ID", "TAX_PRODUCT_ID"}))
@JsonIgnoreProperties(ignoreUnknown = true)
@IdClass(TaxpayerTaxProductEntity.Pk.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxpayerTaxProductEntity {
    
    @Id
    @Column(name = "TAXPAYER_ID", nullable = false)
    @Convert(converter = RawUuidConverter.class)
    private UUID taxpayerId;

    @Id
    @Column(name = "TAX_PRODUCT_ID", nullable = false)
    @Convert(converter = RawUuidConverter.class)
    private UUID taxProductId;

    /**
     * فیلد transient برای bulk operations - لیست شناسه‌های محصولات مالیاتی
     * این فیلد در دیتابیس ذخیره نمی‌شود و فقط برای عملیات bulk استفاده می‌شود
     */
    @Transient
    @JsonProperty("taxProductIds")
    private List<UUID> taxProductIds;

    /**
     * کلاس برای Composite Primary Key
     */
    public static class Pk implements java.io.Serializable {
        public UUID taxpayerId;
        public UUID taxProductId;
        
        public Pk() {}
        
        public Pk(UUID taxpayerId, UUID taxProductId) { 
            this.taxpayerId = taxpayerId; 
            this.taxProductId = taxProductId; 
        }
        
        @Override 
        public int hashCode() { 
            return java.util.Objects.hash(taxpayerId, taxProductId); 
        }
        
        @Override 
        public boolean equals(Object o) {
            if (this == o) return true; 
            if (!(o instanceof Pk pk)) return false;
            return java.util.Objects.equals(taxpayerId, pk.taxpayerId) && 
                   java.util.Objects.equals(taxProductId, pk.taxProductId);
        }
    }
}

