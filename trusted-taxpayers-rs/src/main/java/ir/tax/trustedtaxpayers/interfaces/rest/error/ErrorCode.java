package ir.tax.trustedtaxpayers.interfaces.rest.error;

/**
 * Author: Behzad Farajpour
 * Project: TSP
 * Email: farajpour-b@hamfekrangroup.ir
 */

public enum ErrorCode {
    VALIDATION_FAILED("ERR-VAL-001"),
    RESOURCE_NOT_FOUND("ERR-RSC-404"),
    CONFLICT("ERR-CNF-409"),
    UNAUTHORIZED("ERR-AUT-401"),
    FORBIDDEN("ERR-FBD-403"),
    METHOD_NOT_ALLOWED("ERR-MTH-405"),
    RATE_LIMITED("ERR-RAT-429"),
    INTERNAL_ERROR("ERR-SRV-500");

    private final String code;
    ErrorCode(String code) { this.code = code; }
    public String code() { return code; }
}
