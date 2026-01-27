package ir.tax.trustedtaxpayers.beans.model;

/**
 * نوع حمل و نقل (lt)
 * 
 * <p>این enum نوع حمل و نقل بارنامه را مشخص می‌کند.</p>
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
public enum ShippingType {
    /**
     * جاده‌ای (1)
     */
    ROAD(1, "جاده‌ای"),
    
    /**
     * ریلی (2)
     */
    RAIL(2, "ریلی"),
    
    /**
     * هوایی (3)
     */
    AIR(3, "هوایی"),
    
    /**
     * دریایی (4)
     */
    MARITIME(4, "دریایی");
    
    private final Integer value;
    private final String persianName;
    
    ShippingType(Integer value, String persianName) {
        this.value = value;
        this.persianName = persianName;
    }
    
    public Integer getValue() {
        return value;
    }
    
    public String getPersianName() {
        return persianName;
    }
    
    public static ShippingType fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (ShippingType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid ShippingType value: " + value);
    }
}


