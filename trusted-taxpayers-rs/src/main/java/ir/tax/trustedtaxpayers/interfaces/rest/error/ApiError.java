package ir.tax.trustedtaxpayers.interfaces.rest.error;

import java.time.OffsetDateTime;

public class ApiError {
    private String code;
    private String message;
    private String path;
    private String traceId;
    private OffsetDateTime timestamp = OffsetDateTime.now();

    public ApiError() {}

    public ApiError(String code, String message, String path, String traceId) {
        this.code = code; this.message = message; this.path = path; this.traceId = traceId;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public OffsetDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(OffsetDateTime timestamp) { this.timestamp = timestamp; }
}


