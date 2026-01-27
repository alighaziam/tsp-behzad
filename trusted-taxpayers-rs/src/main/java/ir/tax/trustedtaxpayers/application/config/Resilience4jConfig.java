package ir.tax.trustedtaxpayers.application.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Resilience4j components (Retry, CircuitBreaker)
 * این کانفیگ برای مدیریت retry و circuit breaker در سرویس‌های حساس استفاده می‌شود
 */
@Configuration
@RequiredArgsConstructor
public class Resilience4jConfig {

    private final RetryRegistry retryRegistry;
    private final CircuitBreakerRegistry circuitBreakerRegistry;

    @Bean("taxpayerSystemRetry")
    public Retry taxpayerSystemRetry() {
        return retryRegistry.retry("taxpayerSystemClient");
    }

    @Bean("taxpayerSystemCircuitBreaker")
    public CircuitBreaker taxpayerSystemCircuitBreaker() {
        return circuitBreakerRegistry.circuitBreaker("taxpayerSystemClient");
    }

    @Bean("grpcClientRetry")
    public Retry grpcClientRetry() {
        return retryRegistry.retry("grpcClient");
    }

    @Bean("grpcClientCircuitBreaker")
    public CircuitBreaker grpcClientCircuitBreaker() {
        return circuitBreakerRegistry.circuitBreaker("grpcClient");
    }
}

