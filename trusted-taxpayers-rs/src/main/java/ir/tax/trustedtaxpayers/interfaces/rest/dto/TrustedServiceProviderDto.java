package ir.tax.trustedtaxpayers.interfaces.rest.dto;

public record TrustedServiceProviderDto(
        String id,
        String code,
        String name,
        String contactName,
        String contactPhone,
        String contactEmail,
        String website,
        boolean active
) {
}



