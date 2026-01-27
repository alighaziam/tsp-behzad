package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.application.cache.Cacheable;
import ir.tax.trustedtaxpayers.application.dto.FormFieldDto;
import ir.tax.trustedtaxpayers.application.dto.FormSchemaDto;
import ir.tax.trustedtaxpayers.application.service.form.InvoicePatternFieldProvider;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * سرویس برای تولید فرم‌های پویا بر اساس 16 الگوی صورتحساب
 * این سرویس از Pattern Provider ها استفاده می‌کند که باعث می‌شود:
 * - اگر الگو جدید اضافه شود، فقط یک Provider جدید اضافه شود
 * - هر الگو فیلدهای خاص خودش را داشته باشد
 * - کد تمیز و قابل نگهداری باشد
 */
@Slf4j
@Service
public class InvoiceFormBuilderService {

    private final List<InvoicePatternFieldProvider> patternProviders;
    private Map<Integer, InvoicePatternFieldProvider> providerMap = new HashMap<>();

    public InvoiceFormBuilderService(List<InvoicePatternFieldProvider> patternProviders) {
        this.patternProviders = patternProviders;
    }

    @PostConstruct
    public void init() {
        // ایجاد Map از pattern number به provider
        providerMap = patternProviders.stream()
                .collect(Collectors.toMap(
                        InvoicePatternFieldProvider::getSupportedPattern,
                        provider -> provider,
                        (existing, replacement) -> {
                            log.warn("Duplicate provider for pattern {}: {} and {}", 
                                    existing.getSupportedPattern(), existing.getClass().getSimpleName(), 
                                    replacement.getClass().getSimpleName());
                            return existing; // Keep the first one
                        }
                ));
        
        log.info("Initialized {} invoice pattern providers", providerMap.size());
    }

    /**
     * تولید Form Schema برای یک الگوی خاص
     * 
     * @param patternNumber شماره الگو (1-16)
     * @return FormSchemaDto شامل فیلدهای مورد نیاز آن الگو
     */
    @Cacheable(cacheName = "invoiceFormSchemas", keyPrefix = "patternNumber")
    public FormSchemaDto generateInvoiceFormSchema(Integer patternNumber) {
        if (patternNumber == null || patternNumber < 1) {
            throw new IllegalArgumentException("Pattern number must be a positive integer");
        }

        InvoicePatternFieldProvider provider = providerMap.get(patternNumber);
        if (provider == null) {
            log.warn("No provider found for pattern {}, using default fields", patternNumber);
            // Fallback: استفاده از فیلدهای پایه
            return createDefaultSchema(patternNumber);
        }

        List<FormFieldDto> fields = new ArrayList<>();
        
        // فیلدهای مشترک همه الگوها
        fields.addAll(getCommonHeaderFields());
        
        // فیلدهای اختصاصی Header این الگو
        fields.addAll(provider.getHeaderSpecificFields(100));
        
        // فیلدهای Body مشترک
        fields.addAll(getCommonBodyFields());
        
        // فیلدهای اختصاصی Body این الگو
        fields.addAll(provider.getBodySpecificFields(200));
        
        // مرتب‌سازی بر اساس order
        fields.sort(Comparator.comparingInt(f -> f.getOrder() != null ? f.getOrder() : Integer.MAX_VALUE));
        
        return FormSchemaDto.builder()
                .entityName("InvoicePattern" + patternNumber)
                .title("فرم صورتحساب الگوی " + patternNumber + " - " + provider.getPatternName())
                .description("فرم ایجاد صورتحساب الکترونیکی الگوی " + patternNumber + ": " + provider.getPatternName())
                .fields(fields)
                .build();
    }

    /**
     * ایجاد Schema پیش‌فرض در صورت نبود Provider
     */
    private FormSchemaDto createDefaultSchema(Integer patternNumber) {
        List<FormFieldDto> fields = new ArrayList<>();
        fields.addAll(getCommonHeaderFields());
        fields.addAll(getCommonBodyFields());
        
        return FormSchemaDto.builder()
                .entityName("InvoicePattern" + patternNumber)
                .title("فرم صورتحساب الگوی " + patternNumber)
                .description("فرم ایجاد صورتحساب الکترونیکی الگوی " + patternNumber)
                .fields(fields)
                .build();
    }

