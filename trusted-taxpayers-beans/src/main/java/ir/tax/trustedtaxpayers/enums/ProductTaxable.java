package ir.tax.trustedtaxpayers.enums;

/**
 * نوع مشمولیت مالیاتی محصول
 * 
 * <p>این enum نوع مشمولیت مالیاتی محصول را تعریف می‌کند.</p>
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
public enum ProductTaxable {
    /**
     * معاف - محصول از پرداخت مالیات معاف است
     */
    EXEMPT("EXEMPT", "معاف"),
    
    /**
     * مشمول - محصول مشمول پرداخت مالیات است
     */
    TAXABLE("TAXABLE", "مشمول"),
    
    /**
     * غیر مشمول - محصول مشمول مالیات نیست
     */
    NON_TAXABLE("NON_TAXABLE", "غیر مشمول");

    private final String code;
    private final String persianName;

    ProductTaxable(String code, String persianName) {
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
     * پیدا کردن ProductTaxable بر اساس کد
     */
    public static ProductTaxable fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (ProductTaxable taxable : values()) {
            if (taxable.code.equalsIgnoreCase(code)) {
                return taxable;
            }
        }
        return null;
    }

    /**
     * پیدا کردن ProductTaxable بر اساس نام فارسی
     */
    public static ProductTaxable fromPersianName(String persianName) {
        if (persianName == null || persianName.isBlank()) {
            return null;
        }
        // Normalize: remove spaces and BOM for comparison
        String normalized = persianName.replaceAll("[\\s\\uFEFF]", "").trim();
        for (ProductTaxable taxable : values()) {
            // Normalize both sides for comparison
            String taxablePersianNormalized = taxable.persianName != null ? 
                    taxable.persianName.replaceAll("[\\s\\uFEFF]", "").trim() : "";
            if (taxablePersianNormalized.equals(normalized)) {
                return taxable;
            }
        }
        return null;
    }
}

