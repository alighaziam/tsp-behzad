package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.core.entity.BaseEntity;
import ir.tax.trustedtaxpayers.core.util.RawUuidConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Entity
@Table(name = "AUDIT_LOGS")
@AttributeOverrides({
    @AttributeOverride(name = "createdOn", column = @Column(name = "CREATED_ON")),
    @AttributeOverride(name = "updatedOn", column = @Column(name = "UPDATED_ON"))
})
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogEntity extends BaseEntity {
    @Column(name = "ACTOR_USER_ID")
    @Convert(converter = RawUuidConverter.class)
    private UUID actorUserId;

    @Column(name = "ENTITY_NAME")
    private String entityName;

    @Column(name = "ENTITY_ID")
    private String entityId;

    @Column(name = "ACTION")
    private String action;

    @Column(name = "DETAILS", length = 250)
    private String details;
}



