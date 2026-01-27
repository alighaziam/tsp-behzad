package ir.tax.trustedtaxpayers.externalService.service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import ir.tax.trustedtaxpayers.externalService.integration.TaxpayerSystemClient;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

/**
 * Wrapper service که از Resilience4j برای retry و circuit breaker استفاده می‌کند
 * این سرویس می‌تواند جایگزین یا مکمل TaxpayerSystemClient باشد
 */
@Slf4j
@Service
public class ResilientTaxpayerSystemClient {

    private final TaxpayerSystemClient taxpayerSystemClient;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public ResilientTaxpayerSystemClient(TaxpayerSystemClient taxpayerSystemClient,
                                       RetryRegistry retryRegistry,
                                       CircuitBreakerRegistry circuitBreakerRegistry) {
        this.taxpayerSystemClient = taxpayerSystemClient;
        this.retry = retryRegistry.retry("taxpayerSystemClient");
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("taxpayerSystemClient");
    }

    /**
     * ارسال صورتحساب با retry و circuit breaker
     */
    public TaxpayerSystemClient.InvoiceSubmissionResponse submitInvoiceWithResilience(ElectronicInvoiceHeaderEntity headerEntity) {
        Supplier<TaxpayerSystemClient.InvoiceSubmissionResponse> supplier = Retry.decorateSupplier(
                retry,
                CircuitBreaker.decorateSupplier(
                        circuitBreaker,
                        () -> {
                            log.info("Attempting to submit invoice {} with resilience", headerEntity.getTaxId());
                            return taxpayerSystemClient.submitInvoice(headerEntity);
                        }
                )
        );

        try {
            return supplier.get();
        } catch (CallNotPermittedException e) {
            log.warn("Circuit breaker is OPEN, rejecting call for invoice {}", headerEntity.getTaxId());
            return TaxpayerSystemClient.InvoiceSubmissionResponse.failure(
                    headerEntity.getTaxId(),
                    "CIRCUIT_OPEN",
                    "Circuit breaker is open, request rejected"
            );
        } catch (Exception e) {
            log.error("Error submitting invoice {} after retries", headerEntity.getTaxId(), e);
            return TaxpayerSystemClient.InvoiceSubmissionResponse.failure(
                    headerEntity.getTaxId(),
                    "RESILIENCE_ERROR",
                    "Failed after retries: " + e.getMessage()
            );
        }
    }
}