    /**
     * فیلدهای مشترک همه الگوها (Header)
     */
    private List<FormFieldDto> getCommonHeaderFields() {
        List<FormFieldDto> fields = new ArrayList<>();
        int order = 0;

        // گروه: اطلاعات پایه صورتحساب
        fields.add(createField("inno", "text", "شماره صورتحساب", "شماره صورتحساب", 
                true, order++, 10, null, "لطفا شماره صورتحساب را وارد کنید"));
        
        fields.add(createField("indatim", "datetime", "تاریخ صورتحساب", "تاریخ صورتحساب", 
                true, order++, null, null, "لطفا تاریخ صورتحساب را وارد کنید"));

        fields.add(createField("m2Indati", "datetime", "تاریخ و زمان تبدیل‌شده به میلادی", "تاریخ و زمان میلادی", 
                false, order++, null, null, "در صورت نیاز تاریخ میلادی را وارد کنید"));

        fields.add(createField("inty", "combobox", "نوع صورتحساب", "نوع صورتحساب", 
                true, order++, null, getInvoiceTypeOptions(), "نوع صورتحساب را انتخاب کنید"));

        fields.add(createField("ins", "combobox", "موضوع صورتحساب", "موضوع صورتحساب", 
                true, order++, null, getInvoiceSubjectOptions(), "موضوع صورتحساب را انتخاب کنید"));

        fields.add(createField("inp", "number", "الگوی صورتحساب", "الگوی صورتحساب", 
                true, order++, null, null, "لطفا الگوی صورتحساب را وارد کنید"));

        // گروه: مراجع صورتحساب
        fields.add(createField("irtaxid", "text", "شناسه یکتای صورتحساب مرجع", "شناسه صورتحساب مرجع", 
                false, order++, 22, null, "در صورت وجود شناسه صورتحساب مرجع را وارد کنید"));

        // گروه: اطلاعات فروشنده
        fields.add(createField("tins", "text", "شناسه ملی/اقتصادی فروشنده", "شناسه ملی/اقتصادی فروشنده", 
                true, order++, 14, null, "لطفا شناسه ملی/اقتصادی فروشنده را وارد کنید"));

        fields.add(createField("sbc", "text", "کد شعبه فروشنده", "کد شعبه فروشنده", 
                false, order++, 4, null, "لطفا کد شعبه فروشنده را وارد کنید"));

        // گروه: اطلاعات خریدار
        fields.add(createField("tob", "combobox", "نوع خریدار", "نوع خریدار", 
                false, order++, null, getPersonTypeOptions(), "نوع خریدار را انتخاب کنید"));

        fields.add(createField("bid", "text", "شناسه ملی/کد فراگیر خریدار", "شناسه ملی/کد فراگیر خریدار", 
                false, order++, 12, null, "لطفا شناسه ملی/کد فراگیر خریدار را وارد کنید"));

        fields.add(createField("tinb", "text", "شناسه ملی/اقتصادی خریدار", "شناسه ملی/اقتصادی خریدار", 
                false, order++, 14, null, "لطفا شناسه ملی/اقتصادی خریدار را وارد کنید"));

        fields.add(createField("bbc", "text", "کد شعبه خریدار", "کد شعبه خریدار", 
                false, order++, 4, null, "لطفا کد شعبه خریدار را وارد کنید"));

        fields.add(createField("bpc", "text", "کد پستی خریدار", "کد پستی خریدار", 
                false, order++, 10, null, "لطفا کد پستی خریدار را وارد کنید"));

        fields.add(createField("bpn", "text", "شماره پاسپورت خریدار", "شماره پاسپورت خریدار", 
                false, order++, 9, null, "لطفا شماره پاسپورت خریدار را وارد کنید"));

        // گروه: مبالغ
        fields.add(createField("tprdis", "number", "جمع مبلغ قبل از تخفیف", "جمع مبلغ قبل از تخفیف", 
                true, order++, null, null, "لطفا جمع مبلغ قبل از تخفیف را وارد کنید"));

        fields.add(createField("tdis", "number", "جمع تخفیف", "جمع تخفیف", 
                false, order++, null, null, "لطفا جمع تخفیف را وارد کنید"));

        fields.add(createField("tadis", "number", "جمع مبلغ بعد از تخفیف", "جمع مبلغ بعد از تخفیف", 
                true, order++, null, null, "لطفا جمع مبلغ بعد از تخفیف را وارد کنید"));

        fields.add(createField("tvam", "number", "جمع مالیات بر ارزش افزوده", "جمع مالیات بر ارزش افزوده", 
                false, order++, null, null, "لطفا جمع مالیات بر ارزش افزوده را وارد کنید"));

        fields.add(createField("tbill", "number", "جمع صورتحساب", "جمع صورتحساب", 
                true, order++, null, null, "لطفا جمع صورتحساب را وارد کنید"));

        fields.add(createField("todam", "number", "جمع سایر عوارض", "جمع سایر عوارض", 
                false, order++, null, null, "در صورت وجود جمع سایر عوارض را وارد کنید"));

        fields.add(createField("tax17", "number", "مالیات ماده 17", "مالیات ماده 17", 
                false, order++, null, null, "در صورت وجود مبلغ ماده 17 را وارد کنید"));

        // گروه: پرداخت
        fields.add(createField("setm", "combobox", "نوع تسویه", "نوع تسویه", 
                false, order++, null, getSettlementTypeOptions(), "نوع تسویه را انتخاب کنید"));

        fields.add(createField("cap", "number", "مبلغ نقدی", "مبلغ نقدی", 
                false, order++, null, null, "لطفا مبلغ نقدی را وارد کنید"));

        fields.add(createField("insp", "number", "مبلغ اقساط", "مبلغ اقساط", 
                false, order++, null, null, "لطفا مبلغ اقساط را وارد کنید"));

        fields.add(createField("tvop", "number", "جمع مالیات پرداختی", "جمع مالیات پرداختی", 
                false, order++, null, null, "لطفا جمع مالیات پرداختی را وارد کنید"));

        // گروه: اطلاعات تکمیلی
        fields.add(createField("inInsuranceId", "text", "شناسه بیمه", "شناسه بیمه", 
                false, order++, 50, null, "در صورت وجود شناسه بیمه را وارد کنید"));

        fields.add(createField("an", "text", "شناسه یکتای بیمه‌نامه", "شناسه بیمه‌نامه", 
                false, order++, 50, null, "شناسه بیمه‌نامه را وارد کنید"));

        fields.add(createField("asn", "text", "شماره اعلامیه فروش", "شماره اعلامیه فروش", 
                false, order++, 50, null, "شماره اعلامیه فروش را وارد کنید"));

        fields.add(createField("asd", "datetime", "تاریخ اعلامیه فروش", "تاریخ اعلامیه فروش", 
                false, order++, null, null, "تاریخ اعلامیه فروش را وارد کنید"));

        return fields;
    }

