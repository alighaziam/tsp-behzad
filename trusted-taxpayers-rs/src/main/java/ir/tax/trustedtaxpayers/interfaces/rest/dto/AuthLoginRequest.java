package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthLoginRequest(
        @NotBlank(message = "نام کاربری الزامی است.")
        @Size(min = 3, max = 100, message = "نام کاربری باید بین ۳ تا ۱۰۰ کاراکتر باشد.")
        String username,

        @NotBlank(message = "رمز عبور الزامی است.")
        @Size(min = 6, max = 200, message = "رمز عبور باید حداقل ۶ کاراکتر باشد.")
        String password
) {
}


