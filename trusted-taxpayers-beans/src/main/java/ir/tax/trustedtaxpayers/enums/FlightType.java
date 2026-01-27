package ir.tax.trustedtaxpayers.enums;

/**
 * نوع پرواز (ft)
 * مقادیر 1 تا 2
 */
public enum FlightType {
    /**
     * داخلی (1)
     */
    DOMESTIC(1, "داخلی"),
    
    /**
     * خارجی (2)
     */
    INTERNATIONAL(2, "خارجی");
    
    private final Integer value;
    private final String persianName;
    
    FlightType(Integer value, String persianName) {
        this.value = value;
        this.persianName = persianName;
    }
    
    public Integer getValue() {
        return value;
    }
    
    public String getPersianName() {
        return persianName;
    }
    
    public static FlightType fromValue(Integer value) {
        for (FlightType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid FlightType value: " + value);
    }
}


