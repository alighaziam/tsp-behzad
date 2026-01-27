package ir.tax.trustedtaxpayers.enums;

/**
 * نوع فیلد تکمیلی (extensionType)
 * 
 * <p>این enum نوع داده فیلد تکمیلی را مشخص می‌کند.</p>
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
public enum ExtensionType {
    /**
     * رشته (STRING)
     */
    STRING("STRING", "رشته"),
    
    /**
     * عدد (NUMBER)
     */
    NUMBER("NUMBER", "عدد"),
    
    /**
     * تاریخ (DATE)
     */
    DATE("DATE", "تاریخ"),
    
    /**
     * JSON
     */
    JSON("JSON", "JSON"),
    
    /**
     * XML
     */
    XML("XML", "XML"),
    
    /**
     * بولین (BOOLEAN)
     */
    BOOLEAN("BOOLEAN", "بولین");
    
    private final String value;
    private final String persianName;
    
    ExtensionType(String value, String persianName) {
        this.value = value;
        this.persianName = persianName;
    }
    
    public String getValue() {
        return value;
    }
    
    public String getPersianName() {
        return persianName;
    }
    
    public static ExtensionType fromValue(String value) {
        if (value == null) {
            return null;
        }
        for (ExtensionType type : values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid ExtensionType value: " + value);
    }
}


