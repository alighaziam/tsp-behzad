package ir.tax.trustedtaxpayers.biz.service.ocr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * سرویس محاسبه Confidence Score برای فیلدهای استخراج شده از OCR
 * این سرویس برای هر فیلد یک امتیاز اعتماد (0-100) محاسبه می‌کند
 */
@Service
@Slf4j
public class OcrConfidenceCalculator {
    
    // الگوهای اعتبارسنجی برای محاسبه confidence
    private static final Pattern TAX_ID_PATTERN = Pattern.compile("^\\d{10,11}$");
    private static final Pattern INVOICE_NUMBER_PATTERN = Pattern.compile("^[\\d\\-]{4,20}$");
    private static final Pattern DATE_PATTERN = Pattern.compile("^\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}$");
    private static final Pattern AMOUNT_PATTERN = Pattern.compile("^\\d+(\\.\\d+)?$");
    private static final Pattern NAME_PATTERN = Pattern.compile("^[\\p{L}\\s\\-\\.]{3,100}$");
    
    /**
     * محاسبه confidence score برای همه فیلدهای استخراج شده
     */
    public Map<String, Integer> calculateConfidence(Map<String, Object> extractedFields) {
        Map<String, Integer> confidenceScores = new HashMap<>();
        
        if (extractedFields == null || extractedFields.isEmpty()) {
            return confidenceScores;
        }
        
        // محاسبه confidence برای هر فیلد
        calculateFieldConfidence(extractedFields, "sellerTaxId", confidenceScores, this::calculateTaxIdConfidence);
        calculateFieldConfidence(extractedFields, "buyerTaxId", confidenceScores, this::calculateTaxIdConfidence);
        calculateFieldConfidence(extractedFields, "invoiceNumber", confidenceScores, this::calculateInvoiceNumberConfidence);
        calculateFieldConfidence(extractedFields, "issueDate", confidenceScores, this::calculateDateConfidence);
        calculateFieldConfidence(extractedFields, "asd", confidenceScores, this::calculateDateConfidence);
        calculateFieldConfidence(extractedFields, "sellerName", confidenceScores, this::calculateNameConfidence);
        calculateFieldConfidence(extractedFields, "buyerName", confidenceScores, this::calculateNameConfidence);
        calculateFieldConfidence(extractedFields, "totalAmount", confidenceScores, this::calculateAmountConfidence);
        calculateFieldConfidence(extractedFields, "tprdis", confidenceScores, this::calculateAmountConfidence);
        calculateFieldConfidence(extractedFields, "tdis", confidenceScores, this::calculateAmountConfidence);
        calculateFieldConfidence(extractedFields, "tadis", confidenceScores, this::calculateAmountConfidence);
        calculateFieldConfidence(extractedFields, "tvam", confidenceScores, this::calculateAmountConfidence);
        calculateFieldConfidence(extractedFields, "cap", confidenceScores, this::calculateAmountConfidence);
        calculateFieldConfidence(extractedFields, "insp", confidenceScores, this::calculateAmountConfidence);
        calculateFieldConfidence(extractedFields, "todam", confidenceScores, this::calculateAmountConfidence);
        calculateFieldConfidence(extractedFields, "tax17", confidenceScores, this::calculateAmountConfidence);
        calculateFieldConfidence(extractedFields, "tvop", confidenceScores, this::calculateAmountConfidence);
        
        // محاسبه confidence برای items
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> items = (java.util.List<Map<String, Object>>) extractedFields.get("items");
        if (items != null && !items.isEmpty()) {
            for (int i = 0; i < items.size(); i++) {
                Map<String, Object> item = items.get(i);
                String prefix = "items[" + i + "]";
                calculateFieldConfidence(item, "sstid", confidenceScores, 
                        v -> calculateItemCodeConfidence(v), prefix);
                calculateFieldConfidence(item, "sstt", confidenceScores, 
                        v -> calculateNameConfidence(v), prefix);
                calculateFieldConfidence(item, "am", confidenceScores, 
                        v -> calculateAmountConfidence(v), prefix);
                calculateFieldConfidence(item, "fee", confidenceScores, 
                        v -> calculateAmountConfidence(v), prefix);
            }
        }
        
        return confidenceScores;
    }
    
    /**
     * Helper method برای محاسبه confidence یک فیلد
     */
    private void calculateFieldConfidence(Map<String, Object> fields, String fieldName, 
                                         Map<String, Integer> confidenceScores, 
                                         java.util.function.Function<String, Integer> calculator) {
        calculateFieldConfidence(fields, fieldName, confidenceScores, calculator, "");
    }
    
    private void calculateFieldConfidence(Map<String, Object> fields, String fieldName, 
                                         Map<String, Integer> confidenceScores, 
                                         java.util.function.Function<String, Integer> calculator,
                                         String prefix) {
        Object value = fields.get(fieldName);
        if (value == null) {
            return;
        }
        
        String valueStr = value.toString();
        if (valueStr.isBlank()) {
            return;
        }
        
        String key = prefix.isEmpty() ? fieldName : prefix + "." + fieldName;
        int confidence = calculator.apply(valueStr);
        confidenceScores.put(key, confidence);
    }
    
