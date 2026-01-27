package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetUserPasswordRequest(
        @NotBlank(message = "هش رمز عبور اجباری است.")
        @Size(min = 8, max = 200, message = "طول هش رمز عبور باید بین ۸ تا ۲۰۰ کاراکتر باشد.")
        String passwordHash
) {
}

