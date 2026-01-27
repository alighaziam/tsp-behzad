package ir.tax.trustedtaxpayers.beans.model;

/**
 * روش تسویه (setm)
 * مقادیر 1 تا 3
 */
public enum SettlementMethod {
    /**
     * نقدی (1)
     */
    CASH(1, "نقدی"),
    
    /**
     * نسیه (2)
     */
    CREDIT(2, "نسیه"),
    
    /**
     * نقدی/نسیه (3)
     */
    CASH_CREDIT(3, "نقدی/نسیه");
    
    private final Integer value;
    private final String persianName;
    
    SettlementMethod(Integer value, String persianName) {
        this.value = value;
        this.persianName = persianName;
    }
    
    public Integer getValue() {
        return value;
    }
    
    public String getPersianName() {
        return persianName;
    }
    
    public static SettlementMethod fromValue(Integer value) {
        for (SettlementMethod method : values()) {
            if (method.value.equals(value)) {
                return method;
            }
        }
        throw new IllegalArgumentException("Invalid SettlementMethod value: " + value);
    }
}


