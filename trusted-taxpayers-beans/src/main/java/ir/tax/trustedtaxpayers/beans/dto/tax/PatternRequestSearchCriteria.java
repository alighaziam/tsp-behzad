package ir.tax.trustedtaxpayers.beans.dto.tax;

import ir.tax.trustedtaxpayers.enums.PatternRequestStatus;
import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

/**
 * مجموعه فیلترهای جستجو برای درخواست‌های الگو.
 * <p>
 * Filter container used when searching pattern requests.
 */
@Value
@Builder
public class PatternRequestSearchCriteria {
    /**
     * وضعیت موردنظر برای فیلتر.
     * <p>
     * Desired request status.
     */
    PatternRequestStatus status;
    /**
     * تاریخ شروع بازه ایجاد.
     * <p>
     * Lower bound for creation date.
     */
    OffsetDateTime from;
    /**
     * تاریخ پایان بازه ایجاد.
     * <p>
     * Upper bound for creation date.
     */
    OffsetDateTime to;
    /**
     * فیلتر شماره الگو.
     * <p>
     * Pattern number filter.
     */
    Integer patternNumber;
    /**
     * شناسه درخواست‌کننده.
     * <p>
     * Requester identifier filter.
     */
    String requestedBy;
}
