package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Entity واسط بین Taxpayer (مودی مالیاتی) و Buyer (خریدار)
 * 
 * <p>این Entity رابطه Many-to-Many بین مودی مالیاتی به عنوان فروشنده و مودی مالیاتی به عنوان خریدار را برقرار می‌کند.</p>
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Entity
@Table(name = "TAXPAYER_BUYER", 
       uniqueConstraints = @UniqueConstraint(name = "UK_TAXPAYER_BUYER", 
                                            columnNames = {"TAXPAYER_ID", "BUYER_ID"}))
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
@IdClass(TaxpayerBuyerEntity.Pk.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaxpayerBuyerEntity {
    
    @Id
    @Column(name = "TAXPAYER_ID", nullable = false)
    @Convert(converter = RawUuidConverter.class)
    private UUID taxpayerId;

    @Id
    @Column(name = "BUYER_ID", nullable = false)
    @Convert(converter = RawUuidConverter.class)
    private UUID buyerId;


    public static class Pk implements java.io.Serializable {
        public UUID taxpayerId;
        public UUID buyerId;
        
        public Pk() {}
        
        public Pk(UUID taxpayerId, UUID buyerId) { 
            this.taxpayerId = taxpayerId; 
            this.buyerId = buyerId; 
        }
        
        @Override 
        public int hashCode() { 
            return java.util.Objects.hash(taxpayerId, buyerId); 
        }
        
        @Override 
        public boolean equals(Object o) {
            if (this == o) return true; 
            if (!(o instanceof Pk pk)) return false;
            return java.util.Objects.equals(taxpayerId, pk.taxpayerId) && 
                   java.util.Objects.equals(buyerId, pk.buyerId);
        }
    }
}






