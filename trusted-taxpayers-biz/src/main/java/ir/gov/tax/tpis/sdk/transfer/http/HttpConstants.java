package ir.gov.tax.tpis.sdk.transfer.http;

/**
 * Constants for HTTP status codes and related values used in HTTP communication.
 */
public final class HttpConstants {

    private HttpConstants() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // HTTP Status Code Ranges
    public static final int HTTP_STATUS_OK_MIN = 200;
    public static final int HTTP_STATUS_OK_MAX = 299;
    public static final int HTTP_STATUS_CLIENT_ERROR_MIN = 400;
    public static final int HTTP_STATUS_CLIENT_ERROR_MAX = 499;
    public static final int HTTP_STATUS_SERVER_ERROR_MIN = 500;
    public static final int HTTP_STATUS_SERVER_ERROR_MAX = 599;

    // Specific HTTP Status Codes
    public static final int HTTP_STATUS_REQUEST_TIMEOUT = 408;
    public static final int HTTP_STATUS_TOO_MANY_REQUESTS = 429;
    public static final int HTTP_STATUS_SSL_HANDSHAKE_FAILED = 496; // Custom status for SSL errors

    // Content Types
    public static final String CONTENT_TYPE_JSON = "application/json; charset=utf-8";

    // Error Messages
    public static final String ERROR_JSON_SERIALIZATION = "error.in.pars.object.to.json";
    public static final String ERROR_HTTP_REQUEST_FAILED = "http request failed.";
    public static final String ERROR_HTTP_REQUEST_FAILED_WITH_URL = "http request failed with url {0}.";
    public static final String ERROR_RETRY_INTERRUPTED = "retry interrupted";
    public static final String ERROR_REQUEST_TIMEOUT_NETWORK = "request timeout / network error";
    public static final String ERROR_TOO_MANY_REQUESTS = "too many requests";
    public static final String ERROR_CLIENT_ERROR = "client error";
    public static final String ERROR_SERVER_ERROR = "server error";
    public static final String ERROR_UNEXPECTED_HTTP_STATUS = "unexpected http status";
    public static final String ERROR_NOT_IMPLEMENTED = "Not Implemented";

    // Timeouts (in minutes)
    public static final int DEFAULT_CONNECT_TIMEOUT_MINUTES = 1;

    // Retry Configuration
    public static final double DEFAULT_BACKOFF_MULTIPLIER = 2.0;
}

