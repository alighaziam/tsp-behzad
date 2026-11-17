package ir.tax.trustedtaxpayers.application.exception;

import ir.tax.trustedtaxpayers.application.rate.RateLimitExceededException;
import ir.tax.trustedtaxpayers.application.resilience.CircuitOpenException;
import ir.tax.trustedtaxpayers.interfaces.rest.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component("applicationGlobalExceptionHandler")
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MessageSource messageSource;
    private final Environment environment;

    public GlobalExceptionHandler(MessageSource messageSource, Environment environment) {
        this.messageSource = messageSource;
        this.environment = environment;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        logger.debug("Validation error occurred: {}", ex.getMessage());
        
        Map<String, String> errors = ex.getBindingResult().getFieldErrors()
                .stream().collect(Collectors.toMap(
                        e -> e.getField(),
                        e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "Validation failed",
                        (a, b) -> a
                ));

        String localized = messageSource.getMessage("error.VALIDATION_FAILED", null, "اطلاعات ورودی نامعتبر است", Locale.getDefault());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_FAILED")
                .message(localized)
                .code("VALIDATION_FAILED")
                .path(request.getRequestURI())
                .details(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        logger.debug("Constraint violation occurred: {}", ex.getMessage());
        
        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        v -> v.getMessage() != null ? v.getMessage() : "Constraint violation",
                        (a, b) -> a
                ));

        String localized = messageSource.getMessage("error.VALIDATION_FAILED", null, "اطلاعات ورودی نامعتبر است", Locale.getDefault());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("VALIDATION_FAILED")
                .message(localized)
                .code("VALIDATION_FAILED")
                .path(request.getRequestURI())
                .details(errors)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        logger.warn("Unreadable HTTP message: {}", ex.getMessage(), ex);
        
        String localized = messageSource.getMessage("error.BAD_REQUEST", null, "درخواست نامعتبر است", Locale.getDefault());

        String detailMessage = ex.getMessage();
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            detailMessage = ex.getCause().getMessage();
        }

        Map<String, String> details = new HashMap<>();
        details.put("error", ex.getClass().getSimpleName());
        details.put("message", ex.getMessage() != null ? ex.getMessage() : "Unknown error");
        if (ex.getCause() != null) {
            details.put("cause", ex.getCause().getMessage() != null ? ex.getCause().getMessage() : "Unknown cause");
        }

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("BAD_REQUEST")
                .message(localized + (detailMessage != null ? ": " + detailMessage : ""))
                .code("BAD_REQUEST")
                .path(request.getRequestURI())
                .details(details)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String parameterName = ex.getName();
        String providedValue = ex.getValue() != null ? ex.getValue().toString() : "null";
        String requiredType = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        
        logger.warn("Method argument type mismatch: parameter={}, value={}, requiredType={}, path={}", 
                parameterName, providedValue, requiredType, request.getRequestURI());
        
        String localized = messageSource.getMessage("error.INVALID_PARAMETER_TYPE", null, 
                "نوع پارامتر ورودی نامعتبر است", Locale.getDefault());
        
        String detailMessage;
        if (ex.getRequiredType() != null && java.util.UUID.class.equals(ex.getRequiredType())) {
            detailMessage = String.format("پارامتر '%s' باید یک UUID معتبر باشد، اما مقدار '%s' ارسال شده است", 
                    parameterName, providedValue);
        } else {
            detailMessage = String.format("پارامتر '%s' باید از نوع '%s' باشد، اما مقدار '%s' ارسال شده است", 
                    parameterName, requiredType, providedValue);
        }
        
        Map<String, String> details = new HashMap<>();
        details.put("parameter", parameterName);
        details.put("providedValue", providedValue);
        details.put("requiredType", requiredType);
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            details.put("cause", ex.getCause().getMessage());
        }

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("INVALID_PARAMETER_TYPE")
                .message(localized + ": " + detailMessage)
                .code("INVALID_PARAMETER_TYPE")
                .path(request.getRequestURI())
                .details(details)
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = ex.getStatusCode() instanceof HttpStatus httpStatus ? httpStatus : HttpStatus.BAD_REQUEST;
        String reason = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        
        logger.debug("ResponseStatusException: status={}, reason={}", status, reason);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.name())
                .message(reason)
                .code(status.name())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(response);
    }

    @ExceptionHandler({NoSuchElementException.class, jakarta.persistence.EntityNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(RuntimeException ex, HttpServletRequest request) {
        logger.debug("Resource not found: {}", ex.getMessage());
        
        String localized = messageSource.getMessage("error.NOT_FOUND", null, "منبع مورد نظر یافت نشد", Locale.getDefault());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.NOT_FOUND.value())
                .error("NOT_FOUND")
                .message(localized)
                .code("NOT_FOUND")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        logger.error("IllegalArgumentException occurred: {}", ex.getMessage(), ex);
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "BUSINESS_RULE_VIOLATION",
                "error.BUSINESS_RULE_VIOLATION",
                "قوانین کسب‌وکار نقض شده است",
                request,
                ex,
                null
        );
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ErrorResponse> handleTransactionSystem(TransactionSystemException ex, HttpServletRequest request) {
        logger.error("TransactionSystemException occurred: {}", ex.getMessage(), ex);

        SqlMapping mapping = mapSqlViolation(ex.getRootCause());
        if (mapping != null) {
            String localized = messageSource.getMessage("error." + mapping.code, null, mapping.defaultMessage, Locale.getDefault());
            ErrorResponse response = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(mapping.status.value())
                    .error(mapping.code)
                    .message(localized)
                    .code(mapping.code)
                    .path(request.getRequestURI())
                    .details(extractSafeSqlDetails(ex.getRootCause()))
                    .build();
            return ResponseEntity.status(mapping.status).body(response);
        }

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "error.INTERNAL_ERROR",
                "خطای داخلی سرور رخ داده است",
                request,
                ex,
                extractSafeSqlDetails(ex.getRootCause())
        );
    }

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccess(DataAccessException ex, HttpServletRequest request) {
        logger.error("DataAccessException occurred: {}", ex.getMessage(), ex);

        SqlMapping mapping = mapSqlViolation(ex.getMostSpecificCause());
        if (mapping != null) {
            String localized = messageSource.getMessage("error." + mapping.code, null, mapping.defaultMessage, Locale.getDefault());
            ErrorResponse response = ErrorResponse.builder()
                    .timestamp(LocalDateTime.now())
                    .status(mapping.status.value())
                    .error(mapping.code)
                    .message(localized)
                    .code(mapping.code)
                    .path(request.getRequestURI())
                    .details(extractSafeSqlDetails(ex.getMostSpecificCause()))
                    .build();
            return ResponseEntity.status(mapping.status).body(response);
        }

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "DATABASE_ERROR",
                "error.DATABASE_ERROR",
                "خطا در دسترسی به پایگاه داده",
                request,
                ex,
                extractSafeSqlDetails(ex.getMostSpecificCause())
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String supportedMethods = ex.getSupportedHttpMethods() != null 
            ? String.join(", ", ex.getSupportedHttpMethods().stream().map(HttpMethod::name).toList())
            : "N/A";
        String message = String.format("Request method '%s' is not supported for this endpoint. Supported methods: %s", 
            ex.getMethod(), supportedMethods);
        
        String localized = messageSource.getMessage("error.METHOD_NOT_ALLOWED", null, 
            "روش درخواست پشتیبانی نمی‌شود", Locale.getDefault());

        Map<String, String> details = new HashMap<>();
        details.put("requestedMethod", ex.getMethod());
        details.put("supportedMethods", supportedMethods);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.METHOD_NOT_ALLOWED.value())
                .error("METHOD_NOT_ALLOWED")
                .message(localized + " (" + message + ")")
                .code("METHOD_NOT_ALLOWED")
                .path(request.getRequestURI())
                .details(details)
                .build();

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(AccessDeniedException ex) {
        logger.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "FORBIDDEN",
                "message", "You do not have permission to access this resource."
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        // Always log unexpected exceptions with full stack trace
        logger.error("Unexpected exception occurred at {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        // در Production، جزئیات خطا را نمایش ندهید
        boolean isDevelopment = environment != null && (
                Arrays.asList(environment.getActiveProfiles()).contains("dev") ||
                Arrays.asList(environment.getActiveProfiles()).contains("development")
        );

        Map<String, String> details = null;
        if (isDevelopment) {
            details = new HashMap<>();
            details.put("exception", ex.getClass().getName());
            details.put("message", ex.getMessage());
            Throwable rootCause = findRootCause(ex);
            if (rootCause != null && rootCause != ex) {
                details.put("rootCause", rootCause.getClass().getName());
                details.put("rootCauseMessage", rootCause.getMessage());
            }
        }

        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "error.INTERNAL_ERROR",
                "خطای داخلی سرور رخ داده است",
                request,
                ex,
                details
        );
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimit(RateLimitExceededException ex, HttpServletRequest req) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Retry-After", String.valueOf(Math.max(ex.getRetryAfterSeconds(), 1)));

        String localized = messageSource.getMessage("error.RATE_LIMIT_EXCEEDED", null, "تعداد فراخوانی مجاز برای این سرویس به اتمام رسیده است", Locale.getDefault());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error("RATE_LIMIT_EXCEEDED")
                .message(localized)
                .code("RATE_LIMIT_EXCEEDED")
                .path(req.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).headers(headers).body(response);
    }

    @ExceptionHandler(CircuitOpenException.class)
    public ResponseEntity<ErrorResponse> handleCircuitOpen(CircuitOpenException ex, HttpServletRequest req) {
        String localized = messageSource.getMessage("error.CIRCUIT_OPEN", null, "سرویس موقتاً در دسترس نیست", Locale.getDefault());

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(HttpStatus.SERVICE_UNAVAILABLE.value())
                .error("CIRCUIT_OPEN")
                .message(localized)
                .code("CIRCUIT_OPEN")
                .path(req.getRequestURI())
                .build();

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @ExceptionHandler(ServiceAccessException.class)
    public ResponseEntity<ErrorResponse> handleServiceAccess(ServiceAccessException ex, HttpServletRequest req) {
        logger.warn("ServiceAccessException: serviceCode={}, reason={}, message={}", 
                ex.getServiceCode(), ex.getReason(), ex.getMessage());

        String localized = messageSource.getMessage("error.SERVICE_ACCESS_DENIED", null, 
                ex.getMessage(), Locale.getDefault());

        Map<String, String> details = new HashMap<>();
        if (ex.getServiceCode() != null) {
            details.put("serviceCode", ex.getServiceCode());
        }
        if (ex.getReason() != null) {
            details.put("reason", ex.getReason());
        }

        HttpStatus status = "LIMIT_EXCEEDED".equals(ex.getReason()) 
                ? HttpStatus.TOO_MANY_REQUESTS 
                : HttpStatus.FORBIDDEN;

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(java.time.LocalDateTime.now())
                .status(status.value())
                .error("SERVICE_ACCESS_DENIED")
                .message(localized)
                .code("SERVICE_ACCESS_DENIED")
                .path(req.getRequestURI())
                .details(details.isEmpty() ? null : details)
                .build();

        return ResponseEntity.status(status).body(response);
    }

    private Map<String, String> extractSafeSqlDetails(Throwable cause) {
        if (cause == null) {
            return null;
        }
        Map<String, String> details = new HashMap<>();
        details.put("cause", cause.getClass().getSimpleName());
        if (cause instanceof SQLException sqlEx) {
            if (sqlEx.getSQLState() != null) {
                details.put("sqlState", sqlEx.getSQLState());
            }
            details.put("vendorCode", String.valueOf(sqlEx.getErrorCode()));
        }
        return details.isEmpty() ? null : details;
    }

    private SqlMapping mapSqlViolation(Throwable cause) {
        if (cause == null) {
            return null;
        }
        String message = cause.getMessage() != null ? cause.getMessage().toLowerCase(Locale.ROOT) : "";
        String sqlState = null;
        int vendorCode = 0;
        if (cause instanceof SQLException sqlEx) {
            sqlState = sqlEx.getSQLState();
            vendorCode = sqlEx.getErrorCode();
        }
        // Unique constraint violations (PostgreSQL: 23505, SQLState starts with 23 for integrity, Oracle: ORA-00001)
        if ("23505".equals(sqlState) || (sqlState != null && sqlState.startsWith("23")) || message.contains("unique constraint") || message.contains("duplicate") || message.contains("ora-00001")) {
            return new SqlMapping(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", "رکورد تکراری است");
        }
        // Foreign key violations (PostgreSQL: 23503, Oracle: ORA-02291/02292)
        if ("23503".equals(sqlState) || message.contains("foreign key") || message.contains("ora-02291") || message.contains("ora-02292")) {
            return new SqlMapping(HttpStatus.CONFLICT, "FOREIGN_KEY_VIOLATION", "نقض قیود کلید خارجی");
        }
        return null;
    }

    private ResponseEntity<ErrorResponse> buildErrorResponse(HttpStatus status,
                                                            String code,
                                                            String messageKey,
                                                            String defaultMessage,
                                                            HttpServletRequest request,
                                                            Throwable ex,
                                                            Map<String, String> extraDetails) {
        String localized = messageSource.getMessage(messageKey, null, defaultMessage, Locale.getDefault());
        String detailMessage = extractMeaningfulMessage(ex);
        String composedMessage = composeMessage(localized, detailMessage);
        Map<String, String> details = mergeDetails(extraDetails, ex, detailMessage);

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(code)
                .message(composedMessage)
                .code(code)
                .path(request.getRequestURI())
                .details(details)
                .build();

        return ResponseEntity.status(status).body(response);
    }

    private String composeMessage(String localized, String detail) {
        String trimmedLocalized = localized != null ? localized.trim() : "";
        if (detail == null || detail.isBlank()) {
            return trimmedLocalized.isEmpty() ? "خطای سامانه" : trimmedLocalized;
        }
        String trimmedDetail = detail.trim();
        if (trimmedLocalized.isEmpty()) {
            return trimmedDetail;
        }
        String localizedLower = trimmedLocalized.toLowerCase(Locale.ROOT);
        String detailLower = trimmedDetail.toLowerCase(Locale.ROOT);
        if (localizedLower.contains(detailLower)) {
            return trimmedLocalized;
        }
        if (detailLower.contains(localizedLower)) {
            return trimmedDetail;
        }
        return trimmedLocalized + " (" + trimmedDetail + ")";
    }

    private String extractMeaningfulMessage(Throwable ex) {
        if (ex == null) {
            return null;
        }
        String sanitized = sanitizeMessage(ex.getMessage());
        if (sanitized != null) {
            return sanitized;
        }
        Throwable cause = ex.getCause();
        if (cause != null && cause != ex) {
            return extractMeaningfulMessage(cause);
        }
        return null;
    }

    private String sanitizeMessage(String message) {
        if (message == null) {
            return null;
        }
        String trimmed = message.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        int maxLength = 400;
        if (trimmed.length() > maxLength) {
            return trimmed.substring(0, maxLength - 3) + "...";
        }
        return trimmed;
    }

    private Map<String, String> mergeDetails(Map<String, String> extraDetails, Throwable ex, String detailMessage) {
        Map<String, String> details = extraDetails != null ? new LinkedHashMap<>(extraDetails) : new LinkedHashMap<>();
        if (ex != null) {
            details.putIfAbsent("exception", ex.getClass().getSimpleName());
            Throwable rootCause = findRootCause(ex);
            if (rootCause != null) {
                details.putIfAbsent("rootCause", rootCause.getClass().getSimpleName());
            }
        }
        if (detailMessage != null && !detailMessage.isBlank()) {
            details.putIfAbsent("reason", detailMessage);
        }
        return details.isEmpty() ? null : details;
    }

    private Throwable findRootCause(Throwable ex) {
        if (ex == null) {
            return null;
        }
        Throwable current = ex;
        Throwable next = current.getCause();
        while (next != null && next != current) {
            current = next;
            next = current.getCause();
        }
        return current == ex ? null : current;
    }

    private static class SqlMapping {
        final HttpStatus status;
        final String code;
        final String defaultMessage;
        SqlMapping(HttpStatus status, String code, String defaultMessage) {
            this.status = status;
            this.code = code;
            this.defaultMessage = defaultMessage;
        }
    }
}



