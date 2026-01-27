package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import ir.tax.trustedtaxpayers.enums.ComponentType;
import ir.tax.trustedtaxpayers.enums.FieldRequirementType;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateFieldEntity;

import java.util.UUID;

public record TemplateFieldWithRelationDto(
        UUID id,
        String tspCode,
        String taxCode,
        String title,
        FieldRequirementType requirementType,
        Boolean enable,
        String regex,
        String errorMessage,
        String invoiceStep,
        String componentType
) {
    public static TemplateFieldWithRelationDto from(TemplateFieldEntity field, FieldRequirementType requirementType, Boolean enable) {
        return new TemplateFieldWithRelationDto(
                field.getId(),
                field.getTspCode(),
                field.getTaxCode(),
                field.getTitle(),
                requirementType,
                enable,
                ComponentType.TEXT_FIELD.equals(field.getComponentType()) ? field.getRegexPattern() : null,
                ComponentType.TEXT_FIELD.equals(field.getComponentType()) ? field.getErrorMessageFa(): field.getSecondErrorMessage(),
                field.getInvoiceStep().name(),
                field.getComponentType().name()
        );
    }
}

