package ir.tax.trustedtaxpayers.interfaces.rest.pattern.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class PatternClientUpdateDto {

    @Schema(description = "نام شرکت", example = "شرکت همکاران")
    private String name;

    @Schema(description = "توضیح کوتاه")
    private String description;

    @Email
    @Schema(description = "ایمیل تماس")
    private String contactEmail;

    @Schema(description = "آدرس callback")
    private String callbackUrl;
}
