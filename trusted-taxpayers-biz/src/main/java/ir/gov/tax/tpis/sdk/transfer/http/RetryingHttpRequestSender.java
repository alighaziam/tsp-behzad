package ir.gov.tax.tpis.sdk.transfer.http;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static ir.gov.tax.tpis.sdk.transfer.http.HttpConstants.*;

/**
 * Decorator implementation of {@link HttpRequestSender} that adds retry logic
 * with exponential backoff for failed requests.
 * <p>
 * This class wraps another {@link HttpRequestSender} and automatically retries
 * requests when they fail with configured HTTP status codes. Retries use an
 * exponential backoff strategy with a configurable multiplier.
 * <p>
 * توضیح فارسی: این کلاس به عنوان یک دکوراتور برای {@link HttpRequestSender}
 * عمل می‌کند و منطق تلاش مجدد (Retry) با تاخیر نمایی را اضافه می‌کند. زمانی که
 * پاسخ با کدهای وضعیت مشخص‌شده شکست بخورد، درخواست به صورت خودکار و تا حداکثر
 * تعداد تلاش‌های تعیین‌شده تکرار می‌شود. تاخیر بین تلاش‌ها بر اساس ضریب رشد
 * نمایی (backoffMultiplier) افزایش پیدا می‌کند.
 */
public class RetryingHttpRequestSender implements HttpRequestSender {

    private static final Logger logger = Logger.getLogger(RetryingHttpRequestSender.class.getName());

    private final HttpRequestSender delegate;
    private final int maxAttempts;
    private final long initialDelayMs;
    private final double backoffMultiplier;
    private final Set<Integer> retryOnStatuses;

    /**
     * Creates a new RetryingHttpRequestSender with the specified configuration.
     *
     * @param delegate the underlying HttpRequestSender to wrap
     * @param maxAttempts the maximum number of attempts (minimum 1)
     * @param initialDelayMs the initial delay before first retry in milliseconds (minimum 0)
     * @param backoffMultiplier the multiplier for exponential backoff (default 2.0 if <= 0)
     * @param retryOnStatuses the set of HTTP status codes that should trigger retries.
     *                        If null, default retry statuses are used (408, 429, 5xx)
     */
    // توضیح فارسی: با استفاده از این سازنده می‌توانید تعداد تلاش‌ها، تاخیر اولیه،
    // ضریب رشد تاخیر و لیست کدهای وضعیت مشمول تلاش مجدد را تنظیم کنید.
    public RetryingHttpRequestSender(HttpRequestSender delegate,
                                     int maxAttempts,
                                     long initialDelayMs,
                                     double backoffMultiplier,
                                     Set<Integer> retryOnStatuses) {
        this.delegate = delegate;
        this.maxAttempts = Math.max(1, maxAttempts);
        this.initialDelayMs = Math.max(0, initialDelayMs);
        this.backoffMultiplier = backoffMultiplier <= 0 ? DEFAULT_BACKOFF_MULTIPLIER : backoffMultiplier;
        this.retryOnStatuses = retryOnStatuses == null ? defaultRetryStatuses() : new HashSet<>(retryOnStatuses);
    }

    /**
     * Creates the default set of HTTP status codes that should trigger retries.
     * <p>
     * Default retry statuses include:
     * <ul>
     *   <li>408 (Request Timeout / network error)</li>
     *   <li>429 (Too Many Requests)</li>
     *   <li>500-599 (Server errors)</li>
     * </ul>
     *
     * @return the default set of retry status codes
     */
    private Set<Integer> defaultRetryStatuses() {
        Set<Integer> defaults = new HashSet<>();
        defaults.add(HTTP_STATUS_REQUEST_TIMEOUT);
        defaults.add(HTTP_STATUS_TOO_MANY_REQUESTS);
        for (int status = HTTP_STATUS_SERVER_ERROR_MIN; status <= HTTP_STATUS_SERVER_ERROR_MAX; status++) {
            defaults.add(status);
        }
        return defaults;
    }

    @Override
    public HttpResponse sendPostRequest(@NotNull String url, Object requestBody, @Nullable Map<String, String> headers) {
        return executeWithRetry(() -> delegate.sendPostRequest(url, requestBody, headers));
    }

    @Override
    public HttpResponse sendGetRequest(@NotNull String url, @Nullable Map<String, String> headers) {
        return executeWithRetry(() -> delegate.sendGetRequest(url, headers));
    }

    /**
     * Executes a request with retry logic and exponential backoff.
     *
     * @param supplier the supplier that executes the HTTP request
     * @return the HttpResponse from the successful attempt or the last failed attempt
     */
    // توضیح فارسی: این متد یک درخواست را اجرا می‌کند و در صورت نیاز تا رسیدن به موفقیت
    // یا پایان تعداد تلاش‌های مجاز آن را با تاخیر نمایی تکرار می‌کند.
    private HttpResponse executeWithRetry(SupplierWithHttpResponse supplier) {
        int attempt = 1;
        long delay = initialDelayMs;
        HttpResponse lastResponse = null;

        while (true) {
            lastResponse = supplier.get();

            if (!shouldRetry(lastResponse.getStatus(), attempt)) {
                return lastResponse;
            }

            if (attempt >= maxAttempts) {
                return lastResponse;
            }

            try {
                if (delay > 0) {
                    Thread.sleep(delay);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logger.log(Level.WARNING, ERROR_RETRY_INTERRUPTED, ie);
                return lastResponse;
            }

            attempt++;
            delay = (long) Math.ceil(delay * backoffMultiplier);
        }
    }

    /**
     * Determines whether a request should be retried based on the status code and attempt number.
     *
     * @param status the HTTP status code
     * @param attempt the current attempt number
     * @return true if the request should be retried
     */
    // توضیح فارسی: مشخص می‌کند با توجه به کد وضعیت پاسخ و شماره تلاش فعلی، آیا باید
    // درخواست تکرار شود یا خیر.
    private boolean shouldRetry(int status, int attempt) {
        return retryOnStatuses.contains(status);
    }

    /**
     * Functional interface for supplying an HttpResponse.
     */
    private interface SupplierWithHttpResponse {
        /**
         * Gets an HttpResponse, typically by executing an HTTP request.
         *
         * @return the HttpResponse
         */
        // توضیح فارسی: اجرای درخواست HTTP و بازگرداندن پاسخ آن.
        HttpResponse get();
    }
}

