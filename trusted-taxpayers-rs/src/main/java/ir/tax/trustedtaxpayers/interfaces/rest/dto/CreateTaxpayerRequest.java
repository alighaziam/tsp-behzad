package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "درخواست ایجاد مودی مالیاتی")
public record CreateTaxpayerRequest(
        @Schema(description = "شماره مودی مالیاتی (Tax ID)", required = true, maxLength = 50)
        @NotBlank @Size(max = 50) String taxId,

        @Schema(description = "نام مودی", required = true, maxLength = 200)
        @NotBlank @Size(max = 200) String name,

        @Schema(description = "نوع مودی (عنوان از BaseInfo)", maxLength = 200)
        @Size(max = 200) String type,

        @Schema(description = "کشور", maxLength = 100)
        @Size(max = 100) String country,

        @Schema(description = "استان", maxLength = 100)
        @Size(max = 100) String province,

        @Schema(description = "شهر", maxLength = 100)
        @Size(max = 100) String city,

        @Schema(description = "آدرس کامل")
        String address,

        @Schema(description = "شماره ثبت مالیات بر ارزش افزوده", maxLength = 50)
        @Size(max = 50) String vatRegisterNo,

        @Schema(description = "شناسه ملی / کد ملی", maxLength = 20)
        @Size(max = 20) String nationalId,

        @Schema(description = "کد پستی", maxLength = 10)
        @Size(max = 10) String postalCode,

        @Schema(description = "شماره گذرنامه", maxLength = 20)
        @Size(max = 20) String passportNo
) {}

