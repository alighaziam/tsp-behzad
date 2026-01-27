package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import ir.tax.trustedtaxpayers.enums.FieldRequirementType;
import ir.tax.trustedtaxpayers.enums.FieldSection;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "TEMPLATE_TEMPLATE_FIELD")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(TemplateTemplateFieldId.class)
public class TemplateTemplateFieldEntity {

    @Id
    @Column(name = "TEMPLATE_ID", columnDefinition = "RAW(16)")
    private UUID templateId;

    @Id
    @Column(name = "TEMPLATE_FIELD_ID", columnDefinition = "RAW(16)")
    private UUID templateFieldId;

    @Enumerated(EnumType.STRING)
    @Column(name = "FIELD_REQUIREMENT_TYPE", length = 30, nullable = false)
    private FieldRequirementType fieldRequirementType;

    @Enumerated(EnumType.STRING)
    @Column(name = "FIELD_SECTION", length = 20, nullable = false)
    private FieldSection fieldSection;

    @Column(name = "ENABLE", columnDefinition = "NUMBER(1) DEFAULT 1", nullable = false)
    private Boolean enable = true;
}

