package ir.tax.trustedtaxpayers.beans.dto.tax;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * پاسخ استعلام صورت‌حساب
 * Invoice Inquiry Response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InquiryResponse {

    /**
     * نتایج استعلام
     * Inquiry results
     */
    private List<InquiryResultModel> results;

    /**
     * پیام عمومی
     * General message
     */
    private String message;

    /**
     * زمان پردازش
     * Processing time
     */
    private LocalDateTime timestamp;

    /**
     * تعداد نتایج
     * Results count
     */
    private int count;
}

