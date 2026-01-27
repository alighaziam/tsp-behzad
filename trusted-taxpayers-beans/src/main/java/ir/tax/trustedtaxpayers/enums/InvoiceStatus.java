package ir.tax.trustedtaxpayers.enums;

/**
 * وضعیت صورتحساب الکترونیکی
 * 
 * <p>این enum وضعیت‌های مختلف صورتحساب را تعریف می‌کند.</p>
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
public enum InvoiceStatus {
    /**
     * آماده ارسال - صورتحساب در دیتابیس ذخیره شده و آماده ارسال به سامانه مودیان است
     */
    READY_TO_SEND("READY_TO_SEND", "آماده ارسال"),
    
    /**
     * ارسال شده - صورتحساب با موفقیت به سامانه مودیان ارسال شده است
     */
    SENT("SENT", "ارسال شده");

    private final String code;
    private final String persianName;

    InvoiceStatus(String code, String persianName) {
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
     * پیدا کردن InvoiceStatus بر اساس کد
     */
    public static InvoiceStatus fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        for (InvoiceStatus status : values()) {
            if (status.code.equalsIgnoreCase(code)) {
                return status;
            }
        }
        return null;
    }
}














