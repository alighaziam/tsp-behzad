package ir.tax.trustedtaxpayers.beans.model;

/**
 * نوع شخص خریدار (tob)
 * مقادیر 1 تا 4
 */
public enum PersonType {
    /**
     * حقیقی (1)
     */
    NATURAL_PERSON(1, "حقیقی"),
    
    /**
     * حقوقی (2)
     */
    LEGAL_ENTITY(2, "حقوقی"),
    
    /**
     * مشارکت مدنی (3)
     */
    PARTNERSHIP(3, "مشارکت مدنی"),
    
    /**
     * اتباع غیر ایرانی (4)
     */
    NON_IRANIAN(4, "اتباع غیر ایرانی");
    
    private final Integer value;
    private final String persianName;
    
    PersonType(Integer value, String persianName) {
        this.value = value;
        this.persianName = persianName;
    }
    
    public Integer getValue() {
        return value;
    }
    
    public String getPersianName() {
        return persianName;
    }
    
    public static PersonType fromValue(Integer value) {
        for (PersonType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid PersonType value: " + value);
    }
}


