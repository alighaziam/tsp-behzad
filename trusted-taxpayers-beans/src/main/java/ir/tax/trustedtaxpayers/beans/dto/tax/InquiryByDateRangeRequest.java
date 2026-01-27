package ir.tax.trustedtaxpayers.beans.dto.tax;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * درخواست استعلام بر اساس بازه زمانی
 * Inquiry by Date Range Request
 */
@Data
public class InquiryByDateRangeRequest {

    /**
     * تاریخ شروع (شمسی، فرمت: 14030101)
     * Start date (Persian, format: YYYYMMDD)
     */
    @NotBlank(message = "Start date cannot be blank")
    @Pattern(regexp = "\\d{8}", message = "Start date must be in format YYYYMMDD (e.g., 14030101)")
    private String startDate;

    /**
     * تاریخ پایان (شمسی، فرمت: 14030101)
     * End date (Persian, format: YYYYMMDD)
     */
    @NotBlank(message = "End date cannot be blank")
    @Pattern(regexp = "\\d{8}", message = "End date must be in format YYYYMMDD (e.g., 14030101)")
    private String endDate;
}

