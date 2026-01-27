package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreateTaxpayerContractRequest(
        @NotNull(message = "شناسه مودی الزامی است.")
        UUID taxpayerId,

        @NotNull(message = "شناسه شرکت معتمد الزامی است.")
        UUID providerId,

        @Size(max = 100, message = "شماره قرارداد نباید بیشتر از ۱۰۰ کاراکتر باشد.")
        String contractNumber,

        @Size(max = 200, message = "عنوان قرارداد نباید بیشتر از ۲۰۰ کاراکتر باشد.")
        String contractTitle,

        @NotNull(message = "تاریخ عقد قرارداد الزامی است.")
        LocalDate signedAt,

        LocalDate expiresAt,

        @NotBlank(message = "وضعیت قرارداد الزامی است.")
        @Size(max = 20, message = "وضعیت قرارداد نباید بیشتر از ۲۰ کاراکتر باشد.")
        String status,

        @Size(max = 1000, message = "توضیحات قرارداد نباید بیشتر از ۱۰۰۰ کاراکتر باشد.")
        String notes
) {
}


