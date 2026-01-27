package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @Size(min = 3, max = 50, message = "طول نام کاربری باید بین ۳ تا ۵۰ کاراکتر باشد.")
        String username,

        @Email(message = "ایمیل وارد شده نامعتبر است.")
        @Size(max = 200, message = "ایمیل نباید بیشتر از ۲۰۰ کاراکتر باشد.")
        String email,

        @Pattern(regexp = "^(ACTIVE|INACTIVE|PENDING|SUSPENDED)$", message = "وضعیت انتخاب شده معتبر نیست.")
        String status,

        Boolean active
) {
}

