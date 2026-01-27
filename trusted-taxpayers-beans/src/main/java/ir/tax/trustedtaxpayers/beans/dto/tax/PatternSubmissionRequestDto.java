package ir.tax.trustedtaxpayers.beans.dto.tax;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * بدنه ورودی برای ارسال داده‌های تکمیل‌شده یک درخواست الگو.
 * <p>
 * Request payload carrying completed pattern data for submission.
 */
@Data
public class PatternSubmissionRequestDto {

    /**
     * مقادیر سربرگ مطابق شمای ارائه‌شده.
     * <p>
     * Header values aligned with the provided schema.
     */
    @NotNull
    @Schema(description = "مقادیر سربرگ صورتحساب مطابق با فیلدهای ارائه‌شده")
    private Map<String, Object> header;

    /**
     * مجموعه اقلام صورتحساب.
     * <p>
     * Collection of invoice items.
     */
    @NotEmpty
    @Schema(description = "لیست اقلام صورتحساب. برای الگوهایی که اقلام ندارند می‌توان یک لیست با یک آیتم خالی ارسال کرد")
    private List<Map<String, Object>> items;

    /**
     * متادیتای اضافی اختیاری.
     * <p>
     * Optional additional metadata.
     */
    @Schema(description = "متادیتای اضافی اختیاری")
    private Map<String, Object> metadata;

    /**
     * در صورت true بودن، ارسال به‌طور معوق انجام می‌شود.
     * <p>
     * Flag indicating deferred submission when set to true.
     */
    @Schema(description = "اگر true باشد ارسال به سامانه مودیان در لحظه انجام نمی‌شود و پس از تایید و اجرای جاب انجام خواهد شد", example = "false")
    private boolean deferred;
}
