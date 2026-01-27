package ir.tax.trustedtaxpayers.biz.service;

import ir.tax.trustedtaxpayers.biz.service.ocr.TableStructureDetector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic field extraction from OCR text.
 * NOTE: This is a simple baseline using regex. Replace/extend with NLP as needed.
 * Enhanced with table detection and multi-pass extraction.
 */
@Service
@Slf4j
public class FieldExtractionService {
    
    // Constants for field extraction
    private static final int MIN_EXTRACTED_FIELDS_FOR_RETRY = 5;
    private static final int MAX_ITEMS_EXTRACTED = 50;
    private static final int MAX_SIMPLE_ITEMS_EXTRACTED = 20;
    private static final int MIN_ROW_COUNT_FOR_TABLE = 2;
    private static final int MIN_SIMPLE_ROW_COUNT_FOR_TABLE = 3;
    private static final int MAX_CONSECUTIVE_ROW_DISTANCE = 500;
    private static final int MIN_ITEM_DESCRIPTION_LENGTH = 3;
    private static final int MAX_SEARCH_RANGE_SELLER_BUYER = 300;
    private static final int MAX_SEARCH_RANGE_TAX_ID = 50;
    private static final int MAX_SEARCH_RANGE_INVOICE_NUMBER = 100;
    private static final int MAX_SEARCH_RANGE_DATE = 150;
    private static final int MAX_SEARCH_RANGE_AMOUNT = 200;
    private static final int MAX_SEARCH_RANGE_ITEM_CODE = 50;
    private static final int MAX_SEARCH_RANGE_ITEM_CONTEXT = 200;
    private static final int TAX_ID_MIN_LENGTH = 10;
    private static final int TAX_ID_MAX_LENGTH = 11;
    private static final int ITEM_CODE_MIN_LENGTH = 10;
    private static final int ITEM_CODE_MAX_LENGTH = 20;
    
    private final TableStructureDetector tableDetector;
    
    public FieldExtractionService(TableStructureDetector tableDetector) {
        this.tableDetector = tableDetector;
    }

