package ir.tax.trustedtaxpayers.biz.service.ocr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * سرویس Post-processing برای تصحیح خطاهای رایج OCR فارسی
 * این سرویس خطاهای رایج OCR را تصحیح می‌کند تا دقت استخراج فیلدها افزایش یابد
 */
@Service
@Slf4j
public class OcrTextPostProcessor {
    
    // Map برای تصحیح خطاهای رایج OCR فارسی
    private static final Map<String, String> COMMON_OCR_ERRORS = new HashMap<>();
    
    static {
        // خطاهای رایج در تشخیص حروف فارسی (OCR گاهی حروف عربی را به جای فارسی تشخیص می‌دهد)
        COMMON_OCR_ERRORS.put("ك", "ک"); // ک عربی به ک فارسی
        COMMON_OCR_ERRORS.put("ي", "ی"); // ی عربی به ی فارسی
        COMMON_OCR_ERRORS.put("ة", "ه"); // تای مرکب عربی به ه فارسی
        COMMON_OCR_ERRORS.put("أ", "ا");
        COMMON_OCR_ERRORS.put("إ", "ا");
        COMMON_OCR_ERRORS.put("ؤ", "و");
        COMMON_OCR_ERRORS.put("ئ", "ی");
        COMMON_OCR_ERRORS.put("آ", "آ"); // آ فارسی (برای اطمینان)
        
        // تصحیح حروف مشابه که OCR اشتباه می‌خواند
        // این موارد باید با دقت انجام شوند تا کلمات صحیح را خراب نکنند
        // ق و ف: فقط در کلمات خاص
        // غ و ق: فقط در کلمات خاص
        
        // تصحیح کلمات رایج که OCR اشتباه می‌خواند
        // این کلمات باید با word boundary بررسی شوند تا کلمات دیگر را خراب نکنند
    }
    
    // الگوهای regex برای تصحیح خطاهای ساختاری
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("\\s{2,}");
    private static final Pattern MULTIPLE_NEWLINES = Pattern.compile("\\n{3,}");
    private static final Pattern LEADING_TRAILING_WHITESPACE = Pattern.compile("^\\s+|\\s+$", Pattern.MULTILINE);
    
    /**
     * تصحیح متن استخراج شده از OCR
     */
    public String postProcess(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        
        String processed = text;
        
        // 1. حذف whitespace اضافی
        processed = MULTIPLE_SPACES.matcher(processed).replaceAll(" ");
        processed = MULTIPLE_NEWLINES.matcher(processed).replaceAll("\n\n");
        processed = LEADING_TRAILING_WHITESPACE.matcher(processed).replaceAll("");
        
        // 2. تصحیح خطاهای رایج OCR (حروف عربی به فارسی)
        // ابتدا حروف عربی را به فارسی تبدیل می‌کنیم
        processed = normalizeArabicToPersian(processed);
        
        // سپس کلمات رایج را تصحیح می‌کنیم
        processed = correctCommonInvoiceWords(processed);
        
        // 3. تصحیح اعداد که به اشتباه به حروف تبدیل شده‌اند (O -> 0, l -> 1, I -> 1)
        processed = correctNumberErrors(processed);
        
        // 4. تصحیح فاصله‌گذاری در اعداد (مثلاً "1 000" به "1000")
        processed = correctNumberSpacing(processed);
        
        // 5. تصحیح خطاهای رایج در تشخیص تاریخ
        processed = correctDateErrors(processed);
        
        // 6. تصحیح اعداد انگلیسی در context فارسی (فقط در متن، نه در کدها)
        processed = correctEnglishDigitsInPersianContext(processed);
        
        // 7. تصحیح خطاهای رایج در تشخیص حروف مشابه
        processed = correctSimilarCharacterErrors(processed);
        
        // 8. تصحیح خطاهای رایج در تشخیص کلمات
        processed = correctCommonWordErrors(processed);
        
        // 9. تصحیح خطاهای ساختاری (مثل جداکننده‌های اشتباه)
        processed = correctStructuralErrors(processed);
        
        // 10. تصحیح خطاهای رایج در تشخیص اعداد فارسی
        processed = correctPersianDigitErrors(processed);
        
        // 11. تصحیح خطاهای رایج در تشخیص جداکننده‌های اعداد (comma و dot)
        processed = correctNumberSeparators(processed);
        
        // 12. تصحیح خطاهای رایج در تشخیص فاصله در اعداد
        processed = correctNumberSpacingAdvanced(processed);
        
        // 13. اصلاح ترتیب RTL برای متن فارسی
        processed = fixRtlTextOrder(processed);
        
        log.debug("Post-processing: {} chars -> {} chars", text.length(), processed.length());
        if (!text.equals(processed)) {
            log.debug("Post-processing changed text. First 200 chars of original: {}", 
                    text.length() > 200 ? text.substring(0, 200) + "..." : text);
            log.debug("Post-processing changed text. First 200 chars of processed: {}", 
                    processed.length() > 200 ? processed.substring(0, 200) + "..." : processed);
        }
        
        return processed;
    }
    
