package ir.tax.trustedtaxpayers.beans.dto.tax;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * درخواست استعلام با شماره مرجع
 * Inquiry by Reference Number Request
 */
@Data
public class InquiryByReferenceRequest {

    /**
     * لیست شماره‌های مرجع
     * List of reference numbers
     */
    @NotNull(message = "Reference numbers list cannot be null")
    @NotEmpty(message = "Reference numbers list cannot be empty")
    private List<String> referenceNumbers;
}

