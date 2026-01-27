package ir.tax.trustedtaxpayers.beans.dto.tax;

import ir.gov.tax.tpis.sdk.transfer.dto.PacketResponse;
import ir.tax.trustedtaxpayers.core.dto.BaseDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * پاسخ ارسال صورت‌حساب‌های مالیاتی
 * Tax Invoice Send Response
 */
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(callSuper = true)
public class TaxInvoiceResponse extends BaseDto {

    /**
     * لیست نتایج ارسال بسته‌ها
     * List of packet response results
     */
    private List<PacketResponse> results;

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
     * تعداد موفق
     * Success count
     */
    private int successCount;

    /**
     * تعداد ناموفق
     * Failure count
     */
    private int failureCount;
}

