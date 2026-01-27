package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "USER_TAXPAYER")
@IdClass(UserTaxpayerEntity.Pk.class)
@Data
public class UserTaxpayerEntity {
    @Id
    @Column(name = "USER_ID")
    @Convert(converter = RawUuidConverter.class)
    private UUID userId;

    @Id
    @Column(name = "TAXPAYER_ID")
    @Convert(converter = RawUuidConverter.class)
    private UUID taxpayerId;

    @Column(name = "RELATION_ROLE", length = 30, nullable = false)
    private String relationRole = "OWNER";

    public static class Pk implements java.io.Serializable {
        public UUID userId;
        public UUID taxpayerId;
        public Pk() {}
        public Pk(UUID userId, UUID taxpayerId) { 
            this.userId = userId; 
            this.taxpayerId = taxpayerId; 
        }
        @Override 
        public int hashCode() { 
            return java.util.Objects.hash(userId, taxpayerId); 
        }
        @Override 
        public boolean equals(Object o) {
            if (this == o) return true; 
            if (!(o instanceof Pk pk)) return false;
            return java.util.Objects.equals(userId, pk.userId) && 
                   java.util.Objects.equals(taxpayerId, pk.taxpayerId);
        }
    }
}

