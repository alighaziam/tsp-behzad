package ir.tax.trustedtaxpayers.application.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Metrics for service access system
 * متریک‌های سیستم مدیریت دسترسی به سرویس‌ها
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceAccessMetrics {

    private final MeterRegistry meterRegistry;
    private final Map<String, Counter> accessCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> deniedCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> limitExceededCounters = new ConcurrentHashMap<>();
    private final Map<String, Timer> accessTimers = new ConcurrentHashMap<>();
    private final Map<String, Double> quotaPercentages = new ConcurrentHashMap<>();

    /**
     * Record service access attempt
     */
    public void recordAccess(UUID taxpayerId, String serviceCode, boolean granted, long durationMs) {
        String key = serviceCode;
        
        // Record access counter
        Counter accessCounter = accessCounters.computeIfAbsent(
                key,
                k -> Counter.builder("service.access.attempts")
                        .tag("service", serviceCode)
                        .description("Number of service access attempts")
                        .register(meterRegistry)
        );
        accessCounter.increment();
        
        // Record access timer
        Timer accessTimer = accessTimers.computeIfAbsent(
                key,
                k -> Timer.builder("service.access.duration")
                        .tag("service", serviceCode)
                        .description("Service access check duration")
                        .register(meterRegistry)
        );
        accessTimer.record(durationMs, TimeUnit.MILLISECONDS);
        
        if (granted) {
            // Record granted access
            Counter grantedCounter = Counter.builder("service.access.granted")
                    .tag("service", serviceCode)
                    .tag("taxpayer", taxpayerId.toString())
                    .description("Number of granted service accesses")
                    .register(meterRegistry);
            grantedCounter.increment();
        } else {
            // Record denied access
            Counter deniedCounter = deniedCounters.computeIfAbsent(
                    key,
                    k -> Counter.builder("service.access.denied")
                            .tag("service", serviceCode)
                            .description("Number of denied service accesses")
                            .register(meterRegistry)
            );
            deniedCounter.increment();
        }
    }

    /**
     * Record limit exceeded
     */
    public void recordLimitExceeded(UUID taxpayerId, String serviceCode) {
        String key = serviceCode;
        Counter limitExceededCounter = limitExceededCounters.computeIfAbsent(
                key,
                k -> Counter.builder("service.access.limit_exceeded")
                        .tag("service", serviceCode)
                        .description("Number of limit exceeded attempts")
                        .register(meterRegistry)
        );
        limitExceededCounter.increment();
        
        // Also record per taxpayer
        Counter perTaxpayerCounter = Counter.builder("service.access.limit_exceeded")
                .tag("service", serviceCode)
                .tag("taxpayer", taxpayerId.toString())
                .description("Number of limit exceeded attempts per taxpayer")
                .register(meterRegistry);
        perTaxpayerCounter.increment();
    }

    /**
     * Record service usage
     */
    public void recordUsage(UUID taxpayerId, String serviceCode, int usageCount) {
        Counter usageCounter = Counter.builder("service.usage.total")
                .tag("service", serviceCode)
                .tag("taxpayer", taxpayerId.toString())
                .description("Total service usage count")
                .register(meterRegistry);
        usageCounter.increment(usageCount);
    }

    /**
     * Record quota percentage
     */
    public void recordQuotaPercentage(UUID taxpayerId, String serviceCode, double percentage) {
        String key = serviceCode + ":" + taxpayerId.toString();
        quotaPercentages.put(key, percentage);
        
        // Register or update gauge
        Gauge.builder("service.quota.percentage", quotaPercentages, map -> map.getOrDefault(key, 0.0))
                .tag("service", serviceCode)
                .tag("taxpayer", taxpayerId.toString())
                .description("Service quota usage percentage")
                .register(meterRegistry);
    }
}

