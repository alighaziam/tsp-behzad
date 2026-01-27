package ir.tax.trustedtaxpayers.application.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/**
 * Interceptor برای جمع‌آوری Metrics از HTTP Requests
 */
@Component
public class RequestMetricsInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(RequestMetricsInterceptor.class);
    
    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> requestCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> errorCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> requestTimers = new ConcurrentHashMap<>();

    public RequestMetricsInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute("startTime", System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        try {
            Long startTime = (Long) request.getAttribute("startTime");
            if (startTime == null) {
                return;
            }

            long duration = System.currentTimeMillis() - startTime;
            String method = request.getMethod();
            String path = getPathPattern(request);
            int status = response.getStatus();
            String statusCategory = getStatusCategory(status);

            // Record request count
            String counterKey = method + ":" + path;
            Counter counter = requestCounters.computeIfAbsent(counterKey, key ->
                Counter.builder("http.requests.total")
                    .description("Total HTTP requests")
                    .tag("method", method)
                    .tag("path", path)
                    .tag("status", String.valueOf(status))
                    .tag("status.category", statusCategory)
                    .register(meterRegistry)
            );
            counter.increment();

            // Record error count for 4xx and 5xx
            if (status >= 400) {
                String errorKey = method + ":" + path + ":" + statusCategory;
                Counter errorCounter = errorCounters.computeIfAbsent(errorKey, key ->
                    Counter.builder("http.requests.errors")
                        .description("HTTP request errors")
                        .tag("method", method)
                        .tag("path", path)
                        .tag("status", String.valueOf(status))
                        .tag("status.category", statusCategory)
                        .register(meterRegistry)
                );
                errorCounter.increment();
            }

            // Record request duration
            String timerKey = method + ":" + path;
            Timer timer = requestTimers.computeIfAbsent(timerKey, key ->
                Timer.builder("http.request.duration")
                    .description("HTTP request duration")
                    .tag("method", method)
                    .tag("path", path)
                    .tag("status", String.valueOf(status))
                    .tag("status.category", statusCategory)
                    .register(meterRegistry)
            );
            timer.record(duration, java.util.concurrent.TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            logger.warn("Error recording request metrics", e);
        }
    }

    private String getPathPattern(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Simplify path patterns (remove IDs, etc.)
        if (path.startsWith("/api/")) {
            // Replace UUIDs and numbers with placeholders
            path = path.replaceAll("/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "/{id}");
            path = path.replaceAll("/\\d+", "/{id}");
        }
        return path;
    }

    private String getStatusCategory(int status) {
        if (status >= 200 && status < 300) return "2xx";
        if (status >= 300 && status < 400) return "3xx";
        if (status >= 400 && status < 500) return "4xx";
        if (status >= 500) return "5xx";
        return "unknown";
    }
}

