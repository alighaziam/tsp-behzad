package ir.tax.trustedtaxpayers.beans.model;

/**
 * موضوع صورتحساب (ins)
 * مقادیر 1 تا 4
 */
public enum InvoiceSubject {
    /**
     * اصلی (1)
     */
    ORIGINAL(1, "اصلی"),
    
    /**
     * اصلاحی (2)
     */
    CORRECTION(2, "اصلاحی"),
    
    /**
     * ابطالی (3)
     */
    CANCELLATION(3, "ابطالی"),
    
    /**
     * برگشت از فروش (4)
     */
    RETURN(4, "برگشت از فروش");
    
    private final Integer value;
    private final String persianName;
    
    InvoiceSubject(Integer value, String persianName) {
        this.value = value;
        this.persianName = persianName;
    }
    
    public Integer getValue() {
        return value;
    }
    
    public String getPersianName() {
        return persianName;
    }
    
    public static InvoiceSubject fromValue(Integer value) {
        for (InvoiceSubject subject : values()) {
            if (subject.value.equals(value)) {
                return subject;
            }
        }
        throw new IllegalArgumentException("Invalid InvoiceSubject value: " + value);
    }
}


