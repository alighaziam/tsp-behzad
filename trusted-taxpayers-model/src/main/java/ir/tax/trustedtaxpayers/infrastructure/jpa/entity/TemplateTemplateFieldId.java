package ir.tax.trustedtaxpayers.infrastructure.jpa.entity;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class TemplateTemplateFieldId implements Serializable {
    private UUID templateId;
    private UUID templateFieldId;
}

