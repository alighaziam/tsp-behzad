package ir.tax.trustedtaxpayers.interfaces.rest.dto;

public class BaseApiResponse<T> {
    private boolean success;
    private String message;
    private String traceId;
    private T data;

    public BaseApiResponse() {}

    public BaseApiResponse(boolean success, String message, String traceId, T data) {
        this.success = success;
        this.message = message;
        this.traceId = traceId;
        this.data = data;
    }

    public static <T> BaseApiResponse<T> ok(T data) {
        return new BaseApiResponse<>(true, "Success", null, data);
    }

    public static <T> BaseApiResponse<T> ok(String message, T data) {
        return new BaseApiResponse<>(true, message, null, data);
    }

    public static <T> BaseApiResponse<T> error(String message, String traceId) {
        return new BaseApiResponse<>(false, message, traceId, null);
    }

    public static <T> BaseApiResponse<T> error(String message, T data) {
        return new BaseApiResponse<>(false, message, null, data);
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}


