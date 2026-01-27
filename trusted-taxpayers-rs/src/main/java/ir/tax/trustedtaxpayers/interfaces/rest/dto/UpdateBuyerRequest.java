package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import jakarta.validation.constraints.Size;

public record UpdateBuyerRequest(
        @Size(max = 20) String buyerType,
        @Size(max = 20) String nationalId,
        @Size(max = 200) String name,
        Boolean active
) {}


