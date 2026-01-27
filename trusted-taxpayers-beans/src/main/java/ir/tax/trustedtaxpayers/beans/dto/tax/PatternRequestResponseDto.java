package ir.tax.trustedtaxpayers.beans.dto.tax;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * پاسخ اولیه برای ثبت درخواست الگو شامل تعاریف فرم و شناسه‌های مرتبط.
 * <p>
 * Response returned after creating a pattern request containing form schema and identifiers.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatternRequestResponseDto {

    /**
     * شناسه درخواست برای استفاده در فراخوانی‌های بعدی.
     * <p>
     * Unique request identifier for subsequent calls.
     */
    @Schema(description = "شناسه درخواست برای استفاده در سرویس‌های بعدی")
    private UUID requestId;

    /**
     * شماره الگوی تشخیص داده شده.
     * <p>
     * Detected pattern number.
     */
    @Schema(description = "شماره الگوی تشخیص داده شده")
    private Integer patternNumber;

    /**
     * عنوان قابل‌نمایش الگو.
     * <p>
     * Human-readable pattern title.
     */
    @Schema(description = "عنوان الگو")
    private String title;

    /**
     * توضیحات تکمیلی الگو.
     * <p>
     * Pattern description.
     */
    @Schema(description = "شرح الگو")
    private String description;

    /**
     * شناسه یا نام درخواست‌کننده (third-party).
     * <p>
     * Identifier or name of the requester.
     */
    @Schema(description = "شناسه یا نام third-party درخواست کننده")
    private String requestedBy;

    /**
     * کلید یونیک شرکت طرف قرارداد.
     * <p>
     * Unique client key.
     */
    @Schema(description = "کلید یونیک third-party")
    private String clientKey;

    /**
     * نام شرکت طرف قرارداد.
     * <p>
     * Client display name.
     */
    @Schema(description = "نام third-party")
    private String clientName;

    /**
     * شناسه اختصاصی تولیدشده برای نمایش به کلاینت.
     * <p>
     * External request identifier for third-party usage.
     */
    @Schema(description = "شناسه یونیک اختصاص‌داده شده برای third-party")
    private String externalRequestId;

    /**
     * وضعیت معوق بودن ارسال.
     * <p>
     * Indicates whether submission is deferred.
     */
    @Schema(description = "آیا ارسال معوق است؟", example = "false")
    private boolean deferredSubmission;

    /**
     * تایید شدن درخواست معوق.
     * <p>
     * Indicates whether deferred submission is approved.
     */
    @Schema(description = "آیا درخواست معوق تایید شده است؟", example = "false")
    private boolean deferredApproved;

    /**
     * وضعیت جاری درخواست.
     * <p>
     * Current status string.
     */
    @Schema(description = "وضعیت فعلی درخواست")
    private String status;

    /**
     * تعاریف فیلدهای بخش سربرگ.
     * <p>
     * Field definitions for the header section.
     */
    @Schema(description = "تعاریف فیلدهای سربرگ")
    private List<PatternFieldDto> headerFields;

    /**
     * تعاریف فیلدهای بخش اقلام.
     * <p>
     * Field definitions for the item section.
     */
    @Schema(description = "تعاریف فیلدهای اقلام")
    private List<PatternFieldDto> itemFields;

    /**
     * متادیتای ذخیره‌شده برای درخواست.
     * <p>
     * Persisted metadata map.
     */
    @Schema(description = "متادیتای اختیاری ذخیره‌شده")
    private Map<String, Object> metadata;
}
