package ir.tax.trustedtaxpayers.application.exception;

import ir.gov.tax.tpis.sdk.transfer.exception.TaxApiException;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Exception Handler برای خطاهای SDK
 * Handler برای مدیریت خطاهای TaxApiException و سایر خطاهای مربوط به SDK
 */
@RestControllerAdvice
public class TaxApiExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(TaxApiExceptionHandler.class);

    private final MessageSource messageSource;

    public TaxApiExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(TaxApiException.class)
    public ResponseEntity<ErrorResponse> handleTaxApiException(TaxApiException e, HttpServletRequest request) {
        logger.error("TaxApiException occurred: {}", e.getMessage(), e);

        String code = e.getCode() != null && !e.getCode().isBlank() ? e.getCode() : "TAX_API_ERROR";
        MappingResult mapping = mapSdkCodeToStatusAndMessage(code, e.getMessage());

        String localized = messageSource.getMessage("error." + code, null, mapping.message, Locale.getDefault());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(mapping.status.value())
                .error(code)
                .message(localized)
                .code(code)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(mapping.status).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e, HttpServletRequest request) {
        logger.error("IllegalArgumentException occurred: {}", e.getMessage(), e);

        HttpStatus status = HttpStatus.BAD_REQUEST;
        String localized = messageSource.getMessage("error.INVALID_REQUEST", null, "درخواست نامعتبر است", Locale.getDefault());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error("INVALID_REQUEST")
                .message(localized)
                .code("INVALID_REQUEST")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntimeException(RuntimeException e, HttpServletRequest request) {
        // فقط برای خطاهای مربوط به Modian System
        if (e.getMessage() != null && e.getMessage().contains("Modian")) {
            logger.error("RuntimeException in Modian transfer: {}", e.getMessage(), e);

            HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
            String localized = messageSource.getMessage("error.MODIAN_TRANSFER_ERROR", null, "خطای انتقال در سامانه مودیان", Locale.getDefault());

            ErrorResponse response = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(status.value())
                    .error("MODIAN_TRANSFER_ERROR")
                    .message(localized)
                    .code("MODIAN_TRANSFER_ERROR")
                    .path(request.getRequestURI())
                    .build();

            return ResponseEntity.status(status).body(response);
        }

        // برای سایر RuntimeExceptionها، به handler پیش‌فرض ارسال می‌شود
        throw e;
    }

    private MappingResult mapSdkCodeToStatusAndMessage(String code, String fallbackMessage) {
        if (code == null || code.isBlank()) {
            return new MappingResult(HttpStatus.BAD_REQUEST, fallbackOrDefault(fallbackMessage, "خطای سرویس مالیاتی"));
        }
        String c = code.toUpperCase(Locale.ROOT).trim();

        // کدهای شناخته‌شده داخلی سامانه
        if (c.equals("TRANSMISSION_ERROR")) {
            return new MappingResult(HttpStatus.BAD_GATEWAY, "ارسال بسته به سرویس مالیاتی ناموفق بود");
        }
        if (c.equals("RESILIENCE_ERROR")) {
            return new MappingResult(HttpStatus.SERVICE_UNAVAILABLE, "خطای تاب‌آوری ارتباط با سرویس مالیاتی");
        }
        if (c.equals("NOT_RETRIABLE")) {
            return new MappingResult(HttpStatus.BAD_REQUEST, "خطا غیرقابل تلاش مجدد است");
        }

        // نگاشت‌های وضعیت متداول
        if (c.contains("TIMEOUT") || c.contains("GATEWAY_TIMEOUT") || c.contains("READ_TIMEOUT")) {
            return new MappingResult(HttpStatus.GATEWAY_TIMEOUT, "پاسخ سرویس مالیاتی به‌موقع دریافت نشد");
        }
        if (c.contains("TOO_MANY_REQUEST") || c.contains("RATE_LIMIT")) {
            return new MappingResult(HttpStatus.TOO_MANY_REQUESTS, "تعداد درخواست‌ها بیش از حد مجاز است");
        }
        if (c.contains("UNAVAILABLE") || c.contains("SERVICE_UNAVAILABLE") || c.contains("CIRCUIT_OPEN")) {
            return new MappingResult(HttpStatus.SERVICE_UNAVAILABLE, "سرویس مالیاتی موقتاً در دسترس نیست");
        }
        if (c.contains("UNAUTHORIZED") || c.equals("401")) {
            return new MappingResult(HttpStatus.UNAUTHORIZED, "احراز هویت نامعتبر است");
        }
        if (c.contains("FORBIDDEN") || c.equals("403")) {
            return new MappingResult(HttpStatus.FORBIDDEN, "دسترسی مجاز نیست");
        }
        if (c.contains("NOT_FOUND") || c.equals("404")) {
            return new MappingResult(HttpStatus.NOT_FOUND, "منبع مورد نظر یافت نشد");
        }
        if (c.contains("CONFLICT") || c.equals("409")) {
            return new MappingResult(HttpStatus.CONFLICT, "تعارض در داده‌ها وجود دارد");
        }
        if (c.contains("BAD_REQUEST") || c.contains("VALIDATION") || c.contains("INVALID") || c.equals("400")) {
            return new MappingResult(HttpStatus.BAD_REQUEST, fallbackOrDefault(fallbackMessage, "درخواست نامعتبر است"));
        }
        if (c.contains("INTERNAL") || c.equals("500") || c.contains("SERVER_ERROR")) {
            return new MappingResult(HttpStatus.INTERNAL_SERVER_ERROR, "خطای داخلی سرور رخ داده است");
        }

        // پیش‌فرض
        return new MappingResult(HttpStatus.BAD_REQUEST, fallbackOrDefault(fallbackMessage, "خطای سرویس مالیاتی"));
    }

    private String fallbackOrDefault(String value, String def) {
        return (value != null && !value.isBlank()) ? value : def;
    }

    private static class MappingResult {
        final HttpStatus status;
        final String message;
        MappingResult(HttpStatus status, String message) {
            this.status = status;
            this.message = message;
        }
    }
}

