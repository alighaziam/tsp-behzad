package ir.gov.tax.tpis.sdk.transfer.http;

import ir.gov.tax.tpis.sdk.transfer.exception.TaxApiException;

import static ir.gov.tax.tpis.sdk.transfer.http.HttpConstants.*;

/**
 * Utility class for validating HTTP responses.
 * <p>
 * This class provides static methods to ensure HTTP responses meet expected criteria,
 * throwing appropriate exceptions when validation fails.
 */
public final class HttpResponseValidator {

    private HttpResponseValidator() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Validates that the HTTP response has a successful status code (2xx range).
     * <p>
     * If the response is not successful, a {@link TaxApiException} is thrown with
     * an appropriate error message based on the status code.
     *
     * @param response the HTTP response to validate
     * @throws TaxApiException if the response status code is not in the 2xx range
     */
    public static void ensureSuccess(HttpResponse response) {
        int status = response.getStatus();
        if (status >= HTTP_STATUS_OK_MIN && status <= HTTP_STATUS_OK_MAX) {
            return;
        }

        String message = getErrorMessage(status);
        String body = response.getBody();

        if (body != null && !body.isEmpty()) {
            message = message + ": " + body;
        }

        throw new TaxApiException(message);
    }

    /**
     * Gets an error message based on the HTTP status code.
     *
     * @param status the HTTP status code
     * @return the error message
     */
    private static String getErrorMessage(int status) {
        if (status == HTTP_STATUS_REQUEST_TIMEOUT) {
            return ERROR_REQUEST_TIMEOUT_NETWORK;
        } else if (status == HTTP_STATUS_TOO_MANY_REQUESTS) {
            return ERROR_TOO_MANY_REQUESTS;
        } else if (status >= HTTP_STATUS_CLIENT_ERROR_MIN && status <= HTTP_STATUS_CLIENT_ERROR_MAX) {
            return ERROR_CLIENT_ERROR;
        } else if (status >= HTTP_STATUS_SERVER_ERROR_MIN && status <= HTTP_STATUS_SERVER_ERROR_MAX) {
            return ERROR_SERVER_ERROR;
        } else {
            return ERROR_UNEXPECTED_HTTP_STATUS;
        }
    }
}

