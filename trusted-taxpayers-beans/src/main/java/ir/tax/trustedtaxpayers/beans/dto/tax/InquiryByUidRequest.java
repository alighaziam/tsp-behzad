package ir.tax.trustedtaxpayers.beans.dto.tax;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * درخواست استعلام با UID
 * Inquiry by UID Request
 */
@Data
public class InquiryByUidRequest {

    /**
     * لیست UID و شناسه مالیاتی
     * List of UID and Fiscal IDs
     */
    @Valid
    @NotNull(message = "UID list cannot be null")
    @NotEmpty(message = "UID list cannot be empty")
    private List<UidAndFiscalId> uidAndFiscalIds;
}