    /**
     * فیلدهای مشترک Body (اقلام)
     */
    private List<FormFieldDto> getCommonBodyFields() {
        List<FormFieldDto> fields = new ArrayList<>();
        int baseOrder = 200;

        fields.add(createField("body.sstid", "text", "کد کالا/خدمت", "کد کالا/خدمت", 
                true, baseOrder++, 13, null, "لطفا کد کالا/خدمت را وارد کنید"));

        fields.add(createField("body.sstt", "text", "عنوان کالا/خدمت", "عنوان کالا/خدمت", 
                true, baseOrder++, 400, null, "لطفا عنوان کالا/خدمت را وارد کنید"));

        fields.add(createField("body.mu", "text", "واحد اندازه‌گیری", "واحد اندازه‌گیری", 
                true, baseOrder++, 8, null, "لطفا واحد اندازه‌گیری را وارد کنید"));

        fields.add(createField("body.am", "number", "مقدار", "مقدار", 
                true, baseOrder++, null, null, "لطفا مقدار را وارد کنید"));

        fields.add(createField("body.fee", "number", "فی واحد", "فی واحد", 
                true, baseOrder++, null, null, "لطفا فی واحد را وارد کنید"));

        fields.add(createField("body.prdis", "number", "مبلغ قبل از تخفیف", "مبلغ قبل از تخفیف", 
                true, baseOrder++, null, null, "لطفا مبلغ قبل از تخفیف را وارد کنید"));

        fields.add(createField("body.dis", "number", "تخفیف", "تخفیف", 
                false, baseOrder++, null, null, "لطفا تخفیف را وارد کنید"));

        fields.add(createField("body.adis", "number", "مبلغ بعد از تخفیف", "مبلغ بعد از تخفیف", 
                true, baseOrder++, null, null, "لطفا مبلغ بعد از تخفیف را وارد کنید"));

        fields.add(createField("body.vra", "number", "نرخ مالیات", "نرخ مالیات", 
                false, baseOrder++, null, null, "لطفا نرخ مالیات را وارد کنید"));

        fields.add(createField("body.vam", "number", "مبلغ مالیات", "مبلغ مالیات", 
                false, baseOrder++, null, null, "لطفا مبلغ مالیات را وارد کنید"));

        fields.add(createField("body.vop", "number", "مالیات پرداختی قلم", "مالیات پرداختی قلم", 
                false, baseOrder++, null, null, "در صورت وجود مالیات پرداختی قلم را وارد کنید"));

        fields.add(createField("body.odt", "text", "عنوان عوارض", "عنوان عوارض", 
                false, baseOrder++, 400, null, "عنوان عوارض را وارد کنید"));

        fields.add(createField("body.odr", "number", "نرخ عوارض", "نرخ عوارض", 
                false, baseOrder++, null, null, "نرخ عوارض را وارد کنید"));

        fields.add(createField("body.odam", "number", "مبلغ عوارض", "مبلغ عوارض", 
                false, baseOrder++, null, null, "مبلغ عوارض را وارد کنید"));

        fields.add(createField("body.olt", "text", "عنوان سایر حقوق قانونی", "عنوان سایر حقوق قانونی", 
                false, baseOrder++, 400, null, "عنوان سایر حقوق قانونی را وارد کنید"));

        fields.add(createField("body.olr", "number", "نرخ سایر حقوق قانونی", "نرخ سایر حقوق قانونی", 
                false, baseOrder++, null, null, "نرخ سایر حقوق قانونی را وارد کنید"));

        fields.add(createField("body.olam", "number", "مبلغ سایر حقوق قانونی", "مبلغ سایر حقوق قانونی", 
                false, baseOrder++, null, null, "مبلغ سایر حقوق قانونی را وارد کنید"));

        fields.add(createField("body.cop", "number", "مبلغ نقدی پرداختی", "مبلغ نقدی پرداختی", 
                false, baseOrder++, null, null, "مبلغ نقدی پرداختی را وارد کنید"));

        fields.add(createField("body.tsstam", "number", "جمع مبلغ کالا/خدمت", "جمع مبلغ کالا/خدمت", 
                false, baseOrder++, null, null, "جمع مبلغ کالا/خدمت را وارد کنید"));

        fields.add(createField("body.bsrn", "text", "شناسه یکتای پرداخت قلم", "شناسه یکتای پرداخت قلم", 
                false, baseOrder++, 50, null, "شناسه یکتای پرداخت قلم را وارد کنید"));

        fields.add(createField("body.medid", "text", "شناسه کالای پزشکی", "شناسه کالای پزشکی", 
                false, baseOrder++, 50, null, "شناسه کالای پزشکی را وارد کنید"));

        fields.add(createField("body.aspr", "number", "قیمت فروش دارو", "قیمت فروش دارو", 
                false, baseOrder++, null, null, "قیمت فروش دارو را وارد کنید"));

        return fields;
    }

