package ir.tax.trustedtaxpayers.beans.dto.tax;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * DTO نمایش‌دهنده مشخصات یک فیلد الگو برای سربرگ یا اقلام.
 * <p>
 * DTO describing a pattern field definition for header or item sections.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatternFieldDto {

    /**
     * بخشی که فیلد به آن تعلق دارد (HEADER/ITEM).
     * <p>
     * Section the field belongs to (HEADER/ITEM).
     */
    @Schema(description = "بخش فیلد", example = "HEADER", allowableValues = {"HEADER", "ITEM"})
    private String section;

    /**
     * کلید فنی فیلد برای ارسال.
     * <p>
     * Technical key used in payloads.
     */
    @Schema(description = "کلید فیلد برای ارسال", example = "inno")
    private String key;

    /**
     * برچسب نمایشی فیلد.
     * <p>
     * Human-readable field label.
     */
    @Schema(description = "عنوان فیلد", example = "شماره صورتحساب")
    private String label;

    /**
     * نوع ورودی پیشنهادی (مثلاً text, number).
     * <p>
     * Suggested input type (e.g., text, number).
     */
    @Schema(description = "نوع ورودی پیشنهادی", example = "text")
    private String type;

    /**
     * نشانگر اجباری بودن فیلد.
     * <p>
     * Whether the field is required.
     */
    @Schema(description = "آیا فیلد اجباری است؟")
    private Boolean required;

    /**
     * طول مجاز حداکثری.
     * <p>
     * Maximum allowed length.
     */
    @Schema(description = "حداکثر طول مجاز")
    private Integer maxLength;

    /**
     * طول مجاز حداقلی.
     * <p>
     * Minimum allowed length.
     */
    @Schema(description = "حداقل طول مجاز")
    private Integer minLength;

    /**
     * مقدار پیش‌فرض در صورت وجود.
     * <p>
     * Default value if provided.
     */
    @Schema(description = "مقدار پیش‌فرض پیشنهادی")
    private Object defaultValue;

    /**
     * گزینه‌های انتخابی برای فیلدهای لیستی.
     * <p>
     * Options for list-based fields.
     */
    @Schema(description = "گزینه‌های انتخابی در صورت وجود")
    private List<Map<String, Object>> options;
}
