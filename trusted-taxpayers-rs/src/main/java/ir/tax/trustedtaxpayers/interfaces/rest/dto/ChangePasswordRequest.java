package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @Size(min = 3, max = 100, message = "نام کاربری باید بین ۳ تا ۱۰۰ کاراکتر باشد.")
        String username,

        @NotBlank(message = "رمز عبور فعلی الزامی است.")
        @Size(min = 6, max = 200, message = "رمز عبور فعلی باید حداقل ۶ کاراکتر باشد.")
        String currentPassword,

        @NotBlank(message = "رمز عبور جدید الزامی است.")
        @Size(min = 8, max = 200, message = "رمز عبور جدید باید حداقل ۸ کاراکتر باشد.")
        String newPassword
) {
}


