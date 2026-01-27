package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "توکن بازیابی الزامی است.")
        String token,

        @NotBlank(message = "رمز عبور جدید الزامی است.")
        @Size(min = 8, max = 200, message = "رمز عبور جدید باید حداقل ۸ کاراکتر باشد.")
        String newPassword
) {
}


