package ir.tax.trustedtaxpayers.beans.dto.tax;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * بدنه ورودی برای ایجاد درخواست الگو شامل مشخصات پایه و متادیتا.
 * <p>
 * Request payload for creating a pattern request including base attributes and metadata.
 */
@Data
public class PatternRequestCreateDto {

    /**
     * شماره الگو (۱ تا ۱۶) که تعیین‌کننده ساختار فرم است.
     * <p>
     * Pattern number (1-16) determining the form structure.
     */
    @NotNull
    @Min(1)
    @Max(16)
    @Schema(description = "شماره الگو (1-16)", example = "2")
    private Integer patternNumber;

    /**
     * نام یا شناسه درخواست‌کننده در صورت استفاده بدون کلید.
     * <p>
     * Requester name or identifier when no client key is provided.
     */
    @Schema(description = "نام یا شناسه درخواست‌کننده", example = "thirdparty-service")
    private String requestedBy;

    /**
     * کلید یونیک صادرشده برای third-party.
     * <p>
     * Issued unique client key for the third-party.
     */
    @Schema(description = "کلید یونیک third-party ثبت‌شده", example = "a3c595f0-8d29-4c7c-b107-08a98e4fa5d3")
    private String clientKey;

    /**
     * متادیتای اختیاری جهت رهگیری و اتصال به سامانه‌های بیرونی.
     * <p>
     * Optional metadata for tracking or external system correlation.
     */
    @Schema(description = "متادیتای اختیاری برای رهگیری بهتر درخواست")
    private Map<String, Object> metadata;
}
