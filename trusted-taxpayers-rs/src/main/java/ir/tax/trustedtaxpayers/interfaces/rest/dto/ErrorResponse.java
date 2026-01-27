package ir.tax.trustedtaxpayers.interfaces.rest.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "پاسخ خطای سیستم - شامل کد خطا، پیام فارسی و اطلاعات مربوط به خطا")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    @Schema(description = "زمان بروز خطا", example = "2024-01-15T10:30:00")
    private LocalDateTime timestamp;
    
    @Schema(description = "کد وضعیت HTTP", example = "404", required = true)
    private int status;
    
    @Schema(description = "کد خطای سیستم (انگلیسی)", example = "ENTITY_NOT_FOUND", required = true)
    private String code;
    
    @Schema(description = "نوع خطا (مشابه کد خطا)", example = "ENTITY_NOT_FOUND")
    private String error;
    
    @Schema(description = "پیام خطا به فارسی", example = "رکورد مورد نظر یافت نشد", required = true)
    private String message;
    
    @Schema(description = "مسیر درخواست که باعث خطا شده است", example = "/api/taxpayers/123")
    private String path;

    @Schema(description = "جزئیات خطا (برای نمایش خطاهای اعتبارسنجی فیلدها)")
    private Map<String, String> details;

    public ErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ErrorResponse(int status, String error, String message, String code, String path) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.code = code;
        this.path = path;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Map<String, String> getDetails() {
        return details;
    }

    public void setDetails(Map<String, String> details) {
        this.details = details;
    }

    public static ErrorResponseBuilder builder() {
        return new ErrorResponseBuilder();
    }

    public static class ErrorResponseBuilder {
        private LocalDateTime timestamp = LocalDateTime.now();
        private int status;
        private String error;
        private String message;
        private String code;
        private String path;
        private Map<String, String> details;

        public ErrorResponseBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public ErrorResponseBuilder status(int status) {
            this.status = status;
            return this;
        }

        public ErrorResponseBuilder error(String error) {
            this.error = error;
            return this;
        }

        public ErrorResponseBuilder message(String message) {
            this.message = message;
            return this;
        }

        public ErrorResponseBuilder code(String code) {
            this.code = code;
            return this;
        }

        public ErrorResponseBuilder path(String path) {
            this.path = path;
            return this;
        }

        public ErrorResponseBuilder details(Map<String, String> details) {
            this.details = details;
            return this;
        }

        public ErrorResponse build() {
            ErrorResponse response = new ErrorResponse();
            response.setTimestamp(timestamp);
            response.setStatus(status);
            response.setError(error);
            response.setMessage(message);
            response.setCode(code);
            response.setPath(path);
            response.setDetails(details);
            return response;
        }
    }
}


