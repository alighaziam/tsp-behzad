package ir.tax.trustedtaxpayers.enums;

/**
 * نوع محصول مالیاتی
 * 
 * <p>این enum انواع مختلف شناسه‌های محصولات مالیاتی را تعریف می‌کند.</p>
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
public enum TaxProductType {
    /**
     * شناسه اختصاصی تولید داخل - Exclusive ID for Domestic Production
     */
    EXCLUSIVE_DOMESTIC_PRODUCTION("EXCLUSIVE_DOMESTIC_PRODUCTION", "شناسه اختصاصی تولید داخل"),
    
    /**
     * شناسه اختصاصی خدمت - Exclusive ID for Service
     */
    EXCLUSIVE_SERVICE("EXCLUSIVE_SERVICE", "شناسه اختصاصی خدمت"),
    
    /**
     * شناسه اختصاصی وارداتی - Exclusive ID for Imported
     */
    EXCLUSIVE_IMPORTED("EXCLUSIVE_IMPORTED", "شناسه اختصاصی وارداتی"),
    
    /**
     * شناسه عمومی تولید داخل - General ID for Domestic Production
     */
    GENERAL_DOMESTIC_PRODUCTION("GENERAL_DOMESTIC_PRODUCTION", "شناسه عمومی تولید داخل"),
    
    /**
     * شناسه عمومی خدمت - General ID for Service
     */
    GENERAL_SERVICE("GENERAL_SERVICE", "شناسه عمومی خدمت"),
    
    /**
     * شناسه عمومی وارداتی - General ID for Imported
     */
    GENERAL_IMPORTED("GENERAL_IMPORTED", "شناسه عمومی وارداتی");

    private final String code;
    private final String persianName;

    TaxProductType(String code, String persianName) {
        this.code = code;
        this.persianName = persianName;
    }

    public String getCode() {
        return code;
    }

    public String getPersianName() {
        return persianName;
    }

    /**
     * پیدا کردن TaxProductType بر اساس کد
     */
    public static TaxProductType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (TaxProductType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * پیدا کردن TaxProductType بر اساس نام فارسی
     */
    public static TaxProductType fromPersianName(String persianName) {
        if (persianName == null || persianName.isBlank()) {
            return null;
        }
        // Normalize: remove spaces and BOM for comparison
        String normalized = persianName.replaceAll("[\\s\\uFEFF]", "").trim();
        for (TaxProductType type : values()) {
            // Normalize both sides for comparison
            String typePersianNormalized = type.persianName != null ? 
                    type.persianName.replaceAll("[\\s\\uFEFF]", "").trim() : "";
            if (typePersianNormalized.equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}

