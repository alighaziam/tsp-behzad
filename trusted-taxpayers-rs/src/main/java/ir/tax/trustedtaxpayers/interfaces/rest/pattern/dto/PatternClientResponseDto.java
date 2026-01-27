package ir.tax.trustedtaxpayers.interfaces.rest.pattern.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO نمایش عمومی اطلاعات یک کلاینت الگو.
 * <p>
 * DTO representing the public view of a pattern client.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatternClientResponseDto {

    /**
     * شناسه یکتای رکورد کلاینت.
     * <p>
     * Unique identifier of the client record.
     */
    @Schema(description = "شناسه ردیفی")
    private UUID id;

    /**
     * کلید دسترسی صادرشده برای شرکت طرف قرارداد.
     * <p>
     * API key issued to the third-party client.
     */
    @Schema(description = "کلید یونیک برای استفاده third-party", example = "c3ebf6c0-a4ff-4d07-9a3b-1a0f2aa7d7f4")
    private String clientKey;

    /**
     * نام نمایشی شرکت.
     * <p>
     * Display name of the client organization.
     */
    @Schema(description = "نام شرکت")
    private String name;

    /**
     * ایمیل تماس شرکت.
     * <p>
     * Contact e-mail address.
     */
    @Schema(description = "ایمیل تماس")
    private String contactEmail;

    /**
     * آدرس callback ثبت‌شده توسط شرکت.
     * <p>
     * Callback endpoint configured by the client.
     */
    @Schema(description = "آدرس callback")
    private String callbackUrl;

    /**
     * توضیحات تکمیلی اختیاری درباره شرکت.
     * <p>
     * Optional descriptive text about the client.
     */
    @Schema(description = "توضیحات")
    private String description;

    /**
     * کلید مخفی برای یکپارچه‌سازی مبتنی بر HMAC.
     * <p>
     * Shared secret used for HMAC-based integrations.
     */
    @Schema(description = "کلید مخفی third-party")
    private String clientSecret;

    /**
     * زمان ایجاد رکورد.
     * <p>
     * Creation timestamp.
     */
    @Schema(description = "زمان ایجاد")
    private OffsetDateTime createdOn;

    /**
     * نشان می‌دهد کلاینت در حال حاضر فعال است یا خیر.
     * <p>
     * Indicates whether the client is currently active.
     */
    @Schema(description = "آیا فعال است؟", example = "true")
    private boolean active;
}
