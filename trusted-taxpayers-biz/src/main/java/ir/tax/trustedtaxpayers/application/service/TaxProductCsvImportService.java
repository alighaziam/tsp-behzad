package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.util.JalaliDateUtils;
import ir.tax.trustedtaxpayers.enums.ProductTaxable;
import ir.tax.trustedtaxpayers.enums.TaxProductType;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.TaxProductEntity;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * سرویس برای import محصولات مالیاتی از فایل CSV
 */
@Slf4j
@Service
public class TaxProductCsvImportService {

    private final TaxProductRepository taxProductRepository;
    private final TaxProductService taxProductService;

    public TaxProductCsvImportService(TaxProductRepository taxProductRepository, TaxProductService taxProductService) {
        this.taxProductRepository = taxProductRepository;
        this.taxProductService = taxProductService;
    }

    /**
     * آپلود و import محصولات مالیاتی از فایل CSV
     * 
     * @param file فایل CSV
     * @return نتیجه import شامل تعداد موفق و ناموفق
     */
    @Transactional
    public TaxProductImportResult importFromCsv(MultipartFile file) {
        List<TaxProductEntity> entities = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        Set<String> processedIds = new HashSet<>();
        int duplicateCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            
            // خواندن header
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return TaxProductImportResult.failure("فایل خالی است");
            }

            // Parse header
            String[] headers = parseCsvLine(headerLine);
            
            // Log headers for debugging
            log.debug("CSV Headers found: {}", String.join(", ", headers));
            
            // Try multiple variations of ID column name
            int idIndex = findColumnIndex(headers, "id");
            if (idIndex < 0) {
                idIndex = findColumnIndex(headers, "ID");
            }
            if (idIndex < 0) {
                idIndex = findColumnIndex(headers, "Id");
            }
            int descriptionIndex = findColumnIndex(headers, "DescriptionOfID");
            int typeIndex = findColumnIndex(headers, "Type");
            int taxableIndex = findColumnIndex(headers, "Taxable");
            int runDateIndex = findColumnIndex(headers, "RunDate");
            int expirationIndex = findColumnIndex(headers, "Expiration");
            int vatIndex = findColumnIndex(headers, "Vat");
            int taxCreateDateIndex = findColumnIndex(headers, "TaxCreateDate");
            int taxLastEditDateIndex = findColumnIndex(headers, "TaxLastEditDate");
            int pricingDescriptionIndex = findColumnIndex(headers, "PricingDescription");
            
            // بررسی وجود ستون‌های اجباری
            if (idIndex < 0) {
                return TaxProductImportResult.failure("ستون 'id' در فایل CSV یافت نشد. Header های موجود: " + String.join(", ", headers));
            }
            if (typeIndex < 0) {
                return TaxProductImportResult.failure("ستون 'Type' در فایل CSV یافت نشد. Header های موجود: " + String.join(", ", headers));
            }
            if (taxableIndex < 0) {
                return TaxProductImportResult.failure("ستون 'Taxable' در فایل CSV یافت نشد. Header های موجود: " + String.join(", ", headers));
            }
            if (runDateIndex < 0) {
                return TaxProductImportResult.failure("ستون 'RunDate' در فایل CSV یافت نشد. Header های موجود: " + String.join(", ", headers));
            }

            // خواندن ردیف‌های داده
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                
                // اگر ردیف خالی است، رد کن
                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    String[] values = parseCsvLine(line);
                    
                    // خواندن ID (TAX_PRODUCT_ID) - اگر ID خالی است، به آخر فایل رسیده‌ایم
                    String taxProductIdRaw = getRawValue(values, idIndex);
                    if (taxProductIdRaw == null || taxProductIdRaw.isBlank()) {
                        // ID خالی یعنی آخرین ردیف انجام شده - متوقف می‌شویم
                        log.debug("ردیف {}: فیلد ID خالی است - پردازش فایل متوقف شد", lineNumber);
                        break; // به جای continue، break می‌کنیم
                    }
                    // ذخیره بدون trim
                    final String taxProductId = taxProductIdRaw;
                    // برای مقایسه، normalize می‌کنیم
                    final String taxProductIdNormalized = normalizeForComparison(taxProductId);

                    // بررسی duplicate (مقایسه با normalize)
                    if (processedIds.contains(taxProductIdNormalized)) {
                        duplicateCount++;
                        errors.add("ردیف " + lineNumber + ": شناسه تکراری در فایل: " + taxProductId);
                        continue;
                    }