    /**
     * Helper method برای ایجاد FormFieldDto
     */
    private FormFieldDto createField(String key, String type, String name, String label, 
                                     boolean required, int order, Integer maxLength, 
                                     List<Map<String, Object>> options, String placeholder) {
        FormFieldDto.FormFieldDtoBuilder builder = FormFieldDto.builder()
                .key(key)
                .type(type)
                .name(getEnglishNameForKey(key, name))
                .label(label)
                .required(required)
                .mandatory(required)
                .order(order)
                .disabled(false)
                .readonly(false)
                .placeholder(placeholder);

        if (maxLength != null) {
            builder.maxLength(maxLength);
        }

        if (options != null) {
            builder.options(options);
        }

        // Inferred validation based on key conventions (aligned with RC_IITP_IS_V7_6)
        String k = key.toLowerCase();
        try {
            if (k.equals("tins") || k.equals("tinb")) { // شناسه ملی/اقتصادی
                builder.minLength(10);
                builder.maxLength(14);
                builder.pattern("^\\d{10,14}$");
            } else if (k.equals("bid")) { // شناسه ملی/کد فراگیر خریدار
                builder.minLength(10);
                builder.maxLength(12);
                builder.pattern("^\\d{10,12}$");
            } else if (k.equals("bpc")) { // کد پستی
                builder.minLength(10);
                builder.maxLength(10);
                builder.pattern("^\\d{10}$");
            } else if (k.equals("sbc") || k.equals("bbc")) { // کد شعبه
                builder.minLength(4);
                builder.maxLength(4);
                builder.pattern("^\\d{4}$");
            } else if (k.equals("bpn")) { // شماره پاسپورت
                builder.minLength(6);
                builder.maxLength(9);
                builder.pattern("^[A-Za-z0-9]{6,9}$");
            } else if (k.equals("inno")) { // شماره صورتحساب
                builder.minLength(1);
                builder.maxLength(10);
                builder.pattern("^[A-Za-z0-9-]{1,10}$");
            } else if (k.equals("irtaxid")) { // شناسه یکتای صورتحساب مرجع
                builder.minLength(22);
                builder.maxLength(22);
                builder.pattern("^[A-Za-z0-9]{22}$");
            } else if (k.equals("body.cut")) { // نوع ارز - ISO 4217
                builder.minLength(3);
                builder.maxLength(3);
                builder.pattern("^[A-Z]{3}$");
            } else if (k.equals("body.hs")) { // کد HS
                builder.minLength(6);
                builder.maxLength(10);
                builder.pattern("^\\d{6,10}$");
            } else if (k.equals("scc")) { // کد گمرکی فروشنده
                builder.minLength(5);
                builder.maxLength(5);
                builder.pattern("^\\d{5}$");
            } else if (k.equals("scln")) { // شماره مجوز گمرکی فروشنده
                builder.minLength(5);
                builder.maxLength(14);
                builder.pattern("^[A-Za-z0-9]{5,14}$");
            } else if (k.equals("crn")) { // شماره ثبت قرارداد
                builder.minLength(1);
                builder.maxLength(12);
                builder.pattern("^[A-Za-z0-9-]{1,12}$");
            } else if (k.equals("billid")) { // شناسه قبض
                builder.minLength(6);
                builder.maxLength(19);
                builder.pattern("^\\d{6,19}$");
            } else if (k.equals("billingid")) { // شناسه یکتای پرداخت
                builder.minLength(6);
                builder.maxLength(50);
                builder.pattern("^\\d{6,50}$");
            } else if (k.equals("body.bsrn")) { // شناسه پرداخت قلم
                builder.minLength(1);
                builder.maxLength(50);
                builder.pattern("^\\d{1,50}$");
            } else if (k.equals("ocu") || k.equals("dco")) { // کد کشور مبدا/مقصد - ISO 3166 alpha-3
                builder.minLength(3);
                builder.maxLength(3);
                builder.pattern("^[A-Z]{3}$");
            } else if (k.equals("tid") || k.equals("rid") || k.equals("did")) { // شناسه‌های اشخاص
                builder.minLength(8);
                builder.maxLength(12);
                builder.pattern("^\\d{8,12}$");
            } else if (k.equals("cno")) { // شماره ناوگان
                builder.minLength(1);
                builder.maxLength(50);
                builder.pattern("^[A-Za-z0-9-]{1,50}$");
            }
        } catch (Exception e) {
            log.debug("Could not apply custom validation for field {}: {}", k, e.getMessage());
        }

        return builder.build();
    }

