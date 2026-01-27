package ir.tax.trustedtaxpayers.beans.dto.tax;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * درخواست استعلام بر اساس تاریخ
 * Inquiry by Date Request
 */
@Data
public class InquiryByDateRequest {

    /**
     * تاریخ شمسی (فرمت: 14030101)
     * Persian date (format: YYYYMMDD)
     */
    @NotBlank(message = "Date cannot be blank")
    @Pattern(regexp = "\\d{8}", message = "Date must be in format YYYYMMDD (e.g., 14030101)")
    private String date;
}

