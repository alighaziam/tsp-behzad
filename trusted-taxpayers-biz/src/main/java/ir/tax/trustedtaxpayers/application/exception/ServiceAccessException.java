package ir.tax.trustedtaxpayers.application.exception;

/**
 * Exception thrown when a taxpayer doesn't have access to a required service
 * or when service limits are exceeded.
 */
public class ServiceAccessException extends RuntimeException {
    
    private final String serviceCode;
    private final String reason;
    
    public ServiceAccessException(String message) {
        super(message);
        this.serviceCode = null;
        this.reason = null;
    }
    
    public ServiceAccessException(String message, String serviceCode) {
        super(message);
        this.serviceCode = serviceCode;
        this.reason = null;
    }
    
    public ServiceAccessException(String message, String serviceCode, String reason) {
        super(message);
        this.serviceCode = serviceCode;
        this.reason = reason;
    }
    
    public ServiceAccessException(String message, Throwable cause) {
        super(message, cause);
        this.serviceCode = null;
        this.reason = null;
    }
    
    public String getServiceCode() {
        return serviceCode;
    }
    
    public String getReason() {
        return reason;
    }
}

