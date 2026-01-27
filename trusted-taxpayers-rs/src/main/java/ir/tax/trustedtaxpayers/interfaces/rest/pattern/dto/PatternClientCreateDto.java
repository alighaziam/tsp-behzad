package ir.tax.trustedtaxpayers.interfaces.rest.pattern.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PatternClientCreateDto {

    @NotBlank
    @Schema(description = "نام شرکت طرف قرارداد", example = "شرکت همکاران")
    private String name;

    @Schema(description = "توضیح کوتاه درباره شرکت")
    private String description;

    @Email
    @Schema(description = "ایمیل تماس", example = "support@example.com")
    private String contactEmail;

    @Schema(description = "آدرس callback برای ارسال نتایج", example = "https://partner.example.com/callback")
    private String callbackUrl;
}