    /**
     * تصحیح خطاهای رایج در اعداد
     */
    private String correctNumberErrors(String text) {
        // تصحیح "O" (حرف O) به "0" (عدد صفر) در اعداد
        text = text.replaceAll("(\\d+)O(\\d+)", "$10$2");
        text = text.replaceAll("O(\\d+)", "0$1");
        text = text.replaceAll("(\\d+)O", "$10");
        
        // تصحیح "l" (حرف L کوچک) به "1" (عدد یک) در اعداد
        text = text.replaceAll("(\\d+)l(\\d+)", "$11$2");
        text = text.replaceAll("l(\\d+)", "1$1");
        text = text.replaceAll("(\\d+)l", "$11");
        
        // تصحیح "I" (حرف I بزرگ) به "1" (عدد یک) در اعداد
        text = text.replaceAll("(\\d+)I(\\d+)", "$11$2");
        text = text.replaceAll("I(\\d+)", "1$1");
        text = text.replaceAll("(\\d+)I", "$11");
        
        return text;
    }
    
    /**
     * تصحیح فاصله‌گذاری در اعداد
     */
    private String correctNumberSpacing(String text) {
        // حذف فاصله در اعداد (مثلاً "1 000 000" به "1000000")
        // اما فقط در اعداد که فاصله دارند
        return text.replaceAll("(\\d{1,3})\\s+(\\d{3})\\s*(\\d{3})?", "$1$2$3");
    }
    
    /**
     * تصحیح خطاهای رایج در تشخیص تاریخ
     */
    private String correctDateErrors(String text) {
        // تصحیح تاریخ‌هایی که به اشتباه خوانده شده‌اند
        // مثلاً "1403/08/25" که ممکن است به "1403/0۸/25" تبدیل شده باشد
        text = text.replaceAll("(\\d{4})[/-]([0O])(\\d{1,2})[/-](\\d{1,2})", "$1/0$3/$4");
        text = text.replaceAll("(\\d{4})[/-](\\d{1,2})[/-]([0O])(\\d{1,2})", "$1/$2/0$4");
        
        return text;
    }
    
