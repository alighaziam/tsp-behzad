package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Token ذخیره شده برای فرآیند بازیابی یا تغییر رمز عبور.
 */
@Entity
@Table(name = "PASSWORD_RESET_TOKENS")
@AttributeOverrides({
    @AttributeOverride(name = "createdOn", column = @Column(name = "CREATED_ON")),
    @AttributeOverride(name = "updatedOn", column = @Column(name = "UPDATED_ON")),
    @AttributeOverride(name = "createdBy", column = @Column(name = "CREATED_BY")),
    @AttributeOverride(name = "updatedBy", column = @Column(name = "UPDATED_BY"))
})
@Getter
@Setter
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PasswordResetTokenEntity extends BaseEntity {

    @Column(name = "USER_ID", nullable = false)
    @Convert(converter = RawUuidConverter.class)
    private UUID userId;

    @Column(name = "TOKEN", nullable = false, length = 128)
    private String token;

    @Column(name = "PURPOSE", length = 30)
    private String purpose;

    @Column(name = "EXPIRES_AT", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "USED_AT")
    private OffsetDateTime usedAt;
}