                    // بررسی وجود در دیتابیس (با trim برای مقایسه - هر دو طرف)
                    boolean exists = taxProductRepository.findAll().stream()
                            .anyMatch(e -> normalizeForComparison(e.getTaxProductId())
                                    .equals(taxProductIdNormalized));
                    if (exists) {
                        duplicateCount++;
                        errors.add("ردیف " + lineNumber + ": شناسه در دیتابیس موجود است: " + taxProductId);
                        continue;
                    }

                    processedIds.add(taxProductIdNormalized);

                    // خواندن و تبدیل DescriptionOfID -> title
                    String titleRaw = getRawValue(values, descriptionIndex);
                    // ذخیره بدون trim
                    String title = titleRaw;

                    // خواندن و تبدیل Type (فارسی به enum) - normalize برای مقایسه
                    String typePersianRaw = getRawValue(values, typeIndex);
                    // برای مقایسه enum، normalize می‌کنیم
                    String typePersianNormalized = typePersianRaw != null ? normalizeForComparison(typePersianRaw) : null;
                    TaxProductType type = null;
                    if (typePersianNormalized != null && !typePersianNormalized.isBlank()) {
                        type = TaxProductType.fromPersianName(typePersianNormalized);
                        if (type == null) {
                            // Try with trimmed version for enum comparison (هر دو طرف normalize)
                            for (TaxProductType t : TaxProductType.values()) {
                                if (t.getPersianName() != null && 
                                    typePersianNormalized.equals(normalizeForComparison(t.getPersianName()))) {
                                    type = t;
                                    break;
                                }
                            }
                        }
                        if (type == null) {
                            errors.add("ردیف " + lineNumber + ": نوع محصول نامعتبر: " + typePersianRaw);
                            continue;
                        }
                    } else {
                        errors.add("ردیف " + lineNumber + ": فیلد Type خالی است");
                        continue;
                    }

                    // خواندن و تبدیل Taxable (فارسی به enum) - normalize برای مقایسه
                    String taxablePersianRaw = getRawValue(values, taxableIndex);
                    // برای مقایسه enum، normalize می‌کنیم
                    String taxablePersianNormalized = taxablePersianRaw != null ? normalizeForComparison(taxablePersianRaw) : null;
                    ProductTaxable taxable = null;
                    if (taxablePersianNormalized != null && !taxablePersianNormalized.isBlank()) {
                        taxable = ProductTaxable.fromPersianName(taxablePersianNormalized);
                        if (taxable == null) {
                            // Try with trimmed version for enum comparison (هر دو طرف normalize)
                            for (ProductTaxable t : ProductTaxable.values()) {
                                if (t.getPersianName() != null && 
                                    taxablePersianNormalized.equals(normalizeForComparison(t.getPersianName()))) {
                                    taxable = t;
                                    break;
                                }
                            }
                        }
                        if (taxable == null) {
                            errors.add("ردیف " + lineNumber + ": نوع مشمولیت مالیاتی نامعتبر: " + taxablePersianRaw);
                            continue;
                        }
                    } else {
                        errors.add("ردیف " + lineNumber + ": فیلد Taxable خالی است");
                        continue;
                    }

                    // خواندن و تبدیل RunDate (شمسی به LocalDate)
                    String runDateStrRaw = getRawValue(values, runDateIndex);
                    // برای پارس کردن تاریخ، normalize می‌کنیم (فقط برای پارس، نه ذخیره)
                    String runDateStr = runDateStrRaw != null ? normalizeForComparison(runDateStrRaw) : null;
                    LocalDate runDate = null;
                    if (runDateStr != null && !runDateStr.isBlank()) {
                        try {
                            runDate = JalaliDateUtils.parseJalaliDate(runDateStr);
                        } catch (Exception e) {
                            errors.add("ردیف " + lineNumber + ": تاریخ RunDate نامعتبر: " + runDateStrRaw);
                            continue;
                        }
                    } else {
                        errors.add("ردیف " + lineNumber + ": فیلد RunDate خالی است");
                        continue;
                    }

                    // خواندن و تبدیل Expiration (شمسی به LocalDate)
                    String expirationStrRaw = getRawValue(values, expirationIndex);
                    // برای پارس کردن تاریخ، normalize می‌کنیم (فقط برای پارس، نه ذخیره)
                    String expirationStr = expirationStrRaw != null ? normalizeForComparison(expirationStrRaw) : null;
                    LocalDate expiration = null;
                    if (expirationStr != null && !expirationStr.isBlank()) {
                        try {
                            expiration = JalaliDateUtils.parseJalaliDate(expirationStr);
                        } catch (Exception e) {
                            log.warn("ردیف {}: تاریخ Expiration نامعتبر: {}", lineNumber, expirationStrRaw);
                        }
                    }

