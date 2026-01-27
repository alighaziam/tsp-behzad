package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "TRUSTED_SERVICE_PROVIDERS")
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
public class TrustedServiceProviderEntity extends BaseEntity {

    @Column(name = "CODE", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "NAME", nullable = false, length = 200)
    private String name;

    @Column(name = "CONTACT_NAME", length = 150)
    private String contactName;

    @Column(name = "CONTACT_PHONE", length = 50)
    private String contactPhone;

    @Column(name = "CONTACT_EMAIL", length = 150)
    private String contactEmail;

    @Column(name = "WEBSITE", length = 200)
    private String website;
}


