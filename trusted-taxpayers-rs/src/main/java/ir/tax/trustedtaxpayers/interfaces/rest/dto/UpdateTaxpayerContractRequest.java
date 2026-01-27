package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateTaxpayerContractRequest(
        @Size(max = 100, message = "شماره قرارداد نباید بیشتر از ۱۰۰ کاراکتر باشد.")
        String contractNumber,

        @Size(max = 200, message = "عنوان قرارداد نباید بیشتر از ۲۰۰ کاراکتر باشد.")
        String contractTitle,

        LocalDate signedAt,

        LocalDate expiresAt,

        @Size(max = 20, message = "وضعیت قرارداد نباید بیشتر از ۲۰ کاراکتر باشد.")
        String status,

        @Size(max = 1000, message = "توضیحات قرارداد نباید بیشتر از ۱۰۰۰ کاراکتر باشد.")
        String notes,

        Boolean active
) {
}

