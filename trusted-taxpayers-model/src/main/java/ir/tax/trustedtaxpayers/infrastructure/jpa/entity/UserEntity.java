package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entity کاربر سیستم
 * 
 * <p>این Entity شامل اطلاعات کاربران سیستم است.</p>
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Entity
@Table(name = "USERS")
@AttributeOverrides({
    @AttributeOverride(name = "createdOn", column = @Column(name = "CREATED_AT")),
    @AttributeOverride(name = "updatedOn", column = @Column(name = "UPDATED_AT"))
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity extends BaseEntity {

    @Column(name = "USERNAME", nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "PASSWORD_HASH", nullable = false, length = 4000)
    private String passwordHash;

    @Column(name = "EMAIL", length = 200)
    private String email;

    @Column(name = "STATUS", length = 20)
    private String status;
}



