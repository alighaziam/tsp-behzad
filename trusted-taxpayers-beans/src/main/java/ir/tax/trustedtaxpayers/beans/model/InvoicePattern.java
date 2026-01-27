package ir.tax.trustedtaxpayers.beans.model;

/**
 * الگوی صورتحساب (inp)
 * مقادیر 1 تا 13
 */
public enum InvoicePattern {
    /**
     * الگوی اول - فروش (1)
     */
    SALES(1, "فروش"),
    
    /**
     * الگوی دوم - فروش ارز (2)
     */
    CURRENCY_SALES(2, "فروش ارز"),
    
    /**
     * الگوی سوم - طلا، جواهر و پالتین (3)
     */
    GOLD_JEWELRY(3, "طلا، جواهر و پالتین"),
    
    /**
     * الگوی چهارم - قرارداد پیمانکاری (4)
     */
    CONTRACTING(4, "قرارداد پیمانکاری"),
    
    /**
     * الگوی پنجم - قبوض خدماتی (5)
     */
    SERVICE_BILLS(5, "قبوض خدماتی"),
    
    /**
     * الگوی ششم - بلیط هواپیما (6)
     */
    AIRPLANE_TICKET(6, "بلیط هواپیما"),
    
    /**
     * الگوی هفتم - صادرات (7)
     */
    EXPORT(7, "صادرات"),
    
    /**
     * الگوی هشتم - بارنامه (8)
     */
    WAYBILL(8, "بارنامه"),
    
    /**
     * الگوی نهم - فروش فرآورده‌های نفتی (9)
     */
    PETROLEUM_PRODUCTS(9, "فروش فرآورده‌های نفتی"),
    
    /**
     * الگوی دهم - (10)
     */
    TEMPLATE_10(10, "الگوی دهم"),
    
    /**
     * الگوی یازدهم - بورس اوراق بهادار مبتنی بر کالا (11)
     */
    EXCHANGE_SECURITIES(11, "بورس اوراق بهادار مبتنی بر کالا"),
    
    /**
     * الگوی دوازدهم - (12)
     */
    TEMPLATE_12(12, "الگوی دوازدهم"),
    
    /**
     * الگوی سیزدهم - فروش خدمات بیمه‌ای (13)
     */
    INSURANCE_SERVICES(13, "فروش خدمات بیمه‌ای");
    
    private final Integer value;
    private final String persianName;
    
    InvoicePattern(Integer value, String persianName) {
        this.value = value;
        this.persianName = persianName;
    }
    
    public Integer getValue() {
        return value;
    }
    
    public String getPersianName() {
        return persianName;
    }
    
    public static InvoicePattern fromValue(Integer value) {
        for (InvoicePattern pattern : values()) {
            if (pattern.value.equals(value)) {
                return pattern;
            }
        }
        throw new IllegalArgumentException("Invalid InvoicePattern value: " + value);
    }
}