    private String getEnglishNameForKey(String key, String fallbackFa) {
        Map<String, String> map = new HashMap<>();
        map.put("inno", "Invoice Number");
        map.put("indatim", "Invoice DateTime");
        map.put("m2Indati", "Invoice DateTime (Gregorian)");
        map.put("inty", "Invoice Type");
        map.put("ins", "Invoice Subject");
        map.put("inp", "Invoice Pattern");
        map.put("irtaxid", "Invoice Reference Tax ID");
        map.put("tins", "Seller Tax ID");
        map.put("sbc", "Seller Branch Code");
        map.put("tob", "Buyer Type");
        map.put("bid", "Buyer National/Foreign ID");
        map.put("tinb", "Buyer Tax ID");
        map.put("bbc", "Buyer Branch Code");
        map.put("bpc", "Buyer Postal Code");
        map.put("bpn", "Buyer Passport Number");
        map.put("tprdis", "Total Price Before Discount");
        map.put("tdis", "Total Discount");
        map.put("tadis", "Total After Discount");
        map.put("tvam", "Total VAT Amount");
        map.put("tbill", "Invoice Total");
        map.put("todam", "Total Other Duty Amount");
        map.put("tax17", "Article 17 Tax");
        map.put("setm", "Settlement Type");
        map.put("cap", "Cash Amount");
        map.put("insp", "Installment Amount");
        map.put("tvop", "Total Tax Paid");
        map.put("inInsuranceId", "Insurance Identifier");
        map.put("an", "Insurance Tracking Number");
        map.put("asn", "Sales Notice Number");
        map.put("asd", "Sales Notice Date");
        map.put("body.sstid", "Item Code");
        map.put("body.sstt", "Item Title");
        map.put("body.mu", "Measure Unit");
        map.put("body.am", "Amount");
        map.put("body.fee", "Unit Price");
        map.put("body.prdis", "Price Before Discount");
        map.put("body.dis", "Discount");
        map.put("body.adis", "Price After Discount");
        map.put("body.vra", "Tax Rate");
        map.put("body.vam", "Tax Amount");
        map.put("body.vop", "Tax Paid Per Line");
        map.put("body.odt", "Duty Title");
        map.put("body.odr", "Duty Rate");
        map.put("body.odam", "Duty Amount");
        map.put("body.olt", "Other Legal Title");
        map.put("body.olr", "Other Legal Rate");
        map.put("body.olam", "Other Legal Amount");
        map.put("body.cop", "Cash Paid");
        map.put("body.tsstam", "Total Item Amount");
        map.put("body.bsrn", "Billing Serial Number");
        map.put("body.medid", "Medical Item ID");
        map.put("body.aspr", "Approved Sale Price");
        map.put("lno", "Waybill Number");
        map.put("lrno", "Reference Waybill Number");
        map.put("ocu", "Origin Country");
        map.put("oci", "Origin City");
        map.put("dco", "Destination Country");
        map.put("dci", "Destination City");
        map.put("tid", "Sender Identifier");
        map.put("rid", "Receiver Identifier");
        map.put("lt", "Shipping Type");
        map.put("cno", "Fleet Number");
        map.put("did", "Driver Identifier");
        // pattern 2 specific
        map.put("scc", "Seller Customs Code");
        map.put("scln", "Seller Customs License No");
        map.put("body.cut", "Currency Code");
        map.put("body.cfee", "Unit Price (Currency)");
        map.put("body.exr", "Exchange Rate");
        map.put("body.cpr", "Currency Purchase Rate");
        map.put("body.sovat", "VAT Base");
        map.put("body.vba", "Tax Base");

        String v = map.get(key);
        if (v != null) return v;
        // default fallback: transform key to English-like title
        String k = key.toLowerCase().contains(".") ? key.substring(key.lastIndexOf('.') + 1) : key;
        return toTitle(k);
    }

