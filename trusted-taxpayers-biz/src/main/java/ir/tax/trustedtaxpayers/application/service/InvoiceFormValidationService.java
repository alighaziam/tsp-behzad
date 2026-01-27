package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.dto.FormFieldDto;
import ir.tax.trustedtaxpayers.application.dto.FormSchemaDto;
import ir.tax.trustedtaxpayers.beans.dto.tax.ValidationResultDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * سرویس اعتبارسنجی داده‌های فرم صورتحساب بر اساس الگو (Pattern)
 * از Schema تولیدی توسط InvoiceFormBuilderService استفاده می‌کند.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceFormValidationService {

    private final InvoiceFormBuilderService invoiceFormBuilderService;

    /**
     * دریافت Schema فرم بر اساس شماره الگو
     */
    public FormSchemaDto getFormSchema(Integer patternNumber) {
        return invoiceFormBuilderService.generateInvoiceFormSchema(patternNumber);
    }

    /**
     * اعتبارسنجی داده‌ها بر اساس الگو.
     * header: فیلدهای بدون پیشوند "body."
     * items: لیست اقلامی که قوانین فیلدهای با پیشوند "body." روی آن‌ها اعمال می‌شود.
     */
    @SuppressWarnings("unchecked")
    public ValidationResultDto validate(Integer patternNumber, Map<String, Object> header, List<Map<String, Object>> items) {
        FormSchemaDto schema = invoiceFormBuilderService.generateInvoiceFormSchema(patternNumber);
        List<FormFieldDto> fields = schema.getFields();

        ValidationResultDto result = ValidationResultDto.ok();

        if (header == null) header = new HashMap<>();
        if (items == null) items = new ArrayList<>();

        // جدا کردن فیلدهای header و body
        List<FormFieldDto> headerFields = new ArrayList<>();
        List<FormFieldDto> bodyFields = new ArrayList<>();
        for (FormFieldDto f : fields) {
            if (f.getKey() != null && f.getKey().startsWith("body.")) bodyFields.add(f);
            else headerFields.add(f);
        }

        // 1) اعتبارسنجی Header
        for (FormFieldDto f : headerFields) {
            validateFieldValue(result, f, f.getKey(), header.get(f.getKey()));
        }

        // 2) اعتبارسنجی Body (برای هر آیتم)
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            if (item == null) item = new HashMap<>();
            for (FormFieldDto f : bodyFields) {
                String plainKey = f.getKey().substring("body.".length());
                String reportKey = f.getKey() + "[" + i + "]"; // برای گزارش خطا
                validateFieldValue(result, f, reportKey, item.get(plainKey));
            }
        }

        // اگر آیتم وجود دارد و بعضی فیلدهای body اجباری هستند، نبود هیچ آیتمی خطاست
        boolean hasMandatoryBody = bodyFields.stream().anyMatch(x -> Boolean.TRUE.equals(x.getRequired()));
        if (hasMandatoryBody && (items.isEmpty())) {
            result.addError("body", "REQUIRED", "حداقل یک قلم کالا/خدمت باید وارد شود");
        }

        return result;
    }

    private void validateFieldValue(ValidationResultDto result, FormFieldDto field, String fieldKeyForReport, Object value) {
        boolean required = Boolean.TRUE.equals(field.getRequired());

        // REQUIRED
        if (required && isEmpty(value)) {
            result.addError(fieldKeyForReport, "REQUIRED", "پر کردن این فیلد الزامی است");
            return;
        }

        if (isEmpty(value)) {
            return; // خالی بودن فیلد اختیاری مشکلی ندارد
        }

        // TYPE CHECK (ساده)
        String type = field.getType() != null ? field.getType().toLowerCase() : "text";
        switch (type) {
            case "number":
                if (!(value instanceof Number) && !isParsableNumber(value)) {
                    result.addError(fieldKeyForReport, "TYPE_MISMATCH", "باید عددی باشد");
                    return;
                }
                break;
            case "datetime":
                if (!(value instanceof Date) && !(value instanceof OffsetDateTime)) {
                    if (!(value instanceof CharSequence) || !isParsableDateTime(value.toString())) {
                        result.addError(fieldKeyForReport, "TYPE_MISMATCH", "فرمت تاریخ/زمان نامعتبر است");
                        return;
                    }
                }
                break;
            default:
                // text/combobox/multiline/checkbox ... فعلاً ساده
                break;
        }

        // LENGTH
        if (value instanceof CharSequence) {
            int len = ((CharSequence) value).length();
            Integer min = field.getMinLength();
            Integer max = field.getMaxLength();
            if (min != null && len < min) {
                result.addError(fieldKeyForReport, "LENGTH", "حداقل طول مجاز " + min + " کاراکتر است");
                return;
            }
            if (max != null && len > max) {
                result.addError(fieldKeyForReport, "LENGTH", "حداکثر طول مجاز " + max + " کاراکتر است");
                return;
            }
        }

        // PATTERN
        if (field.getPattern() != null && value instanceof CharSequence) {
            try {
                if (!Pattern.compile(field.getPattern()).matcher(value.toString()).matches()) {
                    String msg = field.getValidationMessage() != null ? field.getValidationMessage() : "فرمت مقدار نامعتبر است";
                    result.addError(fieldKeyForReport, "PATTERN", msg);
                }
            } catch (Exception e) {
                log.debug("Invalid regex pattern for field {}: {}", field.getKey(), e.getMessage());
            }
        }
    }

    private boolean isEmpty(Object value) {
        if (value == null) return true;
        if (value instanceof CharSequence) return ((CharSequence) value).toString().trim().isEmpty();
        return false;
    }

    private boolean isParsableNumber(Object value) {
        try {
            new java.math.BigDecimal(value.toString());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isParsableDateTime(String s) {
        try {
            OffsetDateTime.parse(s);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }
}


