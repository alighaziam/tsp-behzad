package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import jakarta.validation.constraints.Size;

public record ApproveContractRequest(
        @Size(max = 20, message = "وضعیت قرارداد نباید بیشتر از ۲۰ کاراکتر باشد.")
        String status,

        @Size(max = 500, message = "توضیحات تایید نباید بیشتر از ۵۰۰ کاراکتر باشد.")
        String approvalNote
) {
}

