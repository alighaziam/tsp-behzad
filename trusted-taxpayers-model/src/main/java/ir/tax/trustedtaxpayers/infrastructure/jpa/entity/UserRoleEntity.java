package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "USER_ROLE")
@EntityListeners(AuditingEntityListener.class)
@IdClass(UserRoleEntity.Pk.class)
@Data
public class UserRoleEntity {
    @Id
    @Column(name = "USER_ID")
    @Convert(converter = RawUuidConverter.class)
    private UUID userId;

    @Id
    @Column(name = "ROLE_ID")
    @Convert(converter = RawUuidConverter.class)
    private UUID roleId;

    @CreatedDate
    @Column(name = "ASSIGNED_AT", nullable = false, updatable = false)
    private OffsetDateTime assignedAt;

    @LastModifiedDate
    @Column(name = "UPDATED_AT")
    private OffsetDateTime updatedAt;

    @CreatedBy
    @Column(name = "CREATED_BY", length = 100, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;

    @Column(name = "IS_ACTIVE", nullable = false)
    private boolean isActive;

    public static class Pk implements java.io.Serializable {
        public UUID userId;
        public UUID roleId;
        public Pk() {}
        public Pk(UUID userId, UUID roleId) { this.userId = userId; this.roleId = roleId; }
        @Override public int hashCode() { return java.util.Objects.hash(userId, roleId); }
        @Override public boolean equals(Object o) {
            if (this == o) return true; if (!(o instanceof Pk pk)) return false;
            return java.util.Objects.equals(userId, pk.userId) && java.util.Objects.equals(roleId, pk.roleId);
        }
    }
}



