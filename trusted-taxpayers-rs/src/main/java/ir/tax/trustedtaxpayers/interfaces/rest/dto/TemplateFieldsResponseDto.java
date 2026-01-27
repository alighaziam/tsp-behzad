package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TemplateTypeEntity;

import java.util.List;

public record TemplateFieldsResponseDto(
        List<TemplateFieldWithRelationDto> fields,
        List<TemplateTypeEntity> templateTypes
) {
}