    private String toTitle(String s) {
        if (s == null || s.isBlank()) return "Field";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (i == 0) out.append(Character.toUpperCase(c));
            else if (c == '_' || c == '-') out.append(' ');
            else if (Character.isUpperCase(c)) { out.append(' ').append(c); }
            else out.append(c);
        }
        return out.toString();
    }

    /**
     * گزینه‌های نوع صورتحساب
     */
    private List<Map<String, Object>> getInvoiceTypeOptions() {
        return Arrays.asList(
                Map.of("value", 1, "label", "فروش"),
                Map.of("value", 2, "label", "برگشت از فروش"),
                Map.of("value", 3, "label", "اصلاحیه")
        );
    }

    /**
     * گزینه‌های موضوع صورتحساب
     */
    private List<Map<String, Object>> getInvoiceSubjectOptions() {
        return Arrays.asList(
                Map.of("value", 1, "label", "اصلی"),
                Map.of("value", 2, "label", "اصلاحی"),
                Map.of("value", 3, "label", "ابطالی"),
                Map.of("value", 4, "label", "برگشت")
        );
    }

    /**
     * گزینه‌های نوع خریدار
     */
    private List<Map<String, Object>> getPersonTypeOptions() {
        return Arrays.asList(
                Map.of("value", 1, "label", "حقیقی"),
                Map.of("value", 2, "label", "حقوقی"),
                Map.of("value", 3, "label", "مشارکت مدنی"),
                Map.of("value", 4, "label", "اتباع غیر ایرانی")
        );
    }

    /**
     * گزینه‌های نوع تسویه
     */
    private List<Map<String, Object>> getSettlementTypeOptions() {
        return Arrays.asList(
                Map.of("value", 1, "label", "نقدی"),
                Map.of("value", 2, "label", "نسیه"),
                Map.of("value", 3, "label", "نقدی و نسیه")
        );
    }

    /**
     * دریافت لیست تمام الگوها
     */
    public List<Map<String, Object>> getAllPatterns() {
        // فهرست دینامیک بر اساس Provider ها (هر تعدادی که وجود داشته باشند)
        return providerMap.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> Map.<String, Object>of(
                        "patternNumber", e.getKey(),
                        "name", e.getValue().getPatternName(),
                        "code", "PATTERN_" + e.getKey(),
                        "hasProvider", true
                ))
                .collect(Collectors.toList());
    }
}