                    // خواندن Vat
                    String vatStrRaw = getRawValue(values, vatIndex);
                    // برای پارس کردن عدد، normalize می‌کنیم (فقط برای پارس، نه ذخیره)
                    String vatStr = vatStrRaw != null ? normalizeForComparison(vatStrRaw) : null;
                    Integer vat = null;
                    if (vatStr != null && !vatStr.isBlank()) {
                        try {
                            vat = Integer.parseInt(vatStr);
                        } catch (NumberFormatException e) {
                            log.warn("ردیف {}: مقدار VAT نامعتبر: {}", lineNumber, vatStrRaw);
                        }
                    }

                    // خواندن و تبدیل TaxCreateDate (شمسی به LocalDate)
                    String taxCreateDateStrRaw = getRawValue(values, taxCreateDateIndex);
                    // برای پارس کردن تاریخ، normalize می‌کنیم (فقط برای پارس، نه ذخیره)
                    String taxCreateDateStr = taxCreateDateStrRaw != null ? normalizeForComparison(taxCreateDateStrRaw) : null;
                    LocalDate taxCreateDate = null;
                    if (taxCreateDateStr != null && !taxCreateDateStr.isBlank()) {
                        try {
                            taxCreateDate = JalaliDateUtils.parseJalaliDate(taxCreateDateStr);
                        } catch (Exception e) {
                            log.warn("ردیف {}: تاریخ TaxCreateDate نامعتبر: {}", lineNumber, taxCreateDateStrRaw);
                        }
                    }

                    // خواندن و تبدیل TaxLastEditDate (شمسی به LocalDate)
                    String taxLastEditDateStrRaw = getRawValue(values, taxLastEditDateIndex);
                    // برای پارس کردن تاریخ، normalize می‌کنیم (فقط برای پارس، نه ذخیره)
                    String taxLastEditDateStr = taxLastEditDateStrRaw != null ? normalizeForComparison(taxLastEditDateStrRaw) : null;
                    LocalDate taxLastEditDate = null;
                    if (taxLastEditDateStr != null && !taxLastEditDateStr.isBlank()) {
                        try {
                            taxLastEditDate = JalaliDateUtils.parseJalaliDate(taxLastEditDateStr);
                        } catch (Exception e) {
                            log.warn("ردیف {}: تاریخ TaxLastEditDate نامعتبر: {}", lineNumber, taxLastEditDateStrRaw);
                        }
                    }

                    // خواندن PricingDescription
                    String pricingDescriptionRaw = getRawValue(values, pricingDescriptionIndex);
                    // ذخیره بدون trim
                    String pricingDescription = pricingDescriptionRaw;

                    // اعتبارسنجی قبل از ایجاد Entity
                    List<String> validationErrors = validateEntity(lineNumber, taxProductId, type, taxable, runDate);
                    if (!validationErrors.isEmpty()) {
                        errors.addAll(validationErrors);
                        continue; // این رکورد را رد می‌کنیم
                    }

                    // ایجاد Entity فقط برای رکوردهای معتبر
                    TaxProductEntity entity = TaxProductEntity.builder()
                            .taxProductId(taxProductId)
                            .productTitle(title)
                            .vat(vat)
                            .taxable(taxable)
                            .runDate(runDate)
                            .expiration(expiration)
                            .type(type)
                            .pricingDescription(pricingDescription)
                            .taxCreateDate(taxCreateDate)
                            .taxLastEditDate(taxLastEditDate)
                            .build();