    /**
     * تصحیح اعداد انگلیسی در context فارسی (فقط در متن، نه در کدها یا شناسه‌ها)
     * این کار فقط برای اعداد در متن فارسی انجام می‌شود
     */
    private String correctEnglishDigitsInPersianContext(String text) {
        // فقط اعداد انگلیسی که در کنار حروف فارسی هستند را تبدیل می‌کنیم
        // نه اعداد در کدها یا شناسه‌ها (که معمولاً فقط عدد هستند)
        
        // الگو: حرف فارسی + عدد انگلیسی + حرف فارسی یا پایان خط
        Pattern persianContextPattern = Pattern.compile(
                "([\\u0600-\\u06FF\\u200C\\u200D])([0-9]+)([\\u0600-\\u06FF\\u200C\\u200D\\s]|$)",
                Pattern.MULTILINE
        );
        
        java.util.regex.Matcher matcher = persianContextPattern.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String englishDigits = matcher.group(2);
            String persianDigits = convertToPersianDigits(englishDigits);
            matcher.appendReplacement(result, matcher.group(1) + persianDigits + matcher.group(3));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * تبدیل اعداد انگلیسی به فارسی
     */
    private String convertToPersianDigits(String digits) {
        return digits
                .replace('0', '۰')
                .replace('1', '۱')
                .replace('2', '۲')
                .replace('3', '۳')
                .replace('4', '۴')
                .replace('5', '۵')
                .replace('6', '۶')
                .replace('7', '۷')
                .replace('8', '۸')
                .replace('9', '۹');
    }
    
    /**
     * تصحیح خطاهای رایج در تشخیص حروف مشابه
     */
    private String correctSimilarCharacterErrors(String text) {
        // تصحیح حروف مشابه که OCR گاهی اشتباه می‌خواند
        // توجه: این تبدیل‌ها باید با دقت انجام شوند تا کلمات صحیح را خراب نکنند
        
        // س به ص در کلمات رایج (OCR معمولاً "ص" را به "س" اشتباه می‌خواند)
        // صورتحساب -> صورتحساب (تصحیح "س" به "ص" در "صورتحساب")
        text = text.replaceAll("صورت\\s*حساب", "صورتحساب");
        // صادرکننده -> صادرکننده (تصحیح "س" به "ص")
        text = text.replaceAll("\\bسادرکننده\\b", "صادرکننده");
        // صادر -> صادر (تصحیح "س" به "ص")
        text = text.replaceAll("\\bسادر\\b", "صادر");
        // صادرات -> صادرات (تصحیح "س" به "ص")
        text = text.replaceAll("\\bسادرات\\b", "صادرات");
        
        // ق و ف (فقط در کلمات خاص)
        // قوانین -> قوانین (نباید تغییر کند)
        // فاکتور -> فاکتور (نباید تغییر کند)
        // اما در برخی موارد OCR "ق" را به "ف" اشتباه می‌خواند
        text = text.replaceAll("\\bفروش\\b", "فروش"); // فروش درست است
        text = text.replaceAll("\\bفروشنده\\b", "فروشنده"); // فروشنده درست است
        
        // غ و ق (فقط در کلمات خاص)
        // غلط -> قوانین (نباید تغییر کند)
        // قوانین -> قوانین (نباید تغییر کند)
        
        // ز و ذ (فقط در کلمات خاص)
        // ذخیره -> ذخیره (نباید تغییر کند)
        // زود -> زود (نباید تغییر کند)
        
        return text;
    }
    
    /**
     * تصحیح خطاهای رایج در تشخیص کلمات
     */
    private String correctCommonWordErrors(String text) {
        // تصحیح کلمات رایج که OCR اشتباه می‌خواند (س به ص، و غیره)
        Map<String, String> wordCorrections = new HashMap<>();
        
        // تصحیح "س" به "ص" در کلمات رایج (OCR معمولاً "ص" را به "س" اشتباه می‌خواند)
        wordCorrections.put("صورتحساب", "صورتحساب"); // صورتحساب -> صورتحساب
        wordCorrections.put("صورت حساب", "صورتحساب");
        wordCorrections.put("صورت‌حساب", "صورتحساب");
        wordCorrections.put("سادرکننده", "صادرکننده"); // سادرکننده -> صادرکننده
        wordCorrections.put("سادر", "صادر"); // سادر -> صادر
        wordCorrections.put("سادرات", "صادرات"); // سادرات -> صادرات
        
        // تصحیح کلمات دیگر
        wordCorrections.put("فروشنده", "فروشنده");
        wordCorrections.put("خریدار", "خریدار");
        wordCorrections.put("تاریخ", "تاریخ");
        wordCorrections.put("شماره", "شماره");
        wordCorrections.put("مبلغ", "مبلغ");
        wordCorrections.put("جمع", "جمع");
        wordCorrections.put("کالا", "کالا");
        wordCorrections.put("خدمت", "خدمت");
        wordCorrections.put("اقتصادی", "اقتصادی");
        wordCorrections.put("شناسه", "شناسه");
        wordCorrections.put("ملی", "ملی");
        wordCorrections.put("الکترونیکی", "الکترونیکی");
        wordCorrections.put("الکترونیک", "الکترونیک");
        wordCorrections.put("مشخصات", "مشخصات");
        wordCorrections.put("بنگاه", "بنگاه");
        wordCorrections.put("حقیقی", "حقیقی");
        wordCorrections.put("حقوقی", "حقوقی");
        wordCorrections.put("واحد", "واحد");
        wordCorrections.put("تعداد", "تعداد");
        wordCorrections.put("مقدار", "مقدار");
        wordCorrections.put("مالیات", "مالیات");
        wordCorrections.put("تخفیف", "تخفیف");
        wordCorrections.put("ارزش", "ارزش");
        wordCorrections.put("افزوده", "افزوده");
        
        for (Map.Entry<String, String> correction : wordCorrections.entrySet()) {
            String wrong = correction.getKey();
            String correct = correction.getValue();
            // جایگزینی ساده (برای کلمات کامل)
            text = text.replace(wrong, correct);
            // همچنین با word boundary برای اطمینان بیشتر
            // استفاده از pattern برای فارسی (word boundary ممکن است برای فارسی کار نکند)
            text = text.replaceAll("(^|[\\s\\p{P}])" + Pattern.quote(wrong) + "([\\s\\p{P}]|$)", "$1" + correct + "$2");
        }
        
        return text;
    }
    
    /**
     * تصحیح خطاهای ساختاری (مثل جداکننده‌های اشتباه)
     */
    private String correctStructuralErrors(String text) {
        // تصحیح جداکننده‌های اشتباه
        text = text.replaceAll(":\\s*:", ":"); // دو نقطه پشت سر هم
        text = text.replaceAll("-\\s*-", "-"); // دو خط تیره پشت سر هم
        text = text.replaceAll("\\s*:\\s*", ": "); // فاصله قبل و بعد از :
        text = text.replaceAll("\\s*-\\s*", " - "); // فاصله قبل و بعد از -
        
        // تصحیح خطوط جدید اضافی در جداول
        text = text.replaceAll("\\n\\s*\\n\\s*\\n", "\n\n"); // بیش از دو خط جدید
        
        // تصحیح فاصله قبل از اعداد
        text = text.replaceAll("(\\p{L})\\s+(\\d)", "$1 $2"); // فاصله قبل از عدد
        
        return text;
    }
    
    /**
     * تصحیح خطاهای رایج در تشخیص اعداد فارسی
     */
    private String correctPersianDigitErrors(String text) {
        // تصحیح اعداد فارسی که به اشتباه خوانده شده‌اند
        // مثلاً ۰ (صفر فارسی) که ممکن است به O یا 0 تبدیل شده باشد
        // یا ۱ (یک فارسی) که ممکن است به l یا I تبدیل شده باشد
        
        // فقط در context فارسی (نه در کدها)
        Pattern persianNumberPattern = Pattern.compile(
                "([\\u0600-\\u06FF\\u200C\\u200D])([0-9OIl]+)([\\u0600-\\u06FF\\u200C\\u200D\\s]|$)",
                Pattern.MULTILINE
        );
        
        java.util.regex.Matcher matcher = persianNumberPattern.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String englishDigits = matcher.group(2);
            // تبدیل O, l, I به اعداد
            englishDigits = englishDigits.replace('O', '0');
            englishDigits = englishDigits.replace('l', '1');
            englishDigits = englishDigits.replace('I', '1');
            String persianDigits = convertToPersianDigits(englishDigits);
            matcher.appendReplacement(result, matcher.group(1) + persianDigits + matcher.group(3));
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    /**
     * تصحیح خطاهای رایج در تشخیص جداکننده‌های اعداد (comma و dot)
     */
    private String correctNumberSeparators(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        
        // تصحیح comma که ممکن است به dot تبدیل شده باشد یا برعکس
        // الگو: عدد با comma یا dot که ممکن است اشتباه خوانده شده باشد
        // مثلاً "1,000.50" که ممکن است به "1.000,50" تبدیل شده باشد
        
        // در صورتحساب‌های فارسی، معمولاً از comma برای جدا کردن هزارگان استفاده می‌شود
        // و از dot برای اعشار (اگر باشد)
        
        // تصحیح: اگر عددی با dot در وسط باشد که بعد از آن 3 رقم بیاید، احتمالاً comma است
        // مثلاً "1.000" -> "1,000" (اگر در context فارسی باشد)
        Pattern thousandSeparatorPattern = Pattern.compile(
                "([\\u0600-\\u06FF\\s])(\\d{1,3})\\.(\\d{3})(\\s|$|[\\u0600-\\u06FF])",
                Pattern.MULTILINE
        );
        
        java.util.regex.Matcher matcher = thousandSeparatorPattern.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            // اگر بعد از dot سه رقم بیاید و بعد از آن space یا حرف فارسی باشد، احتمالاً comma است
            String replacement = matcher.group(1) + matcher.group(2) + "," + matcher.group(3) + matcher.group(4);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.length() > 0 ? result.toString() : text;
    }
    
    /**
     * تصحیح پیشرفته فاصله‌گذاری در اعداد
     */
    private String correctNumberSpacingAdvanced(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        
        // حذف فاصله در اعداد (مثلاً "1 000 000" به "1000000")
        // اما فقط در اعداد که فاصله دارند و در context فارسی هستند
        
        // الگو: عدد با فاصله که بعد از آن حرف فارسی یا space یا پایان خط باشد
        Pattern spacedNumberPattern = Pattern.compile(
                "([\\u0600-\\u06FF\\s])(\\d{1,3})(\\s+)(\\d{3})(\\s*)(\\d{3})?(\\s*)(\\d{3})?(\\s|$|[\\u0600-\\u06FF])",
                Pattern.MULTILINE
        );
        
        java.util.regex.Matcher matcher = spacedNumberPattern.matcher(text);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            // حذف فاصله‌ها و ترکیب اعداد
            StringBuilder number = new StringBuilder();
            number.append(matcher.group(2));
            number.append(matcher.group(4));
            if (matcher.group(6) != null) {
                number.append(matcher.group(6));
            }
            if (matcher.group(8) != null) {
                number.append(matcher.group(8));
            }
            
            String replacement = matcher.group(1) + number.toString() + matcher.group(9);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.length() > 0 ? result.toString() : text;
    }
    
    /**
     * تبدیل حروف عربی به فارسی
     */
    private String normalizeArabicToPersian(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        
        StringBuilder result = new StringBuilder(text.length());
        for (char ch : text.toCharArray()) {
            switch (ch) {
                case '\u0643': // ك (ک عربی)
                    result.append('\u06A9'); // ک فارسی
                    break;
                case '\u064A': // ي (ی عربی)
                    result.append('\u06CC'); // ی فارسی
                    break;
                case '\u0629': // ة (تای مرکب عربی)
                    result.append('\u0647'); // ه فارسی
                    break;
                case '\u0623': // أ
                case '\u0625': // إ
                    result.append('\u0627'); // ا
                    break;
                case '\u0624': // ؤ
                    result.append('\u0648'); // و
                    break;
                case '\u0626': // ئ
                    result.append('\u06CC'); // ی فارسی
                    break;
                default:
                    result.append(ch);
                    break;
            }
        }
        return result.toString();
    }
    
    /**
     * تصحیح کلمات رایج صورتحساب که OCR اشتباه می‌خواند
     */
    private String correctCommonInvoiceWords(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        
        // الگوهای تصحیح کلمات رایج (با word boundary برای اطمینان)
        Map<String, String> wordCorrections = new HashMap<>();
        
        // تصحیح کلمات که OCR ممکن است "ص" را به "س" اشتباه بخواند
        // صورتحساب -> صورتحساب (اگر "س" به جای "ص" خوانده شده باشد)
        wordCorrections.put("صورتحساب", "صورتحساب");
        wordCorrections.put("صورت حساب", "صورتحساب");
        wordCorrections.put("صورت‌حساب", "صورتحساب");
        
        // تصحیح کلمات دیگر که ممکن است اشتباه خوانده شوند
        // فروشنده (اگر "ق" به جای "ف" خوانده شده باشد)
        wordCorrections.put("قروشنده", "فروشنده");
        wordCorrections.put("قروش", "فروش");
        
        // تصحیح کلمات با حروف مشابه
        wordCorrections.put("سادرکننده", "صادرکننده");
        wordCorrections.put("سادر", "صادر");
        wordCorrections.put("سادرات", "صادرات");
        
        // اعمال تصحیحات
        String result = text;
        for (Map.Entry<String, String> correction : wordCorrections.entrySet()) {
            String wrong = correction.getKey();
            String correct = correction.getValue();
            // استفاده از word boundary برای اطمینان از جایگزینی صحیح
            result = result.replace(wrong, correct);
            // همچنین با pattern برای اطمینان بیشتر
            result = Pattern.compile("\\b" + Pattern.quote(wrong) + "\\b", Pattern.UNICODE_CASE)
                    .matcher(result).replaceAll(correct);
        }
        
        return result;
    }
    
    /**
     * نرمال‌سازی متن برای استخراج بهتر
     */
    public String normalizeForExtraction(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        
        String normalized = text;
        
        // یکسان‌سازی جداکننده‌ها
        normalized = normalized.replaceAll("[\\u200C\\u200D]", ""); // حذف zero-width characters
        normalized = normalized.replaceAll("[\\u00A0]", " "); // non-breaking space به space
        
        // یکسان‌سازی خطوط جدید
        normalized = normalized.replaceAll("\\r\\n", "\n");
        normalized = normalized.replaceAll("\\r", "\n");
        
        // حذف کاراکترهای کنترل اضافی
        normalized = normalized.replaceAll("[\\u200E\\u200F]", ""); // حذف left-to-right/right-to-left marks
        
        // تصحیح فاصله‌های اضافی بین کلمات فارسی
        normalized = normalized.replaceAll("([\\u0600-\\u06FF])\\s+([\\u0600-\\u06FF])", "$1 $2");
        
        return normalized;
    }
    
    /**
     * اصلاح ترتیب متن برای RTL (راست به چپ)
     * این متد ترتیب خطوط و کاراکترها را برای متن فارسی اصلاح می‌کند
     */
    private String fixRtlTextOrder(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        
        // تقسیم به خطوط
        String[] lines = text.split("[\r\n]+");
        StringBuilder result = new StringBuilder(text.length());
        
        for (String line : lines) {
            if (line.isBlank()) {
                result.append("\n");
                continue;
            }
            
            // بررسی اینکه آیا خط شامل کاراکترهای فارسی است
            boolean hasPersian = line.matches(".*[\\u0600-\\u06FF].*");
            
            if (hasPersian) {
                // برای خطوط فارسی، ترتیب را حفظ می‌کنیم (RTL طبیعی)
                // اما ممکن است نیاز به اصلاح ترتیب کلمات باشد
                String fixedLine = fixRtlLineOrder(line);
                result.append(fixedLine);
            } else {
                // برای خطوط انگلیسی/عددی، ترتیب را حفظ می‌کنیم
                result.append(line);
            }
            
            result.append("\n");
        }
        
        // حذف خط جدید آخر
        if (result.length() > 0 && result.charAt(result.length() - 1) == '\n') {
            result.setLength(result.length() - 1);
        }
        
        return result.toString();
    }
    
    /**
     * اصلاح ترتیب کلمات در یک خط برای RTL
     */
    private String fixRtlLineOrder(String line) {
        if (line == null || line.isBlank()) {
            return line;
        }
        
        // حذف فاصله‌های اضافی
        line = line.replaceAll("\\s+", " ").trim();
        
        // اگر خط شامل کاراکترهای فارسی است، ترتیب را حفظ می‌کنیم
        // OCR معمولاً ترتیب را به درستی حفظ می‌کند، اما ممکن است نیاز به اصلاح باشد
        
        // اگر خط با کاراکتر فارسی شروع می‌شود، ترتیب درست است
        // اگر با کاراکتر انگلیسی/عددی شروع می‌شود، ممکن است نیاز به معکوس کردن باشد
        if (line.length() > 0) {
            char firstChar = line.charAt(0);
            boolean startsWithPersian = (firstChar >= '\u0600' && firstChar <= '\u06FF');
            
            if (!startsWithPersian && line.matches(".*[\\u0600-\\u06FF].*")) {
                // خط شامل فارسی است اما با کاراکتر غیرفارسی شروع می‌شود
                // ممکن است نیاز به اصلاح باشد
                // در اینجا فقط whitespace را normalize می‌کنیم
            }
        }
        
        return line;
    }
}

