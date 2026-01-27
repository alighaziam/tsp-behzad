package ir.tax.trustedtaxpayers.beans.model;

/**
 * نوع صورتحساب (inty)
 * مقادیر 1, 2, 3
 */
public enum InvoiceType {
    /**
     * صورتحساب الکترونیکی نوع اول (1)
     */
    TYPE1_SALES(1, "صورتحساب الکترونیکی نوع اول"),
    
    /**
     * صورتحساب الکترونیکی نوع دوم (2)
     */
    TYPE2_FX(2, "صورتحساب الکترونیکی نوع دوم"),
    
    /**
     * صورتحساب الکترونیکی نوع سوم (3)
     */
    TYPE3_GOLD(3, "صورتحساب الکترونیکی نوع سوم"),
    
    /**
     * نوع چهارم - قرارداد (4)
     */
    TYPE4_CONTRACT(4, "قرارداد"),
    
    /**
     * نوع پنجم - قبوض خدماتی (5)
     */
    TYPE5_UTILITY(5, "قبوض خدماتی"),
    
    /**
     * نوع ششم - بلیط هواپیما (6)
     */
    TYPE6_AIR(6, "بلیط هواپیما"),
    
    /**
     * نوع هفتم - صادرات (7)
     */
    TYPE7_EXPORT(7, "صادرات"),
    
    /**
     * نوع هشتم - الگو (8)
     */
    TYPE8_TEMPLATE(8, "الگوی هشتم"),
    
    /**
     * نوع نهم - الگو (9)
     */
    TYPE9_TEMPLATE(9, "الگوی نهم"),
    
    /**
     * نوع دهم - الگو (10)
     */
    TYPE10_TEMPLATE(10, "الگوی دهم"),
    
    /**
     * نوع یازدهم - الگو (11)
     */
    TYPE11_TEMPLATE(11, "الگوی یازدهم"),
    
    /**
     * نوع دوازدهم - الگو (12)
     */
    TYPE12_TEMPLATE(12, "الگوی دوازدهم"),
    
    /**
     * نوع سیزدهم - الگو (13)
     */
    TYPE13_TEMPLATE(13, "الگوی سیزدهم"),
    
    /**
     * نوع چهاردهم - الگو (14)
     */
    TYPE14_TEMPLATE(14, "الگوی چهاردهم"),
    
    /**
     * نوع پانزدهم - الگو (15)
     */
    TYPE15_TEMPLATE(15, "الگوی پانزدهم"),
    
    /**
     * نوع شانزدهم - الگو (16)
     */
    TYPE16_TEMPLATE(16, "الگوی شانزدهم");
    
    private final Integer value;
    private final String persianName;
    
    InvoiceType(Integer value, String persianName) {
        this.value = value;
        this.persianName = persianName;
    }
    
    public Integer getValue() {
        return value;
    }
    
    public String getPersianName() {
        return persianName;
    }
    
    public static InvoiceType fromValue(Integer value) {
        for (InvoiceType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid InvoiceType value: " + value);
    }
}


