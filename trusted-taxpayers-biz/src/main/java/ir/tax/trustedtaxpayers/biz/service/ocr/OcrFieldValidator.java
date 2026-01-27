package ir.tax.trustedtaxpayers.biz.service.ocr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * سرویس اعتبارسنجی فیلدهای استخراج شده از OCR
 * این سرویس فیلدهای استخراج شده را اعتبارسنجی می‌کند و خطاهای احتمالی را شناسایی می‌کند
 */
@Service
@Slf4j
public class OcrFieldValidator {
    
    // الگوهای اعتبارسنجی
    private static final Pattern TAX_ID_PATTERN = Pattern.compile("^\\d{10,11}$");
    private static final Pattern INVOICE_NUMBER_PATTERN = Pattern.compile("^[\\d\\-]{4,20}$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}$");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d+(\\.\\d+)?$");
    
    // فرمت‌های تاریخ قابل قبول
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/M/d"),
        DateTimeFormatter.ofPattern("yyyy-M-d")
    };
    
    /**
     * اعتبارسنجی همه فیلدهای استخراج شده
     */
    public ValidationResult validate(Map<String, Object> extractedFields) {
        ValidationResult result = new ValidationResult();
        
        if (extractedFields == null || extractedFields.isEmpty()) {
            result.addError("general", "هیچ فیلدی استخراج نشده است");
            return result;
        }
        
        // اعتبارسنجی شماره اقتصادی فروشنده
        String sellerTaxId = getString(extractedFields, "sellerTaxId");
        if (sellerTaxId != null && !sellerTaxId.isBlank()) {
            if (!isValidTaxId(sellerTaxId)) {
                result.addError("sellerTaxId", String.format("شماره اقتصادی فروشنده نامعتبر است: %s (باید 10 یا 11 رقم باشد)", sellerTaxId));
                result.addWarning("sellerTaxId", "شماره اقتصادی فروشنده ممکن است به اشتباه استخراج شده باشد");
            }
        }
        
        // اعتبارسنجی شماره اقتصادی خریدار
        String buyerTaxId = getString(extractedFields, "buyerTaxId");
        if (buyerTaxId != null && !buyerTaxId.isBlank()) {
            if (!isValidTaxId(buyerTaxId)) {
                result.addError("buyerTaxId", String.format("شماره اقتصادی خریدار نامعتبر است: %s (باید 10 یا 11 رقم باشد)", buyerTaxId));
                result.addWarning("buyerTaxId", "شماره اقتصادی خریدار ممکن است به اشتباه استخراج شده باشد");
            }
        }
        
        // اعتبارسنجی شماره صورتحساب
        String invoiceNumber = getString(extractedFields, "invoiceNumber");
        if (invoiceNumber != null && !invoiceNumber.isBlank()) {
            if (!isValidInvoiceNumber(invoiceNumber)) {
                result.addWarning("invoiceNumber", String.format("شماره صورتحساب ممکن است نامعتبر باشد: %s", invoiceNumber));
            }
        }
        
        // اعتبارسنجی تاریخ
        String issueDate = getString(extractedFields, "issueDate");
        if (issueDate != null && !issueDate.isBlank()) {
            DateValidationResult dateValidation = validateDate(issueDate);
            if (!dateValidation.isValid()) {
                result.addError("issueDate", dateValidation.getErrorMessage());
            } else if (dateValidation.hasWarning()) {
                result.addWarning("issueDate", dateValidation.getWarningMessage());
            }
        }
        
        // اعتبارسنجی تاریخ اعلامیه فروش
        String asd = getString(extractedFields, "asd");
        if (asd != null && !asd.isBlank()) {
            DateValidationResult dateValidation = validateDate(asd);
            if (!dateValidation.isValid()) {
                result.addWarning("asd", dateValidation.getErrorMessage());
            } else if (dateValidation.hasWarning()) {
                result.addWarning("asd", dateValidation.getWarningMessage());
            }
        }
        
        // اعتبارسنجی مبالغ
        validateAmount(extractedFields, "totalAmount", "مبلغ کل", result);
        validateAmount(extractedFields, "tprdis", "جمع مبلغ قبل از تخفیف", result);
        validateAmount(extractedFields, "tdis", "جمع تخفیف", result);
        validateAmount(extractedFields, "tadis", "جمع مبلغ بعد از تخفیف", result);
        validateAmount(extractedFields, "tvam", "جمع مالیات بر ارزش افزوده", result);
        validateAmount(extractedFields, "cap", "مبلغ نقدی", result);
        validateAmount(extractedFields, "insp", "مبلغ اقساط", result);
        validateAmount(extractedFields, "todam", "جمع سایر عوارض", result);
        validateAmount(extractedFields, "tax17", "مالیات ماده 17", result);
        validateAmount(extractedFields, "tvop", "جمع مالیات پرداختی", result);
        
        // اعتبارسنجی items
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) extractedFields.get("items");
        if (items != null && !items.isEmpty()) {
            validateItems(items, result);
        }
        
        return result;
    }
    
    /**
     * اعتبارسنجی شماره اقتصادی
     */
    private boolean isValidTaxId(String taxId) {
        if (taxId == null || taxId.isBlank()) {
            return false;
        }
        // حذف فاصله و کاراکترهای غیرعددی
        String cleaned = taxId.replaceAll("[^\\d]", "");
        return TAX_ID_PATTERN.matcher(cleaned).matches();
    }
    
    /**
     * اعتبارسنجی شماره صورتحساب
     */
    private boolean isValidInvoiceNumber(String invoiceNumber) {
        if (invoiceNumber == null || invoiceNumber.isBlank()) {
            return false;
        }
        // شماره صورتحساب می‌تواند شامل اعداد و خط تیره باشد
        return INVOICE_NUMBER_PATTERN.matcher(invoiceNumber).matches();
    }
    
    /**
     * اعتبارسنجی تاریخ
     */
    private DateValidationResult validateDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return new DateValidationResult(false, "تاریخ خالی است", null);
        }
        
        // بررسی فرمت
        if (!DATE_PATTERN.matcher(dateStr).matches()) {
            return new DateValidationResult(false, String.format("فرمت تاریخ نامعتبر است: %s (فرمت صحیح: YYYY/MM/DD)", dateStr), null);
        }
        
        // تلاش برای parse کردن تاریخ
        LocalDate date = null;
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                date = LocalDate.parse(dateStr, formatter);
                break;
            } catch (DateTimeParseException e) {
                // Continue to next formatter
            }
        }
        
        if (date == null) {
            return new DateValidationResult(false, String.format("نمی‌توان تاریخ را parse کرد: %s", dateStr), null);
        }
        
        // بررسی اینکه تاریخ در آینده نباشد (با tolerance 1 روز برای timezone)
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);
        if (date.isAfter(tomorrow)) {
            return new DateValidationResult(true, null, String.format("تاریخ در آینده است: %s (امروز: %s)", dateStr, today));
        }
        
        // بررسی اینکه تاریخ خیلی قدیمی نباشد (مثلاً قبل از 1300 شمسی = 1921 میلادی)
        LocalDate minDate = LocalDate.of(1921, 1, 1);
        if (date.isBefore(minDate)) {
            return new DateValidationResult(false, String.format("تاریخ خیلی قدیمی است: %s", dateStr), null);
        }
        
        return new DateValidationResult(true, null, null);
    }
    
    /**
     * اعتبارسنجی مبلغ
     */
    private void validateAmount(Map<String, Object> fields, String fieldName, String fieldLabel, ValidationResult result) {
        String amountStr = getString(fields, fieldName);
        if (amountStr != null && !amountStr.isBlank()) {
            // حذف comma و کاراکترهای غیرعددی
            String cleaned = amountStr.replaceAll("[^\\d.]", "");
            
            if (cleaned.isBlank()) {
                result.addWarning(fieldName, String.format("%s نامعتبر است: %s", fieldLabel, amountStr));
                return;
            }
            
            if (!AMOUNT_PATTERN.matcher(cleaned).matches()) {
                result.addWarning(fieldName, String.format("%s فرمت نامعتبر دارد: %s", fieldLabel, amountStr));
                return;
            }
            
            try {
                double amount = Double.parseDouble(cleaned);
                if (amount < 0) {
                    result.addError(fieldName, String.format("%s نمی‌تواند منفی باشد: %s", fieldLabel, amountStr));
                } else if (amount == 0 && !fieldName.equals("tdis")) {
                    // تخفیف می‌تواند صفر باشد، اما سایر مبالغ معمولاً نباید صفر باشند
                    result.addWarning(fieldName, String.format("%s صفر است: %s", fieldLabel, amountStr));
                }
            } catch (NumberFormatException e) {
                result.addWarning(fieldName, String.format("%s نمی‌تواند به عدد تبدیل شود: %s", fieldLabel, amountStr));
            }
        }
    }
    
    /**
     * اعتبارسنجی items
     */
    private void validateItems(java.util.List<Map<String, Object>> items, ValidationResult result) {
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            String itemPrefix = String.format("items[%d]", i);
            
            // اعتبارسنجی تعداد
            String am = getString(item, "am");
            if (am != null && !am.isBlank()) {
                try {
                    double quantity = Double.parseDouble(am.replaceAll("[^\\d.]", ""));
                    if (quantity <= 0) {
                        result.addError(itemPrefix + ".am", String.format("تعداد item %d باید مثبت باشد: %s", i + 1, am));
                    }
                } catch (NumberFormatException e) {
                    result.addWarning(itemPrefix + ".am", String.format("تعداد item %d نامعتبر است: %s", i + 1, am));
                }
            }
            
            // اعتبارسنجی قیمت
            String fee = getString(item, "fee");
            if (fee != null && !fee.isBlank()) {
                try {
                    double price = Double.parseDouble(fee.replaceAll("[^\\d.]", ""));
                    if (price < 0) {
                        result.addError(itemPrefix + ".fee", String.format("قیمت item %d نمی‌تواند منفی باشد: %s", i + 1, fee));
                    }
                } catch (NumberFormatException e) {
                    result.addWarning(itemPrefix + ".fee", String.format("قیمت item %d نامعتبر است: %s", i + 1, fee));
                }
            }
        }
    }
    
    /**
     * Helper method برای استخراج String از Map
     */
    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            return (String) value;
        }
        return value.toString();
    }
    
    /**
     * نتیجه اعتبارسنجی
     */
    public static class ValidationResult {
        private final Map<String, String> errors = new HashMap<>();
        private final Map<String, String> warnings = new HashMap<>();
        
        public void addError(String field, String message) {
            errors.put(field, message);
        }
        
        public void addWarning(String field, String message) {
            warnings.put(field, message);
        }
        
        public boolean isValid() {
            return errors.isEmpty();
        }
        
        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
        
        public Map<String, String> getErrors() {
            return new HashMap<>(errors);
        }
        
        public Map<String, String> getWarnings() {
            return new HashMap<>(warnings);
        }
        
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            if (!errors.isEmpty()) {
                sb.append(String.format("خطاها (%d): ", errors.size()));
                sb.append(String.join(", ", errors.values()));
            }
            if (!warnings.isEmpty()) {
                if (sb.length() > 0) sb.append(" | ");
                sb.append(String.format("هشدارها (%d): ", warnings.size()));
                sb.append(String.join(", ", warnings.values()));
            }
            return sb.toString();
        }
    }
    
    /**
     * نتیجه اعتبارسنجی تاریخ
     */
    private static class DateValidationResult {
        private final boolean valid;
        private final String errorMessage;
        private final String warningMessage;
        
        public DateValidationResult(boolean valid, String errorMessage, String warningMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
            this.warningMessage = warningMessage;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public boolean hasWarning() {
            return warningMessage != null && !warningMessage.isBlank();
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public String getWarningMessage() {
            return warningMessage;
        }
    }
}