    /**
     * محاسبه confidence برای شماره اقتصادی
     */
    private int calculateTaxIdConfidence(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        
        // حذف فاصله و کاراکترهای غیرعددی
        String cleaned = value.replaceAll("[^\\d]", "");
        
        // بررسی طول
        if (cleaned.length() < 10 || cleaned.length() > 11) {
            return 20; // طول نامعتبر
        }
        
        // بررسی فرمت
        if (!TAX_ID_PATTERN.matcher(cleaned).matches()) {
            return 30;
        }
        
        // اگر همه چیز درست بود
        if (cleaned.length() == 10 || cleaned.length() == 11) {
            return 95; // confidence بالا
        }
        
        return 50;
    }
    
    /**
     * محاسبه confidence برای شماره صورتحساب
     */
    private int calculateInvoiceNumberConfidence(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        
        // بررسی طول
        if (value.length() < 4 || value.length() > 20) {
            return 30;
        }
        
        // بررسی فرمت
        if (!INVOICE_NUMBER_PATTERN.matcher(value).matches()) {
            return 40;
        }
        
        // اگر شامل اعداد است
        if (value.matches(".*\\d+.*")) {
            return 80;
        }
        
        return 50;
    }
    
    /**
     * محاسبه confidence برای تاریخ
     */
    private int calculateDateConfidence(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        
        // بررسی فرمت
        if (!DATE_PATTERN.matcher(value).matches()) {
            return 30;
        }
        
        // بررسی اینکه شامل اعداد معتبر است
        String[] parts = value.split("[/-]");
        if (parts.length != 3) {
            return 40;
        }
        
        try {
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            
            // بررسی محدوده معقول
            if (year < 1300 || year > 1500) { // محدوده شمسی
                return 50; // ممکن است میلادی باشد
            }
            if (month < 1 || month > 12) {
                return 30;
            }
            if (day < 1 || day > 31) {
                return 30;
            }
            
            return 90; // confidence بالا
        } catch (NumberFormatException e) {
            return 40;
        }
    }
    
    /**
     * محاسبه confidence برای مبلغ
     */
    private int calculateAmountConfidence(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        
        // حذف comma و کاراکترهای غیرعددی
        String cleaned = value.replaceAll("[^\\d.]", "");
        
        if (cleaned.isBlank()) {
            return 20;
        }
        
        // بررسی فرمت
        if (!AMOUNT_PATTERN.matcher(cleaned).matches()) {
            return 40;
        }
        
        try {
            double amount = Double.parseDouble(cleaned);
            if (amount < 0) {
                return 30; // مبلغ منفی
            }
            if (amount == 0) {
                return 50; // مبلغ صفر (ممکن است معتبر باشد)
            }
            
            return 85; // confidence خوب
        } catch (NumberFormatException e) {
            return 40;
        }
    }
    
    /**
     * محاسبه confidence برای نام
     */
    private int calculateNameConfidence(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        
        // بررسی طول
        if (value.length() < 3) {
            return 30;
        }
        if (value.length() > 100) {
            return 40; // خیلی طولانی
        }
        
        // بررسی اینکه شامل حروف فارسی/انگلیسی است
        if (!value.matches(".*[\\p{L}].*")) {
            return 20; // هیچ حرفی ندارد
        }
        
        // بررسی فرمت
        if (NAME_PATTERN.matcher(value).matches()) {
            return 80; // confidence خوب
        }
        
        return 60; // confidence متوسط
    }
    
    /**
     * محاسبه confidence برای کد کالا
     */
    private int calculateItemCodeConfidence(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        
        // بررسی طول
        if (value.length() < 1 || value.length() > 50) {
            return 30;
        }
        
        // اگر شامل اعداد و حروف است
        if (value.matches(".*[\\dA-Za-z].*")) {
            return 75;
        }
        
        return 50;
    }
    
    /**
     * محاسبه confidence کلی برای کل نتیجه استخراج
     */
    public int calculateOverallConfidence(Map<String, Integer> fieldConfidences) {
        if (fieldConfidences == null || fieldConfidences.isEmpty()) {
            return 0;
        }
        
        // محاسبه میانگین weighted (فیلدهای مهم وزن بیشتری دارند)
        int totalWeight = 0;
        int weightedSum = 0;
        
        // وزن‌های مختلف برای فیلدهای مختلف
        Map<String, Integer> weights = new HashMap<>();
        weights.put("sellerTaxId", 15);
        weights.put("buyerTaxId", 10);
        weights.put("invoiceNumber", 15);
        weights.put("issueDate", 15);
        weights.put("sellerName", 10);
        weights.put("buyerName", 10);
        weights.put("totalAmount", 15);
        weights.put("pattern", 10);
        
        for (Map.Entry<String, Integer> entry : fieldConfidences.entrySet()) {
            String field = entry.getKey();
            int confidence = entry.getValue();
            
            // پیدا کردن وزن (default: 5)
            int weight = weights.getOrDefault(field, 5);
            
            // برای items، وزن کمتر
            if (field.startsWith("items[")) {
                weight = 2;
            }
            
            weightedSum += confidence * weight;
            totalWeight += weight;
        }
        
        if (totalWeight == 0) {
            return 0;
        }
        
        return weightedSum / totalWeight;
    }
}

