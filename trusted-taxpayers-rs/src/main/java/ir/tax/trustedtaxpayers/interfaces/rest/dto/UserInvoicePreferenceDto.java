package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import java.util.UUID;

public record UserInvoicePreferenceDto(
        UUID id,
        UUID userId,
        UUID taxpayerId,
        String defaultTemplateCode,
        String defaultTemplateTypeCode,
        Integer defaultPatternNumber,
        Integer defaultIns,
        String lastUsedTemplateCode,
        boolean firstLoginCompleted
) {
}

