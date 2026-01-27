package ir.tax.trustedtaxpayers.interfaces.rest.dto;

public record ForgotPasswordResponse(
        boolean accepted,
        String resetToken
) {
}


