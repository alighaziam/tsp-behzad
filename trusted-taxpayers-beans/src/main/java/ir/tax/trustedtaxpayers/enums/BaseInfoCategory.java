package ir.tax.trustedtaxpayers.enums;

public enum BaseInfoCategory {

    MEASUREMENT_UNIT("MEASUREMENT_UNIT", "واحد اندازه‌گیری"),
    CURRENCY_TYPE("CURRENCY_TYPE", "نوع ارز"),
    PERSON_TYPE("PERSON_TYPE", "نوع شخص"),
    INVOICE_SUBJECT("INVOICE_SUBJECT", "موضوع فاکتور"),
    INVOICE_TYPE("INVOICE_TYPE", "نوع فاکتور"),
    SETTLEMENT_METHOD("SETTLEMENT_METHOD", "روش تسویه"),
    FLIGHT_TYPE("FLIGHT_TYPE", "نوع پرواز"),
    PAYMENT_METHOD("PAYMENT_METHOD", "روش پرداخت"),
    WAYBILL_TYPE("WAYBILL_TYPE", "نوع بارنامه");

    private final String code;
    private final String persianName;

    BaseInfoCategory(String code, String persianName) {
        this.code = code;
        this.persianName = persianName;
    }

    public String getCode() {
        return code;
    }

    public String getPersianName() {
        return persianName;
    }
}