                    entities.add(entity);

                } catch (Exception e) {
                    errors.add("ردیف " + lineNumber + ": خطا در پردازش - " + e.getMessage());
                    log.error("خطا در پردازش ردیف {}: ", lineNumber, e);
                }
            }

            // ذخیره فقط entities معتبر (اعتبارسنجی قبلاً انجام شده)
            int savedCount = 0;
            if (!entities.isEmpty()) {
                try {
                    taxProductRepository.saveAll(entities);
                    savedCount = entities.size();
                } catch (Exception e) {
                    log.error("خطا در ذخیره رکوردها: ", e);
                    errors.add("خطا در ذخیره رکوردها: " + e.getMessage());
                }
            }

            return TaxProductImportResult.success(
                    savedCount,
                    duplicateCount,
                    errors
            );

        } catch (Exception e) {
            log.error("خطا در خواندن فایل CSV: ", e);
            return TaxProductImportResult.failure("خطا در خواندن فایل: " + e.getMessage());
        }
    }

    /**
     * Parse CSV line (handling quoted values)
     * Note: Values are NOT trimmed to preserve original data for storage
     */
    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString());

        return values.toArray(new String[0]);
    }

    /**
     * Find column index by name (case-insensitive, handles various formats)
     */
    private int findColumnIndex(String[] headers, String columnName) {
        if (headers == null || columnName == null) {
            return -1;
        }

        // پاک کردن فاصله‌ها، BOM و حروف بزرگ/کوچک
        String normalizedColumnName = columnName.replaceAll("[\\s\\uFEFF]", "").toLowerCase();

        for (int i = 0; i < headers.length; i++) {
            String header = headers[i];
            if (header == null) {
                continue;
            }

            // پاک کردن فاصله‌ها و BOM از هدر
            String normalizedHeader = header.replaceAll("[\\s\\uFEFF]", "").toLowerCase();

            if (normalizedHeader.equals(normalizedColumnName)) {
                log.debug("Found column '{}' at index {} (matched: '{}')", columnName, i, header);
                return i;
            }
        }

        log.warn("Column '{}' not found in headers. Available headers: {}", columnName, String.join(", ", headers));
        return -1;
    }


    /**
     * Get value from array by index (safe) and normalize
     */
    private String getValue(String[] values, int index) {
        if (index < 0 || index >= values.length) {
            return null;
        }
        String value = values[index];
        if (value == null || value.isEmpty()) {
            return null;
        }
        // Normalize: remove spaces and BOM
        return value.replaceAll("[\\s\\uFEFF]", "").trim();
    }
    
    /**
     * Get value from array by index without normalization (for cases where we need original value)
     * Note: Returns value as-is without trimming to preserve original data for storage
     */
    private String getRawValue(String[] values, int index) {
        if (index < 0 || index >= values.length) {
            return null;
        }
        String value = values[index];
        return value != null && !value.isEmpty() ? value : null;
    }
    
    /**
     * Normalize string for comparison (trim both sides)
     * Used when comparing values, not for storage
     */
    private String normalizeForComparison(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[\\s\\uFEFF]", "").trim();
    }

    /**
     * اعتبارسنجی Entity قبل از ذخیره
     */
    private List<String> validateEntity(int lineNumber, String taxProductId, TaxProductType type, 
                                        ProductTaxable taxable, LocalDate runDate) {
        List<String> validationErrors = new ArrayList<>();
        
        if (taxProductId == null || taxProductId.isBlank()) {
            validationErrors.add("ردیف " + lineNumber + ": فیلد ID خالی است");
        } else if (taxProductId.length() > 500) {
            validationErrors.add("ردیف " + lineNumber + ": طول فیلد ID بیش از 500 کاراکتر است: " + taxProductId.length());
        }
        
        if (type == null) {
            validationErrors.add("ردیف " + lineNumber + ": فیلد Type نامعتبر است");
        }
        
        if (taxable == null) {
            validationErrors.add("ردیف " + lineNumber + ": فیلد Taxable نامعتبر است");
        }
        
        if (runDate == null) {
            validationErrors.add("ردیف " + lineNumber + ": فیلد RunDate نامعتبر است");
        }
        
        return validationErrors;
    }

    /**
     * کلاس برای نگهداری نتیجه import
     */
    public static class TaxProductImportResult {
        private final boolean success;
        private final int importedCount;
        private final int duplicateCount;
        private final List<String> errors;
        private final String message;

        private TaxProductImportResult(boolean success, int importedCount, int duplicateCount, 
                                       List<String> errors, String message) {
            this.success = success;
            this.importedCount = importedCount;
            this.duplicateCount = duplicateCount;
            this.errors = errors != null ? errors : new ArrayList<>();
            this.message = message;
        }

        public static TaxProductImportResult success(int importedCount, int duplicateCount, List<String> errors) {
            StringBuilder message = new StringBuilder();
            
            // نمایش تعداد رکوردهای ذخیره شده
            if (importedCount > 0) {
                message.append(String.format("تعداد %d - رکورد با موفقیت ثبت گردید", importedCount));
            }
            
            // نمایش تعداد تکراری‌ها
            if (duplicateCount > 0) {
                if (message.length() > 0) {
                    message.append(". ");
                }
                message.append(String.format("تعداد %d رکورد تکراری بود", duplicateCount));
            }
            
            // نمایش خطاها
            if (errors != null && !errors.isEmpty()) {
                if (message.length() > 0) {
                    message.append(". ");
                }
                message.append(String.format("تعداد %d خطا رخ داد", errors.size()));
            }
            
            // اگر هیچ پیغامی نیست
            if (message.length() == 0) {
                message.append("هیچ رکوردی پردازش نشد");
            }
            
            return new TaxProductImportResult(true, importedCount, duplicateCount, errors, message.toString());
        }

        public static TaxProductImportResult failure(String message) {
            return new TaxProductImportResult(false, 0, 0, null, message);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public int getImportedCount() { return importedCount; }
        public int getDuplicateCount() { return duplicateCount; }
        public List<String> getErrors() { return errors; }
        public String getMessage() { return message; }
    }
}

