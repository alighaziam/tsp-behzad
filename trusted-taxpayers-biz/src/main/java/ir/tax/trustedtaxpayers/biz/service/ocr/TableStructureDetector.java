package ir.tax.trustedtaxpayers.biz.service.ocr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * سرویس تشخیص ساختار جدول در متن OCR
 * این سرویس ساختار جداول را تشخیص می‌دهد تا استخراج دقیق‌تر انجام شود
 */
@Service
@Slf4j
public class TableStructureDetector {
    
    // Constants for table detection
    private static final int MIN_CONSECUTIVE_ROWS_FOR_TABLE = 2;
    private static final int MIN_SIMPLE_ROWS_FOR_TABLE = 3;
    private static final int MAX_CONSECUTIVE_ROW_DISTANCE = 500;
    private static final int MAX_TABLE_LENGTH = 2000;
    private static final int MIN_FIELDS_PER_ROW = 3;
    
    // الگوهای تشخیص هدر جدول - بهبود یافته برای جداول پیچیده‌تر
    private static final List<Pattern> TABLE_HEADER_PATTERNS = List.of(
            // الگوی استاندارد: ردیف | شناسه | شرح | واحد | تعداد | مبلغ
            Pattern.compile("(?:ردیف|row)\\s*[|\\s\\-]+(?:شناسه|id)\\s*[|\\s\\-]+(?:شرح|description|title)\\s*[|\\s\\-]+(?:واحد|unit)\\s*[|\\s\\-]+(?:تعداد|quantity|amount)\\s*[|\\s\\-]+(?:مبلغ|price|fee)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            // الگوی ساده‌تر
            Pattern.compile("ردیف\\s+شناسه\\s+شرح\\s+واحد\\s+تعداد\\s+مبلغ", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            // الگوی با جداکننده‌های مختلف (pipe, dash, space, tab)
            Pattern.compile("ردیف[\\s|\\-\\t]+شناسه[\\s|\\-\\t]+شرح[\\s|\\-\\t]+واحد[\\s|\\-\\t]+تعداد[\\s|\\-\\t]+مبلغ", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            // الگوی با شناسه کالا/خدمت داخلی
            Pattern.compile("(?:ردیف|row)\\s*[|\\s\\-]+(?:شناسه\\s*کالا[\\s\\-]*خدمت\\s*داخلی|شناسه\\s*کالا[\\s\\-]*خدمت|شناسه|id)\\s*[|\\s\\-]+(?:شرح|description)\\s*[|\\s\\-]+(?:واحد|unit)\\s*[|\\s\\-]+(?:تعداد|quantity)\\s*[|\\s\\-]+(?:مبلغ\\s*واحد|price|fee)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            // الگوی چندخطی (header در دو خط)
            Pattern.compile("(?:ردیف|row)\\s*[|\\s\\-]+(?:شناسه|id)[\\s\\S]{0,100}?(?:شرح|description)[\\s\\S]{0,100}?(?:واحد|unit)[\\s\\S]{0,100}?(?:تعداد|quantity)[\\s\\S]{0,100}?(?:مبلغ|price|fee)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL),
            // الگوی با کلمات کلیدی بیشتر
            Pattern.compile("(?:مشخصات\\s*کالا[\\s\\-]*خدمت|اقلام|items)[\\s\\S]{0,200}?(?:ردیف|row)\\s*[|\\s\\-]+(?:شناسه|id)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE | Pattern.DOTALL)
    );
    
    /**
     * تشخیص وجود جدول در متن
     */
    public boolean hasTable(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        
        // بررسی وجود هدر جدول
        for (Pattern pattern : TABLE_HEADER_PATTERNS) {
            if (pattern.matcher(text).find()) {
                log.debug("Table header detected using pattern: {}", pattern.pattern());
                return true;
            }
        }
        
        // بررسی وجود ساختار جدولی (ردیف‌های متوالی با اعداد) - بهبود یافته
        // الگوی دقیق‌تر برای ردیف‌های جدول
        Pattern rowPattern = Pattern.compile(
            "\\b([۰-۹0-9]+)\\s+[\\s\\S]{0,50}?([۰-۹0-9]{10,20})?[\\s\\S]{0,100}?" +
            "([\\p{L}\\s\\-\\.]{5,100}?)\\s*[|\\s\\-\\t]+" +
            "([^|\\s\\-]{1,20}?)?\\s*[|\\s\\-\\t]+" +
            "([۰-۹0-9,\\.]+)\\s*[|\\s\\-\\t]+" +
            "([۰-۹0-9,\\.]+)",
            Pattern.MULTILINE
        );
        Matcher rowMatcher = rowPattern.matcher(text);
        int rowCount = 0;
        int lastRowEnd = -1;
        while (rowMatcher.find() && rowCount < 10) {
            // بررسی اینکه ردیف‌ها متوالی هستند (فاصله بین آنها زیاد نباشد)
            int currentStart = rowMatcher.start();
            if (lastRowEnd == -1 || (currentStart - lastRowEnd) < MAX_CONSECUTIVE_ROW_DISTANCE) {
                rowCount++;
                lastRowEnd = rowMatcher.end();
            }
        }
        
        if (rowCount >= MIN_CONSECUTIVE_ROWS_FOR_TABLE) {
            log.debug("Table structure detected: {} consecutive rows found", rowCount);
            return true;
        }
        
        // بررسی الگوی ساده‌تر برای جداول بدون header
        Pattern simpleRowPattern = Pattern.compile("^\\s*[۰-۹0-9]+\\s+[\\s\\S]{10,}\\s+[۰-۹0-9,\\.]+\\s+[۰-۹0-9,\\.]+", Pattern.MULTILINE);
        Matcher simpleMatcher = simpleRowPattern.matcher(text);
        int simpleRowCount = 0;
        while (simpleMatcher.find() && simpleRowCount < 5) {
            simpleRowCount++;
        }
        
        if (simpleRowCount >= MIN_SIMPLE_ROWS_FOR_TABLE) {
            log.debug("Simple table structure detected: {} rows found", simpleRowCount);
            return true;
        }
        
        return false;
    }
    
    /**
     * استخراج محدوده جدول از متن
     */
    public TableRegion findTableRegion(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        
        // جستجوی هدر جدول
        int headerStart = -1;
        int headerEnd = -1;
        
        for (Pattern pattern : TABLE_HEADER_PATTERNS) {
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                headerStart = matcher.start();
                headerEnd = matcher.end();
                log.debug("Table header found at position: {} - {}", headerStart, headerEnd);
                break;
            }
        }
        
        if (headerStart == -1) {
            return null;
        }
        
        // جستجوی پایان جدول (اولین خط خالی یا بخش بعدی) - بهبود یافته
        int tableEnd = text.length();
        String afterHeader = text.substring(headerEnd);
        
        // جستجوی الگوی پایان جدول (مثلاً "جمع کل" یا خط خالی یا بخش بعدی)
        Pattern endPattern = Pattern.compile(
            "(?:جمع\\s*کل|جمع\\s*نهایی|total|sum|\\n\\s*\\n|" +
            "مشخصات\\s*فروشنده|مشخصات\\s*خریدار|" +
            "مبلغ\\s*نهایی|مبلغ\\s*کل|" +
            "تاریخ|شماره\\s*صورتحساب)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        Matcher endMatcher = endPattern.matcher(afterHeader);
        if (endMatcher.find()) {
            tableEnd = headerEnd + endMatcher.start();
            log.debug("Table end detected at position: {} (pattern: {})", tableEnd, endMatcher.group());
        } else {
            // اگر الگوی پایان پیدا نشد، تا حداکثر طول جدول بعد از header را در نظر بگیریم
            if (afterHeader.length() > MAX_TABLE_LENGTH) {
                tableEnd = headerEnd + MAX_TABLE_LENGTH;
                log.debug("Table end set to max length: {}", tableEnd);
            }
        }
        
        return new TableRegion(headerStart, tableEnd);
    }
    
    /**
     * استخراج ردیف‌های جدول
     */
    public List<String> extractTableRows(String text, TableRegion region) {
        List<String> rows = new ArrayList<>();
        
        if (region == null || text == null || text.isBlank()) {
            return rows;
        }
        
        String tableText = text.substring(region.start, region.end);
        
        // تقسیم به خطوط
        String[] lines = tableText.split("\\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            
            // بررسی اینکه آیا این خط یک ردیف جدول است - بهبود یافته
            // ردیف جدول معمولاً با عدد شروع می‌شود و شامل چندین فیلد است
            // پشتیبانی از اعداد فارسی و انگلیسی
            if (line.matches("^[۰-۹0-9]+\\s+.*")) {
                // بررسی اینکه خط شامل حداقل تعداد فیلد لازم است
                String[] parts = line.split("[|\\s\\-\\t]+");
                if (parts.length >= MIN_FIELDS_PER_ROW) {
                    rows.add(line);
                }
            } else if (line.contains("|") || line.contains("\t")) {
                // اگر خط شامل جداکننده‌های جدول است (pipe یا tab)
                String[] parts = line.split("[|\\t]+");
                if (parts.length >= MIN_FIELDS_PER_ROW) {
                    rows.add(line);
                }
            }
        }
        
        log.debug("Extracted {} table rows from region", rows.size());
        return rows;
    }
    
    /**
     * کلاس برای نگهداری محدوده جدول
     */
    public static class TableRegion {
        public final int start;
        public final int end;
        
        public TableRegion(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}