    private static final List<Pattern> SELLER_TAX_ID_PATTERNS = List.of(
            // شماره اقتصادی فروشنده (most common in real invoices) - الگوهای دقیق‌تر
            Pattern.compile("(?:مشخصات\\s*فروشنده[\\s\\S]{0,300}?شماره\\s*اقتصادی[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9]{10,11})", Pattern.MULTILINE),
            Pattern.compile("(?:فروشنده[\\s\\S]{0,300}?شماره\\s*اقتصادی[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9]{10,11})", Pattern.MULTILINE),
            Pattern.compile("(?:مشخصات\\s*فروشنده[\\s\\S]{0,300}?کد\\s*اقتصادی[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9]{10,11})", Pattern.MULTILINE),
            Pattern.compile("(?:شناسه\\s*ملی\\s*فروشنده|کد\\s*اقتصادی\\s*فروشنده)\\s*[:\\-]?\\s*([۰-۹0-9]{10,11})", Pattern.MULTILINE),
            Pattern.compile("(?:فروشنده[\\s\\S]*?شناسه\\s*ملی[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9]{10,11})", Pattern.MULTILINE),
            Pattern.compile("(?:seller\\s*tax\\s*id|seller\\s*taxid)\\s*[:\\-]?\\s*([۰-۹0-9]{8,12})", Pattern.CASE_INSENSITIVE),
            // الگوی انعطاف‌پذیرتر برای شناسه‌های ملی
            Pattern.compile("(?:شناسه\\s*ملی[\\s\\S]{0,100}?فروشنده[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9]{10,11})", Pattern.MULTILINE),
            Pattern.compile("(?:فروشنده[\\s\\S]{0,200}?شناسه\\s*ملی[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9]{10,11})", Pattern.MULTILINE)
    );
    private static final List<Pattern> BUYER_TAX_ID_PATTERNS = List.of(
            // شماره اقتصادی خریدار (most common in real invoices) - الگوهای دقیق‌تر
            Pattern.compile("(?:مشخصات\\s*خریدار[\\s\\S]{0,300}?شماره\\s*اقتصادی[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9]{10,11})", Pattern.MULTILINE),
            Pattern.compile("(?:خریدار[\\s\\S]{0,300}?شماره\\s*اقتصادی[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9]{10,11})", Pattern.MULTILINE),
            Pattern.compile("(?:مشخصات\\s*خریدار[\\s\\S]{0,300}?کد\\s*اقتصادی[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9]{10,11})", Pattern.MULTILINE),
            Pattern.compile("(?:شناسه\\s*ملی\\s*خریدار|کد\\s*اقتصادی\\s*خریدار)\\s*[:\\-]?\\s*([۰-۹0-9]{10,11})", Pattern.MULTILINE),
            Pattern.compile("(?:خریدار[\\s\\S]*?شناسه\\s*ملی[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9]{10,11})", Pattern.MULTILINE),
            Pattern.compile("(?:buyer\\s*tax\\s*id|buyer\\s*taxid)\\s*[:\\-]?\\s*([۰-۹0-9]{8,12})", Pattern.CASE_INSENSITIVE),
            // الگوی انعطاف‌پذیرتر برای شناسه‌های ملی خریدار
            Pattern.compile("(?:شناسه\\s*ملی[\\s\\S]{0,100}?خریدار[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9]{10,11})", Pattern.MULTILINE),
            Pattern.compile("(?:خریدار[\\s\\S]{0,200}?شناسه\\s*ملی[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9]{10,11})", Pattern.MULTILINE)
    );
    private static final List<Pattern> INVOICE_NUMBER_PATTERNS = List.of(
            // شماره صورتحساب داخلی (Internal Invoice Number) - most common in real invoices - الگوهای دقیق‌تر
            Pattern.compile("(?:شماره\\s*صورتحساب\\s*داخلی[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9\\-]{4,20})", Pattern.MULTILINE),
            Pattern.compile("(?:شماره\\s*فاکتور[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9\\-]{4,20})", Pattern.MULTILINE),
            Pattern.compile("(?:شماره\\s*صورتحساب[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9\\-]{4,20})", Pattern.MULTILINE),
            Pattern.compile("(?:شماره\\s*صورتحساب\\s*داخلی)\\s*([۰-۹0-9\\-]{4,20})", Pattern.MULTILINE),
            Pattern.compile("(?:فاکتور|صورتحساب)[\\s\\S]{0,30}?[:\\-]?\\s*([۰-۹0-9\\-]{4,20})", Pattern.MULTILINE),
            Pattern.compile("(?:invoice\\s*no\\.?|invoice\\s*number)\\s*[:\\-]?\\s*([۰-۹0-9\\-_/A-Za-z]+)", Pattern.CASE_INSENSITIVE),
            // الگوی برای شماره صورتحساب در جدول اطلاعات
            Pattern.compile("(?:شماره[\\s\\S]{0,20}?صورتحساب[\\s\\S]{0,100}?)\\s*[:\\-]?\\s*([۰-۹0-9\\-]{4,20})", Pattern.MULTILINE)
    );
    private static final List<Pattern> TOTAL_AMOUNT_PATTERNS = List.of(
            // مبلغ نهایی (most common in real invoices) - الگوهای دقیق‌تر
            Pattern.compile("(?:مبلغ\\s*نهایی[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9,\\.]+)", Pattern.MULTILINE),
            Pattern.compile("(?:مبلغ\\s*کل\\s*کالا[\\s\\S]{0,20}?خدمت[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9,\\.]+)", Pattern.MULTILINE),
            Pattern.compile("(?:جمع\\s*کل[\\s\\S]{0,300}?مبلغ\\s*کل\\s*کالا[\\s\\S]{0,20}?خدمت[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9,\\.]+)", Pattern.MULTILINE),
            Pattern.compile("(?:جمع\\s*کل[\\s\\S]{0,100}?)\\s*[:\\-]?\\s*([۰-۹0-9,\\.]+)", Pattern.MULTILINE),
            Pattern.compile("(?:مبلغ\\s*کل[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9,\\.]+)", Pattern.MULTILINE),
            Pattern.compile("(?:total\\s*amount|total)\\s*[:\\-]?\\s*([۰-۹0-9,\\.]+)", Pattern.CASE_INSENSITIVE),
            // الگوی برای مبلغ در بخش جمع کل
            Pattern.compile("(?:جمع[\\s\\S]{0,200}?مبلغ[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9,\\.]+)", Pattern.MULTILINE)
    );
    private static final List<Pattern> ISSUE_DATE_PATTERNS = List.of(
            // تاریخ صدور صورتحساب اصلی الکترونیکی (most common in real invoices) - الگوهای دقیق‌تر
            Pattern.compile("(?:تاریخ\\s*صدور\\s*صورتحساب\\s*اصلی\\s*الکترونیکی[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9]{4}[\\/\\-][۰-۹0-9]{1,2}[\\/\\-][۰-۹0-9]{1,2})", Pattern.MULTILINE),
            Pattern.compile("(?:تاریخ\\s*و\\s*زمان\\s*ثبت\\s*صورتحساب\\s*اصلی\\s*الکترونیکی[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9]{4}[\\/\\-][۰-۹0-9]{1,2}[\\/\\-][۰-۹0-9]{1,2})", Pattern.MULTILINE),
            Pattern.compile("(?:تاریخ\\s*صدور[\\s\\S]{0,100}?)\\s*[:\\-]?\\s*([۰-۹0-9]{4}[\\/\\-][۰-۹0-9]{1,2}[\\/\\-][۰-۹0-9]{1,2})", Pattern.MULTILINE),
            Pattern.compile("(?:تاریخ|تاريخ)[\\s\\S]{0,50}?[:\\-]?\\s*([۰-۹0-9]{4}[\\/\\-][۰-۹0-9]{1,2}[\\/\\-][۰-۹0-9]{1,2})", Pattern.MULTILINE),
            Pattern.compile("(?:date)\\s*[:\\-]?\\s*([۰-۹0-9]{4}[\\/\\-][۰-۹0-9]{1,2}[\\/\\-][۰-۹0-9]{1,2})", Pattern.CASE_INSENSITIVE),
            // الگوی برای تاریخ در فرمت‌های مختلف
            Pattern.compile("(?:تاریخ[\\s\\S]{0,200}?)\\s*[:\\-]?\\s*([۰-۹0-9]{4}[\\/\\-][۰-۹0-9]{1,2}[\\/\\-][۰-۹0-9]{1,2})", Pattern.MULTILINE)
    );
    private static final List<Pattern> SELLER_NAME_PATTERNS = List.of(
            // نام بنگاه اقتصادی فروشنده (most common in real invoices) - با محدوده بیشتر
            Pattern.compile("(?:مشخصات\\s*فروشنده[\\s\\S]{0,800}?نام\\s*بنگاه\\s*اقتصادی)\\s*[:\\-]?\\s*([\\p{L}\\s\\-\\.]{3,100}?)(?:\\s*[\\r\\n]|\\s*شناسه|\\s*کد|\\s*شماره|$)", Pattern.MULTILINE),
            Pattern.compile("(?:فروشنده[\\s\\S]{0,800}?نام\\s*بنگاه\\s*اقتصادی)\\s*[:\\-]?\\s*([\\p{L}\\s\\-\\.]{3,100}?)(?:\\s*[\\r\\n]|\\s*شناسه|\\s*کد|\\s*شماره|$)", Pattern.MULTILINE),
            // نام شخص حقیقی/حقوقی فروشنده - با محدوده بیشتر
            Pattern.compile("(?:فروشنده[\\s\\S]{0,500}?نام\\s*شخص\\s*حقیقی[\\s\\S]{0,10}?حقوقی)\\s*[:\\-]?\\s*([\\p{L}\\s\\-\\.]{2,100}?)(?:\\s*[\\r\\n]|\\s*شناسه|\\s*کد|\\s*شماره|$)", Pattern.MULTILINE),
            // Pattern: فروشنده:\nنام: شرکت نمونه (most specific, checks for newline after فروشنده)
            Pattern.compile("فروشنده\\s*:?\\s*[\\r\\n]+\\s*نام\\s*[:\\-]?\\s*([\\p{L}\\s\\-\\.]{2,100}?)(?:\\s*[\\r\\n]|\\s*شناسه|\\s*کد|\\s*شماره|$)", Pattern.MULTILINE),
            // Pattern: فروشنده:\n...نام: شرکت نمونه (handles any whitespace/newlines between)
            Pattern.compile("فروشنده\\s*:?[\\s\\S]{0,300}?نام\\s*[:\\-]?\\s*([\\p{L}\\s\\-\\.]{2,100}?)(?:\\s*[\\r\\n]|\\s*شناسه|\\s*کد|\\s*شماره|$)", Pattern.MULTILINE),
            // Pattern: نام فروشنده: شرکت نمونه (single line)
            Pattern.compile("(?:نام\\s*فروشنده|فروشنده)\\s*[:\\-]?\\s*([\\p{L}\\s\\-\\.]{2,100}?)(?:\\s*[\\r\\n]|\\s*شناسه|\\s*کد|\\s*شماره|$)", Pattern.MULTILINE),
            // Pattern: فروشنده[any chars]نام[...]value (most flexible multi-line) - با محدوده بیشتر
            Pattern.compile("فروشنده[\\s\\S]{0,500}?نام[\\s\\S]{0,200}?[:\\-]?\\s*([\\p{L}\\s\\-\\.]{3,100}?)(?:\\s*[\\r\\n]|\\s*شناسه|\\s*کد|\\s*شماره|$)", Pattern.MULTILINE),
            // Pattern: مودی (معادل فروشنده در برخی فاکتورها)
            Pattern.compile("(?:مودی|مودع)\\s*[:\\-]?\\s*([\\p{L}\\s\\-\\.]{2,100}?)(?:\\s*[\\r\\n]|\\s*شناسه|\\s*کد|\\s*شماره|$)", Pattern.MULTILINE),
            Pattern.compile("(?:مشخصات\\s*مودی[\\s\\S]{0,500}?نام)\\s*[:\\-]?\\s*([\\p{L}\\s\\-\\.]{2,100}?)(?:\\s*[\\r\\n]|\\s*شناسه|\\s*کد|\\s*شماره|$)", Pattern.MULTILINE),
            // English pattern
            Pattern.compile("(?:seller\\s*name)\\s*[:\\-]?\\s*([A-Za-z\\s\\-\\.]{2,100})", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> BUYER_NAME_PATTERNS = List.of(
            // نام بنگاه اقتصادی خریدار (most common in real invoices) - با محدوده بیشتر
            Pattern.compile("(?:مشخصات\\s*خریدار[\\s\\S]{0,800}?نام\\s*بنگاه\\s*اقتصادی)\\s*[:\\-]?\\s*([\\p{L}\\s\\-\\.]{3,100}?)(?:\\s*[\\r\\n]|\\s*شناسه|\\s*کد|\\s*شماره|$)", Pattern.MULTILINE),
            Pattern.compile("(?:خریدار[\\s\\S]{0,800}?نام\\s*بنگاه\\s*اقتصادی)\\s*[:\\-]?\\s*([\\p{L}\\s\\-\\.]{3,100}?)(?:\\s*[\\r\\n]|\\s*شناسه|\\s*کد|\\s*شماره|$)", Pattern.MULTILINE),
            // نام شخص حقیقی/حقوقی خریدار - با محدوده بیشتر
            Pattern.compile("(?:خریدار[\\s\\S]{0,500}?نام\\s*شخص\\s*حقیقی[\\s\\S]{0,10}?حقوقی)\\s*[:\\-]?\\s*([\\p{L}\\s\\-\\.]{2,100}?)(?:\\s*[\\r\\n]|\\s*شناسه|\\s*کد|\\s*شماره|$)", Pattern.MULTILINE),
            // Pattern: خریدار:\nنام: مشتری نمونه (most specific, checks for newline after خریدار)
            Pattern.compile("خریدار\\s*:?\\s*[\\r\\n]+\\s*نام\\s*[:\\-]?\\s*([\\p{L}\\s\\-\\.]{2,100}?)(?:\\s*[\\r\\n]|\\s*شناسه|\\s*کد|\\s*شماره|$)", Pattern.MULTILINE),
            // Pattern: خریدار:\n...نام: مشتری نمونه (handles any whitespace/newlines between)
            Pattern.compile("خریدار\\s*:?[\\s\\S]{0,300}?نام\\s*[:\\-]?\\s*([\\p{L}\\s\\-\\.]{2,100}?)(?:\\s*[\\r\\n]|\\s*شناسه|\\s*کد|\\s*شماره|$)", Pattern.MULTILINE),
            // Pattern: نام خریدار: مشتری نمونه (single line)
            Pattern.compile("(?:نام\\s*خریدار|خریدار)\\s*[:\\-]?\\s*([\\p{L}\\s\\-\\.]{2,100}?)(?:\\s*[\\r\\n]|\\s*شناسه|\\s*کد|\\s*شماره|$)", Pattern.MULTILINE),
            // Pattern: خریدار[any chars]نام[...]value (most flexible multi-line) - با محدوده بیشتر
            Pattern.compile("خریدار[\\s\\S]{0,500}?نام[\\s\\S]{0,200}?[:\\-]?\\s*([\\p{L}\\s\\-\\.]{3,100}?)(?:\\s*[\\r\\n]|\\s*شناسه|\\s*کد|\\s*شماره|$)", Pattern.MULTILINE),
            // English pattern
            Pattern.compile("(?:buyer\\s*name)\\s*[:\\-]?\\s*([A-Za-z\\s\\-\\.]{2,100})", Pattern.CASE_INSENSITIVE)
    );
    
    // Pattern detection patterns
    private static final List<Pattern> PATTERN_NUMBER_PATTERNS = List.of(
            // شماره الگو به صورت عدد (most specific)
            Pattern.compile("(?:شماره\\s*الگو|الگوی\\s*صورتحساب|INP|inp)\\s*[:\\-]?\\s*(\\d{1,2})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("الگو\\s*[:\\-]?\\s*(\\d{1,2})"),
            Pattern.compile("pattern\\s*[:\\-]?\\s*(\\d{1,2})", Pattern.CASE_INSENSITIVE)
    );
    
    // Pattern name to number mapping (for detecting pattern from text like "فروش")
    // معادل‌سازی دقیق انواع الگوهای صورتحساب
    // استفاده از HashMap به جای Map.of() چون تعداد entries بیشتر از 10 است
    private static final Map<String, String> PATTERN_NAME_TO_NUMBER = new java.util.HashMap<>();
    
    static {
        // Pattern 1: فروش کالا و خدمات
        PATTERN_NAME_TO_NUMBER.put("فروش", "1");
        PATTERN_NAME_TO_NUMBER.put("فروش کالا", "1");
        PATTERN_NAME_TO_NUMBER.put("فروش خدمات", "1");
        PATTERN_NAME_TO_NUMBER.put("فروش کالا و خدمات", "1");
        PATTERN_NAME_TO_NUMBER.put("sales", "1");
        PATTERN_NAME_TO_NUMBER.put("sales goods", "1");
        PATTERN_NAME_TO_NUMBER.put("sales services", "1");
        // Pattern 2: فروش ارز
        PATTERN_NAME_TO_NUMBER.put("فروش ارز", "2");
        PATTERN_NAME_TO_NUMBER.put("currency sales", "2");
        PATTERN_NAME_TO_NUMBER.put("فروش ارزی", "2");
        // Pattern 3: طلا، جواهر و پلاتین
        PATTERN_NAME_TO_NUMBER.put("طلا", "3");
        PATTERN_NAME_TO_NUMBER.put("جواهر", "3");
        PATTERN_NAME_TO_NUMBER.put("پلاتین", "3");
        PATTERN_NAME_TO_NUMBER.put("طلا و جواهر", "3");
        PATTERN_NAME_TO_NUMBER.put("صورت حساب طلا", "3");
        PATTERN_NAME_TO_NUMBER.put("صورت حساب طلا، جواهر و پلاتین", "3");
        PATTERN_NAME_TO_NUMBER.put("gold", "3");
        PATTERN_NAME_TO_NUMBER.put("jewelry", "3");
        PATTERN_NAME_TO_NUMBER.put("platinum", "3");
        // Pattern 4: قرارداد
        PATTERN_NAME_TO_NUMBER.put("قرارداد", "4");
        PATTERN_NAME_TO_NUMBER.put("contract", "4");
        // Pattern 5: قبوض
        PATTERN_NAME_TO_NUMBER.put("قبوض", "5");
        PATTERN_NAME_TO_NUMBER.put("utility bill", "5");
        PATTERN_NAME_TO_NUMBER.put("قبض", "5");
    }
    
    // Pattern 2 (Currency) specific fields
    private static final List<Pattern> CUT_PATTERNS = List.of(
            Pattern.compile("(?:نوع\\s*ارز|CUT|cut)\\s*[:\\-]?\\s*([A-Z]{3})", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:ارز|currency)\\s*[:\\-]?\\s*([A-Z]{3})", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> CFEE_PATTERNS = List.of(
            Pattern.compile("(?:فی\\s*واحد\\s*ارزی|CFEE|cfee)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> EXR_PATTERNS = List.of(
            Pattern.compile("(?:نرخ\\s*تبدیل|EXR|exr|exchange\\s*rate)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE)
    );
    
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-M-d");
    
    // Additional field patterns for complete data model extraction
    private static final List<Pattern> TPRDIS_PATTERNS = List.of(
            Pattern.compile("(?:جمع\\s*مبلغ\\s*قبل\\s*از\\s*تخفیف|مبلغ\\s*قبل\\s*از\\s*تخفیف|tprdis)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:جمع\\s*قبل\\s*از\\s*تخفیف)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> TDIS_PATTERNS = List.of(
            Pattern.compile("(?:جمع\\s*تخفیف|مبلغ\\s*تخفیف|tdis)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> TADIS_PATTERNS = List.of(
            Pattern.compile("(?:جمع\\s*مبلغ\\s*بعد\\s*از\\s*تخفیف|مبلغ\\s*بعد\\s*از\\s*تخفیف|tadis)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> TVAM_PATTERNS = List.of(
            Pattern.compile("(?:جمع\\s*مالیات\\s*بر\\s*ارزش\\s*افزوده|مالیات\\s*بر\\s*ارزش\\s*افزوده|tvam)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:جمع\\s*مالیات|مالیات\\s*کل)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> SETM_PATTERNS = List.of(
            Pattern.compile("(?:نوع\\s*تسویه|تسویه|setm)\\s*[:\\-]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:نقدی|نسیه|نقدی\\s*و\\s*نسیه)", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> CAP_PATTERNS = List.of(
            Pattern.compile("(?:مبلغ\\s*نقدی|نقدی|cap)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> INSP_PATTERNS = List.of(
            Pattern.compile("(?:مبلغ\\s*اقساط|اقساط|insp)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> SBC_PATTERNS = List.of(
            Pattern.compile("(?:کد\\s*شعبه\\s*فروشنده|شعبه\\s*فروشنده|sbc)\\s*[:\\-]?\\s*(\\d{1,4})", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> BBC_PATTERNS = List.of(
            Pattern.compile("(?:کد\\s*شعبه\\s*خریدار|شعبه\\s*خریدار|bbc)\\s*[:\\-]?\\s*(\\d{1,4})", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> BPC_PATTERNS = List.of(
            Pattern.compile("(?:کد\\s*پستی\\s*خریدار|کد\\s*پستی|bpc)\\s*[:\\-]?\\s*(\\d{10})", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> BPN_PATTERNS = List.of(
            Pattern.compile("(?:شماره\\s*پاسپورت\\s*خریدار|پاسپورت|bpn)\\s*[:\\-]?\\s*([A-Z0-9]{1,9})", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> INTY_PATTERNS = List.of(
            Pattern.compile("(?:نوع\\s*صورتحساب|inty)\\s*[:\\-]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:فروش|برگشت|اصلاحیه)", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> INS_PATTERNS = List.of(
            Pattern.compile("(?:موضوع\\s*صورتحساب|ins)\\s*[:\\-]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:اصلی|اصلاحی|ابطالی|برگشت)", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> TOB_PATTERNS = List.of(
            Pattern.compile("(?:نوع\\s*خریدار|tob)\\s*[:\\-]?\\s*(\\d+)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(?:حقیقی|حقوقی|مشارکت\\s*مدنی|اتباع\\s*غیر\\s*ایرانی)", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> BID_PATTERNS = List.of(
            Pattern.compile("(?:شناسه\\s*ملی\\s*خریدار|کد\\s*فراگیر\\s*خریدار|bid)\\s*[:\\-]?\\s*(\\d{10,12})", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> IRTAXID_PATTERNS = List.of(
            Pattern.compile("(?:شناسه\\s*صورتحساب\\s*مرجع|صورتحساب\\s*مرجع|irtaxid)\\s*[:\\-]?\\s*([\\d\\-]+)", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> TODAM_PATTERNS = List.of(
            Pattern.compile("(?:جمع\\s*سایر\\s*عوارض|سایر\\s*عوارض|todam)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> TAX17_PATTERNS = List.of(
            Pattern.compile("(?:مالیات\\s*ماده\\s*17|ماده\\s*17|tax17)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> TVOP_PATTERNS = List.of(
            Pattern.compile("(?:جمع\\s*مالیات\\s*پرداختی|مالیات\\s*پرداختی|tvop)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> ASN_PATTERNS = List.of(
            Pattern.compile("(?:شماره\\s*اعلامیه\\s*فروش|اعلامیه\\s*فروش|asn)\\s*[:\\-]?\\s*([\\d\\-]+)", Pattern.CASE_INSENSITIVE)
    );
    private static final List<Pattern> ASD_PATTERNS = List.of(
            Pattern.compile("(?:تاریخ\\s*اعلامیه\\s*فروش|asd)\\s*[:\\-]?\\s*(\\d{4}[\\/\\-]\\d{1,2}[\\/\\-]\\d{1,2})", Pattern.CASE_INSENSITIVE)
    );
    
    // الگوهای اضافی برای اطلاعات مودیان (فروشنده)
    private static final List<Pattern> SELLER_ADDRESS_PATTERNS = List.of(
            Pattern.compile("(?:مشخصات\\s*فروشنده[\\s\\S]{0,500}?آدرس[\\s\\S]{0,100}?)\\s*[:\\-]?\\s*([^\\r\\n]{10,200}?)(?:\\s*[\\r\\n]|\\s*تلفن|\\s*کد\\s*پستی|\\s*شماره|$)", Pattern.MULTILINE),
            Pattern.compile("(?:فروشنده[\\s\\S]{0,500}?آدرس[\\s\\S]{0,100}?)\\s*[:\\-]?\\s*([^\\r\\n]{10,200}?)(?:\\s*[\\r\\n]|\\s*تلفن|\\s*کد\\s*پستی|\\s*شماره|$)", Pattern.MULTILINE),
            Pattern.compile("(?:آدرس\\s*فروشنده)\\s*[:\\-]?\\s*([^\\r\\n]{10,200}?)(?:\\s*[\\r\\n]|\\s*تلفن|\\s*کد\\s*پستی|$)", Pattern.MULTILINE)
    );
    private static final List<Pattern> SELLER_PHONE_PATTERNS = List.of(
            Pattern.compile("(?:مشخصات\\s*فروشنده[\\s\\S]{0,500}?تلفن[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9\\-\\s]{8,20})", Pattern.MULTILINE),
            Pattern.compile("(?:فروشنده[\\s\\S]{0,500}?تلفن[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9\\-\\s]{8,20})", Pattern.MULTILINE),
            Pattern.compile("(?:تلفن\\s*فروشنده)\\s*[:\\-]?\\s*([۰-۹0-9\\-\\s]{8,20})", Pattern.MULTILINE)
    );
    private static final List<Pattern> SELLER_POSTAL_CODE_PATTERNS = List.of(
            Pattern.compile("(?:مشخصات\\s*فروشنده[\\s\\S]{0,500}?کد\\s*پستی[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9]{10})", Pattern.MULTILINE),
            Pattern.compile("(?:فروشنده[\\s\\S]{0,500}?کد\\s*پستی[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9]{10})", Pattern.MULTILINE),
            Pattern.compile("(?:کد\\s*پستی\\s*فروشنده)\\s*[:\\-]?\\s*([۰-۹0-9]{10})", Pattern.MULTILINE)
    );
    private static final List<Pattern> SELLER_REGISTRATION_NUMBER_PATTERNS = List.of(
            Pattern.compile("(?:مشخصات\\s*فروشنده[\\s\\S]{0,500}?شماره\\s*ثبت[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9\\-]{4,20})", Pattern.MULTILINE),
            Pattern.compile("(?:فروشنده[\\s\\S]{0,500}?شماره\\s*ثبت[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9\\-]{4,20})", Pattern.MULTILINE),
            Pattern.compile("(?:شماره\\s*ثبت\\s*فروشنده)\\s*[:\\-]?\\s*([۰-۹0-9\\-]{4,20})", Pattern.MULTILINE)
    );
    
    // الگوهای اضافی برای اطلاعات خریداران
    private static final List<Pattern> BUYER_ADDRESS_PATTERNS = List.of(
            Pattern.compile("(?:مشخصات\\s*خریدار[\\s\\S]{0,500}?آدرس[\\s\\S]{0,100}?)\\s*[:\\-]?\\s*([^\\r\\n]{10,200}?)(?:\\s*[\\r\\n]|\\s*تلفن|\\s*کد\\s*پستی|\\s*شماره|$)", Pattern.MULTILINE),
            Pattern.compile("(?:خریدار[\\s\\S]{0,500}?آدرس[\\s\\S]{0,100}?)\\s*[:\\-]?\\s*([^\\r\\n]{10,200}?)(?:\\s*[\\r\\n]|\\s*تلفن|\\s*کد\\s*پستی|\\s*شماره|$)", Pattern.MULTILINE),
            Pattern.compile("(?:آدرس\\s*خریدار)\\s*[:\\-]?\\s*([^\\r\\n]{10,200}?)(?:\\s*[\\r\\n]|\\s*تلفن|\\s*کد\\s*پستی|$)", Pattern.MULTILINE)
    );
    private static final List<Pattern> BUYER_PHONE_PATTERNS = List.of(
            Pattern.compile("(?:مشخصات\\s*خریدار[\\s\\S]{0,500}?تلفن[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9\\-\\s]{8,20})", Pattern.MULTILINE),
            Pattern.compile("(?:خریدار[\\s\\S]{0,500}?تلفن[\\s\\S]{0,50}?)\\s*[:\\-]?\\s*([۰-۹0-9\\-\\s]{8,20})", Pattern.MULTILINE),
            Pattern.compile("(?:تلفن\\s*خریدار)\\s*[:\\-]?\\s*([۰-۹0-9\\-\\s]{8,20})", Pattern.MULTILINE)
    );
    
    // الگوهای اضافی برای اقلام خدمت/کالا
    private static final List<Pattern> ITEM_DESCRIPTION_PATTERNS = List.of(
            Pattern.compile("(?:شرح\\s*کالا|شرح\\s*خدمت|شرح|SSTT|sstt)\\s*[:\\-]?\\s*([^\\r\\n]{3,200}?)(?:\\s*[\\r\\n]|\\s*تعداد|\\s*واحد|\\s*قیمت|$)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
    );
    private static final List<Pattern> ITEM_QUANTITY_PATTERNS = List.of(
            Pattern.compile("(?:تعداد|مقدار|AM|am)\\s*[:\\-]?\\s*([۰-۹0-9,\\.]+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
    );
    private static final List<Pattern> ITEM_UNIT_PATTERNS = List.of(
            Pattern.compile("(?:واحد|MU|mu)\\s*[:\\-]?\\s*([^\\r\\n]{1,20}?)(?:\\s*[\\r\\n]|\\s*قیمت|\\s*فی|$)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
    );
    private static final List<Pattern> ITEM_PRICE_PATTERNS = List.of(
            Pattern.compile("(?:قیمت\\s*واحد|فی\\s*واحد|FEE|fee)\\s*[:\\-]?\\s*([۰-۹0-9,\\.]+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
            Pattern.compile("(?:قیمت|فی)\\s*[:\\-]?\\s*([۰-۹0-9,\\.]+)", Pattern.MULTILINE)
    );
    private static final List<Pattern> ITEM_TOTAL_PATTERNS = List.of(
            Pattern.compile("(?:مبلغ\\s*کالا|مبلغ\\s*خدمت|مبلغ\\s*کل)\\s*[:\\-]?\\s*([۰-۹0-9,\\.]+)", Pattern.MULTILINE)
    );
    
    // الگوهای اضافی برای نوع الگو
    private static final List<Pattern> PATTERN_NAME_PATTERNS = List.of(
            Pattern.compile("(?:الگوی\\s*صورتحساب|الگو)\\s*[:\\-]?\\s*([^\\r\\n]{5,100}?)(?:\\s*[\\r\\n]|\\s*شماره|\\s*تاریخ|$)", Pattern.MULTILINE),
            Pattern.compile("(?:نوع\\s*الگو|نوع\\s*صورتحساب)\\s*[:\\-]?\\s*([^\\r\\n]{5,100}?)(?:\\s*[\\r\\n]|\\s*شماره|$)", Pattern.MULTILINE)
    );

    public Map<String, Object> extract(String text) {
        if (text == null) text = "";
        
        log.debug("FieldExtractionService.extract() called with text length: {}", text.length());
        if (text.length() > 0) {
            log.debug("First 500 chars of text: {}", text.length() > 500 ? text.substring(0, 500) + "..." : text);
        }
        
        // ابتدا با نرمال‌سازی استخراج می‌کنیم (بهترین حالت)
        Map<String, Object> result = extractWithNormalization(text, true);
        
        // اگر نتیجه کامل نبود (مثلاً برخی فیلدها خالی هستند)، بدون نرمال‌سازی هم امتحان می‌کنیم
        int extractedFieldCount = (int) result.values().stream()
                .filter(v -> v != null && !v.toString().isBlank() && !(v instanceof List && ((List<?>) v).isEmpty()))
                .count();
        
        log.info("FieldExtractionService: Extracted {} fields in first pass", extractedFieldCount);
        result.forEach((key, value) -> {
            if (value != null && !value.toString().isBlank() && !(value instanceof List && ((List<?>) value).isEmpty())) {
                log.debug("  - {}: {}", key, value instanceof List ? ((List<?>) value).size() + " items" : value);
            }
        });
        
        // اگر کمتر از حداقل فیلد استخراج شده، بدون نرمال‌سازی هم امتحان می‌کنیم
        if (extractedFieldCount < MIN_EXTRACTED_FIELDS_FOR_RETRY) {
            log.debug("Few fields extracted ({}), trying without normalization", extractedFieldCount);
            Map<String, Object> result2 = extractWithNormalization(text, false);
            
            // Merge results - prioritize non-null values from result2
            int mergedCount = 0;
            for (Map.Entry<String, Object> entry : result2.entrySet()) {
                if (entry.getValue() != null && (result.get(entry.getKey()) == null || result.get(entry.getKey()).toString().isBlank())) {
                    result.put(entry.getKey(), entry.getValue());
                    mergedCount++;
                    log.debug("  Merged field from non-normalized extraction: {} = {}", entry.getKey(), entry.getValue());
                }
            }
            if (mergedCount > 0) {
                log.info("FieldExtractionService: Merged {} additional fields from non-normalized extraction", mergedCount);
            }
        }
        
        // اگر جدول تشخیص داده شد، استخراج دقیق‌تر انجام شود
        if (tableDetector != null && tableDetector.hasTable(text)) {
            log.debug("Table detected in text, using enhanced extraction");
            TableStructureDetector.TableRegion region = tableDetector.findTableRegion(text);
            if (region != null) {
                // استخراج items از محدوده جدول
                List<Map<String, Object>> tableItems = extractItemsFromTableRegion(text, region);
                if (!tableItems.isEmpty()) {
                    result.put("items", tableItems);
                    log.debug("Extracted {} items from table region", tableItems.size());
                }
            }
        }
        
        // Log final result summary
        int finalFieldCount = (int) result.values().stream()
                .filter(v -> v != null && !v.toString().isBlank() && !(v instanceof List && ((List<?>) v).isEmpty()))
                .count();
        log.info("FieldExtractionService: Final extraction result - {} fields extracted", finalFieldCount);
        
        return result;
    }
    
    /**
     * استخراج با نرمال‌سازی یا بدون آن
     */
    private Map<String, Object> extractWithNormalization(String text, boolean normalize) {
        String normalized = normalize ? normalizeDigits(text) : text;
        
        log.debug("FieldExtractionService.extractWithNormalization() - normalize: {}, text length: {}, normalized length: {}", 
                normalize, text.length(), normalized.length());
        
        String sellerTaxId = findFirst(text, SELLER_TAX_ID_PATTERNS);
        String buyerTaxId = findFirst(text, BUYER_TAX_ID_PATTERNS);
        String invoiceNumber = findFirst(text, INVOICE_NUMBER_PATTERNS);
        String totalAmount = findFirst(normalized, TOTAL_AMOUNT_PATTERNS);
        String issueDate = findFirst(normalized, ISSUE_DATE_PATTERNS);
        String sellerName = findFirst(text, SELLER_NAME_PATTERNS);
        String buyerName = findFirst(text, BUYER_NAME_PATTERNS);
        
        log.debug("FieldExtractionService: Basic fields extracted - sellerTaxId: {}, buyerTaxId: {}, invoiceNumber: {}, totalAmount: {}, issueDate: {}, sellerName: {}, buyerName: {}", 
                sellerTaxId != null ? "found" : "null", 
                buyerTaxId != null ? "found" : "null",
                invoiceNumber != null ? "found" : "null",
                totalAmount != null ? "found" : "null",
                issueDate != null ? "found" : "null",
                sellerName != null ? "found" : "null",
                buyerName != null ? "found" : "null");

        // Detect pattern number (INP) - try both numeric and text patterns
        String patternNumber = findFirst(normalized, PATTERN_NUMBER_PATTERNS);
        
        // If pattern number not found as number, try to detect from pattern name (e.g., "فروش")
        if (patternNumber == null || patternNumber.isEmpty()) {
            patternNumber = detectPatternFromName(text);
        }
        
        String detectedPattern = detectPattern(text, patternNumber);

        // Extract pattern-specific fields
        Map<String, Object> patternFields = extractPatternFields(text, normalized, patternNumber);

        // Extract all additional header fields
        String tprdis = findFirst(normalized, TPRDIS_PATTERNS);
        String tdis = findFirst(normalized, TDIS_PATTERNS);
        String tadis = findFirst(normalized, TADIS_PATTERNS);
        String tvam = findFirst(normalized, TVAM_PATTERNS);
        String setm = findFirst(normalized, SETM_PATTERNS);
        String cap = findFirst(normalized, CAP_PATTERNS);
        String insp = findFirst(normalized, INSP_PATTERNS);
        String sbc = findFirst(normalized, SBC_PATTERNS);
        String bbc = findFirst(normalized, BBC_PATTERNS);
        String bpc = findFirst(normalized, BPC_PATTERNS);
        String bpn = findFirst(text, BPN_PATTERNS);
        String inty = findFirst(normalized, INTY_PATTERNS);
        String ins = findFirst(normalized, INS_PATTERNS);
        String tob = findFirst(normalized, TOB_PATTERNS);
        String bid = findFirst(normalized, BID_PATTERNS);
        String irtaxid = findFirst(normalized, IRTAXID_PATTERNS);
        String todam = findFirst(normalized, TODAM_PATTERNS);
        String tax17 = findFirst(normalized, TAX17_PATTERNS);
        String tvop = findFirst(normalized, TVOP_PATTERNS);
        String asn = findFirst(normalized, ASN_PATTERNS);
        String asd = findFirst(normalized, ASD_PATTERNS);
        
        // Extract additional seller (مودی) information
        String sellerAddress = findFirst(text, SELLER_ADDRESS_PATTERNS);
        String sellerPhone = findFirst(text, SELLER_PHONE_PATTERNS);
        String sellerPostalCode = findFirst(normalized, SELLER_POSTAL_CODE_PATTERNS);
        String sellerRegistrationNumber = findFirst(normalized, SELLER_REGISTRATION_NUMBER_PATTERNS);
        
        // Extract additional buyer information
        String buyerAddress = findFirst(text, BUYER_ADDRESS_PATTERNS);
        String buyerPhone = findFirst(text, BUYER_PHONE_PATTERNS);
        
        // Extract pattern name
        String patternName = findFirst(text, PATTERN_NAME_PATTERNS);

        // Normalize numbers
        totalAmount = normalizeAmount(totalAmount);
        issueDate = normalizeDate(issueDate).orElse(issueDate);
        tprdis = normalizeAmount(tprdis);
        tdis = normalizeAmount(tdis);
        tadis = normalizeAmount(tadis);
        tvam = normalizeAmount(tvam);
        cap = normalizeAmount(cap);
        insp = normalizeAmount(insp);
        todam = normalizeAmount(todam);
        tax17 = normalizeAmount(tax17);
        tvop = normalizeAmount(tvop);
        asd = normalizeDate(asd).orElse(asd);

        // Normalize names
        sellerName = normalizeName(sellerName);
        buyerName = normalizeName(buyerName);
        patternName = normalizeName(patternName);
        
        // Normalize addresses and phone numbers
        sellerAddress = normalizeName(sellerAddress);
        buyerAddress = normalizeName(buyerAddress);
        sellerPhone = normalizePhone(sellerPhone);
        buyerPhone = normalizePhone(buyerPhone);
        
        // Standardize dates
        issueDate = standardizeDate(issueDate);
        asd = standardizeDate(asd);
        
        // Standardize amounts
        totalAmount = standardizeAmount(totalAmount);
        tprdis = standardizeAmount(tprdis);
        tdis = standardizeAmount(tdis);
        tadis = standardizeAmount(tadis);
        tvam = standardizeAmount(tvam);
        cap = standardizeAmount(cap);
        insp = standardizeAmount(insp);
        todam = standardizeAmount(todam);
        tax17 = standardizeAmount(tax17);
        tvop = standardizeAmount(tvop);
        
        // Use HashMap instead of Map.of() to allow null values
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("sellerName", emptyToNull(sellerName));
        result.put("sellerTaxId", emptyToNull(sellerTaxId));
        result.put("buyerName", emptyToNull(buyerName));
        result.put("buyerTaxId", emptyToNull(buyerTaxId));
        result.put("invoiceNumber", emptyToNull(invoiceNumber));
        result.put("totalAmount", emptyToNull(totalAmount));
        result.put("issueDate", emptyToNull(issueDate));
        result.put("pattern", emptyToNull(detectedPattern));
        
        // Add all extracted header fields
        result.put("tprdis", emptyToNull(tprdis));
        result.put("tdis", emptyToNull(tdis));
        result.put("tadis", emptyToNull(tadis));
        result.put("tvam", emptyToNull(tvam));
        result.put("setm", emptyToNull(setm));
        result.put("cap", emptyToNull(cap));
        result.put("insp", emptyToNull(insp));
        result.put("sbc", emptyToNull(sbc));
        result.put("bbc", emptyToNull(bbc));
        result.put("bpc", emptyToNull(bpc));
        result.put("bpn", emptyToNull(bpn));
        result.put("inty", emptyToNull(inty));
        result.put("ins", emptyToNull(ins));
        result.put("tob", emptyToNull(tob));
        result.put("bid", emptyToNull(bid));
        result.put("irtaxid", emptyToNull(irtaxid));
        result.put("todam", emptyToNull(todam));
        result.put("tax17", emptyToNull(tax17));
        result.put("tvop", emptyToNull(tvop));
        result.put("asn", emptyToNull(asn));
        result.put("asd", emptyToNull(asd));
        result.put("pattern", detectedPattern);
        result.put("patternName", emptyToNull(patternName));
        
        // Add additional seller (مودی) information
        result.put("sellerAddress", emptyToNull(sellerAddress));
        result.put("sellerPhone", emptyToNull(sellerPhone));
        result.put("sellerPostalCode", emptyToNull(sellerPostalCode));
        result.put("sellerRegistrationNumber", emptyToNull(sellerRegistrationNumber));
        
        // Add additional buyer information
        result.put("buyerAddress", emptyToNull(buyerAddress));
        result.put("buyerPhone", emptyToNull(buyerPhone));
        
        result.put("items", extractItems(text, normalized, patternNumber, patternFields)); // Extract items with pattern-specific fields
        return result;
    }
    
    private Map<String, Object> extractPatternFields(String text, String normalized, String patternNumber) {
        Map<String, Object> fields = new java.util.HashMap<>();
        
        if (patternNumber != null && !patternNumber.isEmpty()) {
            int pattern = Integer.parseInt(patternNumber);
            
            // Pattern 2: Currency Sales
            if (pattern == 2) {
                String cut = findFirst(text, CUT_PATTERNS);
                String cfee = findFirst(normalized, CFEE_PATTERNS);
                String exr = findFirst(normalized, EXR_PATTERNS);
                
                if (cut != null && !cut.isEmpty()) {
                    fields.put("cut", cut.toUpperCase());
                }
                if (cfee != null && !cfee.isEmpty()) {
                    fields.put("cfee", normalizeAmount(cfee));
                }
                if (exr != null && !exr.isEmpty()) {
                    fields.put("exr", normalizeAmount(exr));
                }
            }
        }
        
        return fields;
    }
    
    // Patterns for extracting items - compiled once for performance
    private static final List<Pattern> ITEM_CODE_PATTERNS = List.of(
        // شناسه کالا/خدمت (most common in real invoices) - پشتیبانی از اعداد فارسی و انگلیسی
        Pattern.compile("(?:شناسه\\s*کالا[\\s\\S]{0,10}?خدمت\\s*داخلی|شناسه\\s*کالا[\\s\\S]{0,10}?خدمت)[\\s\\S]{0,50}?[:\\-]?\\s*([۰-۹0-9]{10,20})", Pattern.MULTILINE),
        Pattern.compile("(?:کد\\s*کالا|کد\\s*خدمت|کد|code|sstid)[\\s\\S]{0,50}?[:\\-]?\\s*([۰-۹0-9A-Za-z\\-]{1,50})", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
        Pattern.compile("(?:کد|code)[\\s\\S]{0,30}?[:\\-]?\\s*([۰-۹0-9A-Za-z\\-]{1,50})", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE),
        Pattern.compile("(?:شناسه|id)[\\s\\S]{0,30}?[:\\-]?\\s*([۰-۹0-9A-Za-z\\-]{1,50})", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE)
    );
    
    private static final Pattern TABLE_ROW_PATTERN = Pattern.compile(
        "\\b([۰-۹0-9]+)\\s+[\\s\\S]{0,50}?([۰-۹0-9]{10,20})?[\\s\\S]{0,100}?" +
        "([\\p{L}\\s\\-\\.]{5,100}?)(?:\\s*[\\r\\n]|\\s*واحد|\\s*عدد|\\s*تعداد)" +
        "[\\s\\S]{0,50}?(?:عدد|واحد|unit)?[\\s\\S]{0,50}?" +
        "([۰-۹0-9,\\.]+)[\\s\\S]{0,50}?" +
        "([۰-۹0-9,\\.]+)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );
    
    private static final Pattern ITEM_SECTION_PATTERN = Pattern.compile(
        "(?:مشخصات\\s*کالا[\\s\\S]{0,10}?خدمت[\\s\\S]{0,20}?معامله|اقلام)[\\s\\S]{0,500}?" +
        "(?:ردیف|row)\\s*(\\d+)[\\s\\S]{0,200}?" +
        "(?:شناسه\\s*کالا[\\s\\S]{0,20}?خدمت[\\s\\S]{0,20}?داخلی|شناسه\\s*کالا[\\s\\S]{0,20}?خدمت)\\s*(\\d{10,20})?" +
        "[\\s\\S]{0,300}?" +
        "([\\p{L}\\s\\-\\.]{5,100}?)(?:\\s*[\\r\\n]|\\s*واحد|\\s*تعداد)" +
        "[\\s\\S]{0,100}?(?:واحد\\s*اندازه\\s*گیری|واحد|عدد)\\s*([^\\n]{1,20}?)?" +
        "[\\s\\S]{0,100}?(?:تعداد[\\s\\S]{0,10}?مقدار|تعداد|مقدار)\\s*([\\d,\\.]+)" +
        "[\\s\\S]{0,100}?(?:مبلغ\\s*واحد[\\s\\S]{0,10}?ریال|مبلغ\\s*واحد|فی\\s*واحد)\\s*([\\d,\\.]+)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );
    
    private static final Pattern PRECISE_TABLE_PATTERN = Pattern.compile(
        "(?:ردیف|row)\\s*([۰-۹0-9]+)[\\s\\S]{0,50}?" +
        "(?:شناسه\\s*کالا[\\s\\S]{0,20}?خدمت[\\s\\S]{0,20}?داخلی|شناسه\\s*کالا[\\s\\S]{0,20}?خدمت)[\\s\\S]{0,50}?[:\\-]?\\s*([۰-۹0-9]{10,20})?" +
        "[\\s\\S]{0,100}?" +
        "([\\p{L}\\s\\-\\.]{5,100}?)(?:\\s*[\\r\\n]|\\s*واحد|\\s*تعداد)" +
        "[\\s\\S]{0,50}?(?:واحد\\s*اندازه\\s*گیری|واحد|عدد)\\s*([^\\n]{1,20}?)?" +
        "[\\s\\S]{0,50}?(?:تعداد[\\s\\S]{0,10}?مقدار|تعداد|مقدار)[\\s\\S]{0,30}?[:\\-]?\\s*([۰-۹0-9,\\.]+)" +
        "[\\s\\S]{0,50}?(?:مبلغ\\s*واحد[\\s\\S]{0,10}?ریال|مبلغ\\s*واحد|فی\\s*واحد)[\\s\\S]{0,30}?[:\\-]?\\s*([۰-۹0-9,\\.]+)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );
    
    // Enhanced item extraction patterns - compiled once for performance
    private static final Pattern ITEM_PATTERN_1 = Pattern.compile(
        "(?:کالای|خدمت|item|محصول)\\s*(\\d+)?\\s*(?:[-–—]\\s*(?:کد|code)\\s*[:\\-]?\\s*([A-Za-z0-9\\-]{1,50}))?\\s*[-–—]\\s*(?:تعداد|مقدار|amount|am)\\s*[:\\-]?\\s*([\\d,\\.]+)\\s*[-–—]\\s*(?:قیمت|فی|price|fee)\\s*[:\\-]?\\s*([\\d,\\.]+)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );
    
    private static final Pattern ITEM_PATTERN_2 = Pattern.compile(
        "(?:اقلام|items)\\s*:?\\s*[\\r\\n]+\\s*(?:کالای|خدمت|item|محصول)\\s*(\\d+)?\\s*[-–—]\\s*(?:تعداد|مقدار|amount|am)\\s*[:\\-]?\\s*([\\d,\\.]+)\\s*[-–—]\\s*(?:قیمت|فی|price|fee)\\s*[:\\-]?\\s*([\\d,\\.]+)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );
    
    private static final Pattern ITEM_PATTERN_3 = Pattern.compile(
        "(?:کالای|کالا|خدمت|item|محصول)\\s*(\\d+)?\\s*[-–—]?\\s*([^\\n]{3,100}?)\\s*[-–—]\\s*(?:تعداد|مقدار|amount|am)\\s*[:\\-]?\\s*([\\d,\\.]+)\\s*[-–—]\\s*(?:قیمت|فی|price|fee)\\s*[:\\-]?\\s*([\\d,\\.]+)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );
    
    private static final Pattern ITEM_PATTERN_4 = Pattern.compile(
        "(?:کالای|کالا|خدمت|item|محصول)\\s*([۰-۹0-9]+|[^\\n]{1,50}?)\\s*(?:[-–—]\\s*(?:کد|code)[\\s\\S]{0,10}?[:\\-]?\\s*([۰-۹0-9A-Za-z\\-]{1,50}))?\\s*[-–—]\\s*(?:تعداد|مقدار|amount|am)[\\s\\S]{0,10}?[:\\-]?\\s*([۰-۹0-9,\\.]+)\\s*[-–—]\\s*(?:قیمت|فی|price|fee)[\\s\\S]{0,10}?[:\\-]?\\s*([۰-۹0-9,\\.]+)",
        Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );
    
    private List<Map<String, Object>> extractItems(String text, String normalized, String patternNumber, Map<String, Object> patternFields) {
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        
        // Try precise table structure first (most accurate for real invoices with table headers)
        Matcher preciseTableMatcher = PRECISE_TABLE_PATTERN.matcher(text);
        while (preciseTableMatcher.find() && items.size() < MAX_ITEMS_EXTRACTED) {
            Map<String, Object> item = new java.util.HashMap<>();
            
            String rowNumber = preciseTableMatcher.group(1);
            String itemCodeInternal = preciseTableMatcher.group(2) != null ? preciseTableMatcher.group(2).trim() : null;
            String itemCode = preciseTableMatcher.group(3) != null ? preciseTableMatcher.group(3).trim() : itemCodeInternal;
            String itemDescription = preciseTableMatcher.group(4) != null ? preciseTableMatcher.group(4).trim() : "";
            String unit = preciseTableMatcher.group(5) != null ? preciseTableMatcher.group(5).trim() : "عدد";
            String quantity = normalizeAmount(preciseTableMatcher.group(6));
            String price = normalizeAmount(preciseTableMatcher.group(7));
            
            // Clean and normalize item description
            itemDescription = normalizeName(itemDescription);
            if (itemDescription == null || itemDescription.isEmpty() || itemDescription.length() < 3) {
                itemDescription = "کالای " + rowNumber;
            }
            
            item.put("sstt", itemDescription);
            
            // Use itemCode if available, otherwise itemCodeInternal
            String finalCode = (itemCode != null && !itemCode.isEmpty()) ? itemCode : itemCodeInternal;
            if (finalCode != null && !finalCode.isEmpty()) {
                item.put("sstid", finalCode);
            }
            
            if (quantity != null && !quantity.isEmpty()) item.put("am", quantity);
            if (price != null && !price.isEmpty()) item.put("fee", price);
            item.put("mu", unit.isEmpty() ? "عدد" : unit);
            
            // Extract body fields (prdis, dis, adis, vra, vam) for this item
            extractItemBodyFields(item, text, normalized, preciseTableMatcher.start());
            
            addPatternFieldsToItem(item, patternNumber, patternFields);
            items.add(item);
            log.debug("Extracted item from precise table: row={}, code={}, desc={}, qty={}, price={}", 
                    rowNumber, finalCode, itemDescription, quantity, price);
        }
        
        // Try detailed table structure (fallback if precise pattern didn't match)
        if (items.isEmpty()) {
            Matcher itemSectionMatcher = ITEM_SECTION_PATTERN.matcher(text);
            while (itemSectionMatcher.find() && items.size() < MAX_ITEMS_EXTRACTED) {
                Map<String, Object> item = new java.util.HashMap<>();
                
                String rowNumber = itemSectionMatcher.group(1);
                String itemCode = itemSectionMatcher.group(2) != null ? itemSectionMatcher.group(2).trim() : null;
                String itemDescription = itemSectionMatcher.group(3) != null ? itemSectionMatcher.group(3).trim() : "";
                String unit = itemSectionMatcher.group(4) != null ? itemSectionMatcher.group(4).trim() : "عدد";
                String quantity = normalizeAmount(itemSectionMatcher.group(5));
                String price = normalizeAmount(itemSectionMatcher.group(6));
                
                // Clean and normalize item description
                itemDescription = normalizeName(itemDescription);
                if (itemDescription == null || itemDescription.isEmpty() || itemDescription.length() < 3) {
                    itemDescription = "کالای " + rowNumber;
                }
                
                item.put("sstt", itemDescription);
                
                if (itemCode != null && !itemCode.isEmpty()) {
                    item.put("sstid", itemCode);
                }
                
                if (quantity != null && !quantity.isEmpty()) item.put("am", quantity);
                if (price != null && !price.isEmpty()) item.put("fee", price);
                item.put("mu", unit.isEmpty() ? "عدد" : unit);
                
                // Extract body fields (prdis, dis, adis, vra, vam) for this item
                extractItemBodyFields(item, text, normalized, itemSectionMatcher.start());
                
                addPatternFieldsToItem(item, patternNumber, patternFields);
                items.add(item);
                log.debug("Extracted item from section pattern: row={}, code={}, desc={}, qty={}, price={}", 
                        rowNumber, itemCode, itemDescription, quantity, price);
            }
        }
        
        // If no items found, try simpler table row pattern
        if (items.isEmpty()) {
            Matcher tableRowMatcher = TABLE_ROW_PATTERN.matcher(text);
            while (tableRowMatcher.find() && items.size() < MAX_SIMPLE_ITEMS_EXTRACTED) { // Limit to prevent too many matches
                Map<String, Object> item = new java.util.HashMap<>();
                
                // rowNumber is extracted but not used - kept for potential future use
                @SuppressWarnings("unused")
                String rowNumber = tableRowMatcher.group(1);
                String itemCode = tableRowMatcher.group(2) != null ? tableRowMatcher.group(2).trim() : null;
                String itemDescription = tableRowMatcher.group(3) != null ? tableRowMatcher.group(3).trim() : "";
                String quantity = normalizeAmount(tableRowMatcher.group(4));
                String price = normalizeAmount(tableRowMatcher.group(5));
                
                // Clean and normalize item description
                itemDescription = normalizeName(itemDescription);
                if (itemDescription == null || itemDescription.isEmpty() || itemDescription.length() < 3) {
                    continue; // Skip invalid items
                }
                
                item.put("sstt", itemDescription);
                
                if (itemCode != null && !itemCode.isEmpty()) {
                    item.put("sstid", itemCode);
                }
                
                if (quantity != null) item.put("am", quantity);
                if (price != null) item.put("fee", price);
                item.put("mu", "عدد");
                
                addPatternFieldsToItem(item, patternNumber, patternFields);
                items.add(item);
            }
        }
        
        // Try pattern 1 first
        Matcher matcher1 = ITEM_PATTERN_1.matcher(text);
        while (matcher1.find()) {
            Map<String, Object> item = new java.util.HashMap<>();
            String itemNumber = matcher1.group(1) != null ? matcher1.group(1) : "";
            String itemCode = matcher1.group(2) != null ? matcher1.group(2).trim() : null;
            String quantity = normalizeAmount(matcher1.group(3));
            String price = normalizeAmount(matcher1.group(4));
            
            String itemName = itemNumber.isEmpty() ? "کالا" : "کالای " + itemNumber;
            item.put("sstt", itemName.trim());
            
            // Extract code if found in pattern, otherwise try to find it nearby
            if (itemCode != null && !itemCode.isEmpty()) {
                item.put("sstid", itemCode);
            } else {
                // Try to find code near this item match
                int matchStart = matcher1.start();
                int searchStart = Math.max(0, matchStart - 100);
                int searchEnd = Math.min(text.length(), matchStart + 200);
                String context = text.substring(searchStart, searchEnd);
                String foundCode = findFirst(context, ITEM_CODE_PATTERNS);
                if (foundCode != null && !foundCode.isEmpty()) {
                    item.put("sstid", foundCode);
                }
            }
            
            if (quantity != null) item.put("am", quantity);
            if (price != null) item.put("fee", price);
            item.put("mu", "عدد"); // Default unit
            
            // Add pattern-specific fields
            addPatternFieldsToItem(item, patternNumber, patternFields);
            
            items.add(item);
        }
        
        // Try pattern 2
        if (items.isEmpty()) {
            Matcher matcher2 = ITEM_PATTERN_2.matcher(text);
            while (matcher2.find()) {
                Map<String, Object> item = new java.util.HashMap<>();
                String itemNumber = matcher2.group(1) != null ? matcher2.group(1) : "1";
                String quantity = normalizeAmount(matcher2.group(2));
                String price = normalizeAmount(matcher2.group(3));
                
                item.put("sstt", "کالای " + itemNumber);
                if (quantity != null) item.put("am", quantity);
                if (price != null) item.put("fee", price);
                item.put("mu", "عدد");
                
                addPatternFieldsToItem(item, patternNumber, patternFields);
                items.add(item);
            }
        }
        
        // Try pattern 3
        if (items.isEmpty()) {
            Matcher matcher3 = ITEM_PATTERN_3.matcher(text);
            while (matcher3.find()) {
                Map<String, Object> item = new java.util.HashMap<>();
                String itemName = matcher3.group(2) != null ? matcher3.group(2).trim() : "کالای " + (matcher3.group(1) != null ? matcher3.group(1) : "");
                String quantity = normalizeAmount(matcher3.group(3));
                String price = normalizeAmount(matcher3.group(4));
                
                item.put("sstt", itemName);
                if (quantity != null) item.put("am", quantity);
                if (price != null) item.put("fee", price);
                item.put("mu", "عدد");
                
                addPatternFieldsToItem(item, patternNumber, patternFields);
                items.add(item);
            }
        }
        
        // Try pattern 4 (most flexible)
        if (items.isEmpty()) {
            Matcher matcher4 = ITEM_PATTERN_4.matcher(text);
            while (matcher4.find()) {
                Map<String, Object> item = new java.util.HashMap<>();
                String itemNamePart = matcher4.group(1) != null ? matcher4.group(1).trim() : "";
                String itemCode = matcher4.group(2) != null ? matcher4.group(2).trim() : null;
                String quantity = normalizeAmount(matcher4.group(3));
                String price = normalizeAmount(matcher4.group(4));
                
                // Build item name - if it's just a number, use "کالای X", otherwise use the text
                String itemName;
                if (itemNamePart.matches("\\d+")) {
                    itemName = "کالای " + itemNamePart;
                } else {
                    itemName = itemNamePart;
                    // Clean item name (remove common prefixes if any)
                    itemName = itemName.replaceAll("^(کالای|کالا|خدمت|item|محصول)\\s*", "").trim();
                    if (itemName.isEmpty()) {
                        itemName = "کالا";
                    }
                }
                
                item.put("sstt", itemName);
                
                // Extract code if found in pattern, otherwise try to find it nearby
                if (itemCode != null && !itemCode.isEmpty()) {
                    item.put("sstid", itemCode);
                } else {
                    // Try to find code near this item match
                    int matchStart = matcher4.start();
                    int searchStart = Math.max(0, matchStart - 100);
                    int searchEnd = Math.min(text.length(), matchStart + 200);
                    String context = text.substring(searchStart, searchEnd);
                    String foundCode = findFirst(context, ITEM_CODE_PATTERNS);
                    if (foundCode != null && !foundCode.isEmpty()) {
                        item.put("sstid", foundCode);
                    }
                }
                
                if (quantity != null) item.put("am", quantity);
                if (price != null) item.put("fee", price);
                item.put("mu", "عدد");
                
                addPatternFieldsToItem(item, patternNumber, patternFields);
                items.add(item);
            }
        }
        
        // Fallback: Try to extract at least item name if no structured pattern matches
        if (items.isEmpty()) {
            Pattern fallbackPattern = Pattern.compile("(?:اقلام|items)\\s*:?\\s*[\\r\\n]+\\s*([^\\n]{3,100})", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
            Matcher fallbackMatcher = fallbackPattern.matcher(text);
            if (fallbackMatcher.find()) {
                String itemLine = fallbackMatcher.group(1).trim();
                Map<String, Object> item = new java.util.HashMap<>();
                
                // Try to extract quantity and price from the line
                Pattern qtyPattern = Pattern.compile("(?:تعداد|مقدار|amount|am)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE);
                Pattern pricePattern = Pattern.compile("(?:قیمت|فی|price|fee)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE);
                
                Matcher qtyMatcher = qtyPattern.matcher(itemLine);
                Matcher priceMatcher = pricePattern.matcher(itemLine);
                
                // Extract item name (everything before "تعداد" or "قیمت")
                String itemName = itemLine.split("(?:تعداد|مقدار|قیمت|فی)")[0].trim();
                itemName = itemName.replaceAll("^[-–—\\s]+", "").trim();
                if (itemName.isEmpty()) {
                    itemName = "کالا";
                }
                
                item.put("sstt", itemName);
                
                if (qtyMatcher.find()) {
                    String quantity = normalizeAmount(qtyMatcher.group(1));
                    if (quantity != null) item.put("am", quantity);
                }
                
                if (priceMatcher.find()) {
                    String price = normalizeAmount(priceMatcher.group(1));
                    if (price != null) item.put("fee", price);
                }
                
                item.put("mu", "عدد");
                addPatternFieldsToItem(item, patternNumber, patternFields);
                items.add(item);
            }
        }
        
        return items.isEmpty() ? List.of() : items;
    }
    
    /**
     * استخراج items از محدوده جدول تشخیص داده شده
     */
    private List<Map<String, Object>> extractItemsFromTableRegion(String text, TableStructureDetector.TableRegion region) {
        List<Map<String, Object>> items = new java.util.ArrayList<>();
        
        if (tableDetector == null || region == null) {
            return items;
        }
        
        List<String> rows = tableDetector.extractTableRows(text, region);
        
        for (String row : rows) {
            // تجزیه ردیف به فیلدها
            // الگو: عدد | شناسه | شرح | واحد | تعداد | مبلغ
            Pattern rowPattern = Pattern.compile(
                    "(\\d+)\\s+[|\\s]+(\\d{10,20})?\\s*[|\\s]+([^|\\n]{5,100}?)\\s+[|\\s]+([^|\\n]{1,20}?)\\s+[|\\s]+([\\d,\\.]+)\\s+[|\\s]+([\\d,\\.]+)",
                    Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
            );
            
            Matcher matcher = rowPattern.matcher(row);
            if (matcher.find()) {
                Map<String, Object> item = new java.util.HashMap<>();
                item.put("sstt", matcher.group(3).trim());
                
                String code = matcher.group(2);
                if (code != null && !code.trim().isEmpty()) {
                    item.put("sstid", code.trim());
                }
                
                String unit = matcher.group(4);
                item.put("mu", unit != null && !unit.trim().isEmpty() ? unit.trim() : "عدد");
                
                String quantity = normalizeAmount(matcher.group(5));
                if (quantity != null) item.put("am", quantity);
                
                String price = normalizeAmount(matcher.group(6));
                if (price != null) item.put("fee", price);
                
                items.add(item);
            }
        }
        
        return items;
    }
    
    private void addPatternFieldsToItem(Map<String, Object> item, String patternNumber, Map<String, Object> patternFields) {
        if (patternNumber != null && "2".equals(patternNumber)) {
            if (patternFields.containsKey("cut")) {
                item.put("cut", patternFields.get("cut"));
            }
            if (patternFields.containsKey("cfee")) {
                item.put("cfee", patternFields.get("cfee"));
            }
            if (patternFields.containsKey("exr")) {
                item.put("exr", patternFields.get("exr"));
            }
        }
    }
    
    /**
     * استخراج فیلدهای body برای هر item از متن (prdis, dis, adis, vra, vam)
     */
    private void extractItemBodyFields(Map<String, Object> item, String text, String normalized, int matchStart) {
        // جستجوی فیلدهای body در محدوده نزدیک به item
        int searchStart = Math.max(0, matchStart - 200);
        int searchEnd = Math.min(text.length(), matchStart + 500);
        // context is used in normalizedContext calculation
        @SuppressWarnings("unused")
        String context = text.substring(searchStart, searchEnd);
        String normalizedContext = normalized.substring(Math.max(0, matchStart - 200), Math.min(normalized.length(), matchStart + 500));
        
        // الگوهای استخراج فیلدهای body
        List<Pattern> prdisPatterns = List.of(
                Pattern.compile("(?:مبلغ\\s*قبل\\s*از\\s*تخفیف|prdis)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE)
        );
        List<Pattern> disPatterns = List.of(
                Pattern.compile("(?:تخفیف|dis)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE)
        );
        List<Pattern> adisPatterns = List.of(
                Pattern.compile("(?:مبلغ\\s*بعد\\s*از\\s*تخفیف|adis)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE)
        );
        List<Pattern> vraPatterns = List.of(
                Pattern.compile("(?:نرخ\\s*مالیات|vra)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?:مالیات\\s*[:\\-]?\\s*)(\\d+(?:\\.\\d+)?)\\s*%", Pattern.CASE_INSENSITIVE)
        );
        List<Pattern> vamPatterns = List.of(
                Pattern.compile("(?:مبلغ\\s*مالیات|vam)\\s*[:\\-]?\\s*([\\d,\\.]+)", Pattern.CASE_INSENSITIVE)
        );
        
        String prdis = findFirst(normalizedContext, prdisPatterns);
        String dis = findFirst(normalizedContext, disPatterns);
        String adis = findFirst(normalizedContext, adisPatterns);
        String vra = findFirst(normalizedContext, vraPatterns);
        String vam = findFirst(normalizedContext, vamPatterns);
        
        if (prdis != null && !prdis.isEmpty()) {
            item.put("prdis", normalizeAmount(prdis));
        }
        if (dis != null && !dis.isEmpty()) {
            item.put("dis", normalizeAmount(dis));
        }
        if (adis != null && !adis.isEmpty()) {
            item.put("adis", normalizeAmount(adis));
        }
        if (vra != null && !vra.isEmpty()) {
            item.put("vra", normalizeAmount(vra));
        }
        if (vam != null && !vam.isEmpty()) {
            item.put("vam", normalizeAmount(vam));
        }
    }

    private static String findFirst(String text, List<Pattern> patterns) {
        for (Pattern p : patterns) {
            Matcher m = p.matcher(text);
            if (m.find()) {
                String result = m.group(1);
                if (result != null) {
                    // Trim and clean up the result
                    result = result.trim();
                    // Remove trailing colons, dashes, and other punctuation
                    result = result.replaceAll("[\\s:;\\-]+$", "");
                    // Limit length to prevent capturing too much
                    if (result.length() > 200) {
                        result = result.substring(0, 200).trim();
                    }
                    if (!result.isBlank()) {
                        return result;
                    }
                }
            }
        }
        return "";
    }

    private static Object emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static String normalizeDigits(String s) {
        if (s == null) return "";
        // Convert Persian/Arabic digits to ASCII
        StringBuilder out = new StringBuilder(s.length());
        for (char ch : s.toCharArray()) {
            if (ch >= 0x06F0 && ch <= 0x06F9) { // Persian
                out.append((char) ('0' + (ch - 0x06F0)));
            } else if (ch >= 0x0660 && ch <= 0x0669) { // Arabic
                out.append((char) ('0' + (ch - 0x0660)));
            } else {
                out.append(ch);
            }
        }
        return out.toString();
    }

    private static String normalizeAmount(String s) {
        if (s == null) return null;
        // حذف همه کاراکترهای غیرعددی به جز نقطه اعشار
        String t = s.replaceAll("[^\\d.]", "");
        // حذف نقطه‌های اضافی (فقط یک نقطه اعشار مجاز است)
        if (t.contains(".")) {
            int firstDot = t.indexOf('.');
            String beforeDot = t.substring(0, firstDot);
            String afterDot = t.substring(firstDot + 1).replaceAll("\\.", "");
            t = beforeDot + "." + afterDot;
        }
        return t.isBlank() ? null : t;
    }
    
    /**
     * نرمال‌سازی بهتر برای نام‌ها (حذف whitespace اضافی، تصحیح حروف)
     */
    private static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        
        // حذف whitespace و کاراکترهای غیرعددی (به جز dash)
        String normalized = phone.replaceAll("[^\\d\\-]", "").trim();
        
        // حذف dash های اضافی
        normalized = normalized.replaceAll("-+", "-");
        if (normalized.startsWith("-")) {
            normalized = normalized.substring(1);
        }
        if (normalized.endsWith("-")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        
        return normalized.isBlank() ? null : normalized;
    }
    
    private static String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        
        // حذف whitespace اضافی
        String normalized = name.replaceAll("\\s+", " ").trim();
        
        // حذف کاراکترهای خاص اضافی
        normalized = normalized.replaceAll("[\\u200C\\u200D]", ""); // Zero-width characters
        normalized = normalized.replaceAll("[\\u00A0]", " "); // Non-breaking space
        
        // تصحیح حروف عربی به فارسی (در صورت نیاز)
        normalized = normalized.replace("ك", "ک");
        normalized = normalized.replace("ي", "ی");
        normalized = normalized.replace("ة", "ه");
        
        // حذف کاراکترهای کنترل
        normalized = normalized.replaceAll("[\\p{Cntrl}]", "");
        
        return normalized.isBlank() ? null : normalized;
    }
    
    /**
     * استانداردسازی فرمت تاریخ به YYYY/MM/DD
     */
    private static String standardizeDate(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        
        // حذف whitespace
        String cleaned = date.trim();
        
        // تبدیل dash به slash
        cleaned = cleaned.replace("-", "/");
        
        // بررسی فرمت
        if (!cleaned.matches("\\d{4}/\\d{1,2}/\\d{1,2}")) {
            return date; // اگر فرمت درست نیست، همان را برگردان
        }
        
        // Split و pad کردن ماه و روز
        String[] parts = cleaned.split("/");
        if (parts.length != 3) {
            return date;
        }
        
        try {
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            int day = Integer.parseInt(parts[2]);
            
            // Format به YYYY/MM/DD
            return String.format("%04d/%02d/%02d", year, month, day);
        } catch (NumberFormatException e) {
            return date;
        }
    }
    
    /**
     * استانداردسازی فرمت مبلغ (حذف comma، تبدیل به عدد)
     */
    private static String standardizeAmount(String amount) {
        if (amount == null || amount.isBlank()) {
            return null;
        }
        
        // حذف comma و کاراکترهای غیرعددی (به جز نقطه)
        String cleaned = amount.replaceAll("[^\\d.]", "");
        
        // حذف نقطه‌های اضافی
        if (cleaned.contains(".")) {
            int firstDot = cleaned.indexOf('.');
            String beforeDot = cleaned.substring(0, firstDot);
            String afterDot = cleaned.substring(firstDot + 1).replaceAll("\\.", "");
            cleaned = beforeDot + "." + afterDot;
        }
        
        return cleaned.isBlank() ? null : cleaned;
    }

    private static Optional<String> normalizeDate(String s) {
        if (s == null || s.isBlank()) return Optional.empty();
        String t = s.replace('_', '-').replace('/', '-');
        try {
            LocalDate d = LocalDate.parse(t, DATE_FMT);
            return Optional.of(d.toString()); // yyyy-MM-dd
        } catch (DateTimeParseException ignored) {
            return Optional.empty();
        }
    }

    /**
     * Detect pattern number from pattern name (e.g., "فروش" -> "1")
     */
    private static String detectPatternFromName(String text) {
        // Look for "الگوی صورتحساب : فروش" pattern
        Pattern patternNamePattern = Pattern.compile(
            "(?:الگوی\\s*صورتحساب|الگو)\\s*[:\\-]?\\s*([^\\n]{1,50}?)(?:\\s*[\\r\\n]|\\s*تاریخ|\\s*شماره|$)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );
        
        Matcher matcher = patternNamePattern.matcher(text);
        if (matcher.find()) {
            String patternName = matcher.group(1).trim();
            // Check if it matches any known pattern name
            for (Map.Entry<String, String> entry : PATTERN_NAME_TO_NUMBER.entrySet()) {
                if (patternName.contains(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        
        return "";
    }
    
    private static String detectPattern(String text, String patternNumber) {
        // If pattern number is explicitly found, return it
        if (patternNumber != null && !patternNumber.isEmpty()) {
            try {
                int pattern = Integer.parseInt(patternNumber);
                if (pattern >= 1 && pattern <= 13) {
                    return patternNumber;
                }
            } catch (NumberFormatException ignored) {
                // Continue with heuristic detection
            }
        }
        
        String t = text == null ? "" : text;
        String lowerText = t.toLowerCase();
        
        // Pattern 2: Currency Sales - look for currency indicators
        if (t.contains("فروش ارز") || lowerText.contains("currency sales") || 
            findFirst(t, CUT_PATTERNS) != null || findFirst(t, CFEE_PATTERNS) != null) {
            return "2";
        }
        
        // Pattern 3: Gold/Jewelry - look for gold indicators
        if (t.contains("طلا") || t.contains("جواهر") || t.contains("عیار") || 
            lowerText.contains("gold") || lowerText.contains("jewelry")) {
            return "3";
        }
        
        // Pattern 4: Contract - look for contract indicators
        if (t.contains("قرارداد") || lowerText.contains("contract") || 
            Pattern.compile("(?:شماره\\s*قرارداد|CRN|crn)\\s*[:\\-]?\\s*([A-Za-z0-9\\-]+)", Pattern.CASE_INSENSITIVE).matcher(t).find()) {
            return "4";
        }
        
        // Pattern 5: Utility Bill - look for bill indicators
        if (t.contains("قبض") || lowerText.contains("bill") || 
            Pattern.compile("(?:شماره\\s*قبض|BILLID|billid)\\s*[:\\-]?\\s*([0-9]+)", Pattern.CASE_INSENSITIVE).matcher(t).find()) {
            return "5";
        }
        
        // Default to pattern 1 (Standard Sales)
        if (t.contains("صورتحساب الکترونیکی") || lowerText.contains("electronic invoice") ||
            t.contains("الگوی صورتحساب") && t.contains("فروش")) {
            return "1";
        }
        
        return patternNumber != null && !patternNumber.isEmpty() ? patternNumber : "1";
    }
}


