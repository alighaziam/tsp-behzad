package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ForgotPasswordRequest(
        @NotBlank(message = "نام کاربری یا ایمیل الزامی است.")
        @Size(min = 3, max = 200, message = "شناسه کاربری باید بین ۳ تا ۲۰۰ کاراکتر باشد.")
        String identifier
) {
}


