package ir.tax.trustedtaxpayers.application.exception;

import org.springframework.http.HttpStatus;

/**
 * کدهای خطای سیستم با پیام‌های فارسی
 */
public enum ErrorCode {

    // 404 - Not Found
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "ENTITY_NOT_FOUND", "رکورد مورد نظر یافت نشد"),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "کاربر یافت نشد"),
    TAXPAYER_NOT_FOUND(HttpStatus.NOT_FOUND, "TAXPAYER_NOT_FOUND", "مودی مالیاتی یافت نشد"),
    INVOICE_NOT_FOUND(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "صورتحساب الکترونیکی یافت نشد"),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "محصول یافت نشد"),
    BUYER_NOT_FOUND(HttpStatus.NOT_FOUND, "BUYER_NOT_FOUND", "خریدار یافت نشد"),
    TEMPLATE_NOT_FOUND(HttpStatus.NOT_FOUND, "TEMPLATE_NOT_FOUND", "قالب یافت نشد"),
    ROLE_NOT_FOUND(HttpStatus.NOT_FOUND, "ROLE_NOT_FOUND", "نقش یافت نشد"),
    STAFF_NOT_FOUND(HttpStatus.NOT_FOUND, "STAFF_NOT_FOUND", "کارمند یافت نشد"),

    // 409 - Conflict
    DUPLICATE_RESOURCE(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "رکورد تکراری است"),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "DUPLICATE_USERNAME", "نام کاربری تکراری است"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "DUPLICATE_EMAIL", "ایمیل تکراری است"),
    DUPLICATE_TAXPAYER_NO(HttpStatus.CONFLICT, "DUPLICATE_TAXPAYER_NO", "شماره مالیاتی تکراری است"),
    DUPLICATE_NATIONAL_ID(HttpStatus.CONFLICT, "DUPLICATE_NATIONAL_ID", "کد ملی/شناسه ملی تکراری است"),
    DUPLICATE_PRODUCT_CODE(HttpStatus.CONFLICT, "DUPLICATE_PRODUCT_CODE", "کد محصول تکراری است"),
    DUPLICATE_TAX_ID(HttpStatus.CONFLICT, "DUPLICATE_TAX_ID", "شماره مالیاتی صورتحساب تکراری است"),

    // 400 - Bad Request
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "اطلاعات ورودی نامعتبر است"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "درخواست نامعتبر است"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "ورودی نامعتبر است"),
    INVALID_PARAMETER(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER", "پارامتر ورودی نامعتبر است"),
    INVALID_TEMPLATE(HttpStatus.BAD_REQUEST, "INVALID_TEMPLATE", "قالب انتخابی نامعتبر است"),
    INVALID_INVOICE_DATA(HttpStatus.BAD_REQUEST, "INVALID_INVOICE_DATA", "اطلاعات صورتحساب نامعتبر است"),
    BUSINESS_RULE_VIOLATION(HttpStatus.BAD_REQUEST, "BUSINESS_RULE_VIOLATION", "قوانین کسب‌وکار نقض شده است"),
    REQUIRED_FIELD_MISSING(HttpStatus.BAD_REQUEST, "REQUIRED_FIELD_MISSING", "فیلدهای اجباری کامل نیست"),
    INVALID_DATE_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_DATE_FORMAT", "فرمت تاریخ نامعتبر است"),

    // 401 - Unauthorized
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "احراز هویت انجام نشده است"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "توکن نامعتبر است"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "توکن منقضی شده است"),

    // 403 - Forbidden
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "دسترسی به این منبع مجاز نیست"),
    INSUFFICIENT_PERMISSIONS(HttpStatus.FORBIDDEN, "INSUFFICIENT_PERMISSIONS", "سطح دسترسی کافی نیست"),

    // 500 - Internal Server Error
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "خطای داخلی سرور رخ داده است"),
    DATABASE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "DATABASE_ERROR", "خطا در دسترسی به پایگاه داده"),
    EXTERNAL_SERVICE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "EXTERNAL_SERVICE_ERROR", "خطا در ارتباط با سرویس خارجی"),

    // 501 - Not Implemented
    NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "NOT_IMPLEMENTED", "این قابلیت هنوز پیاده‌سازی نشده است"),

    // 503 - Service Unavailable
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "سرویس در حال حاضر در دسترس نیست");

    private final HttpStatus status;
    private final String code;
    private final String persianMessage;

    ErrorCode(HttpStatus status, String code, String persianMessage) {
        this.status = status;
        this.code = code;
        this.persianMessage = persianMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getPersianMessage() {
        return persianMessage;
    }

    /**
     * برای سازگاری با کدهای قدیمی
     */
    @Deprecated
    public String getDefaultMessage() {
        return persianMessage;
    }
}


