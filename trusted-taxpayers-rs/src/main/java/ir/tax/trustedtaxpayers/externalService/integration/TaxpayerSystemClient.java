package ir.tax.trustedtaxpayers.externalService.integration;

import ir.tax.trustedtaxpayers.externalService.config.TaxpayerSystemConfigProperties;
import ir.tax.trustedtaxpayers.infrastructure.jpa.entity.ElectronicInvoiceHeaderEntity;
import ir.tax.trustedtaxpayers.application.security.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Client for integrating with external Taxpayer System
 * کلاینت برای ارتباط با سامانه مودیان خارجی
 * 
 * این کلاس مسئولیت ارسال صورتحساب‌ها به سامانه مودیان و دریافت وضعیت ارسال را دارد.
 * 
 * @author Trusted Taxpayers Team
 * @since 1.0
 */
@Component
public class TaxpayerSystemClient {
    
    private static final Logger logger = LoggerFactory.getLogger(TaxpayerSystemClient.class);
    
    private final RestTemplate restTemplate;
    private final TaxpayerSystemConfigProperties config;
    private final MeterRegistry meterRegistry;
    private final TokenService tokenService;
    
    // Optional SDK TaxApi bean wired reflectively (see TpisSdkConfig)
    @Autowired(required = false)
    @Qualifier("tpisTaxApi")
    private Object tpisTaxApi;
    
    public TaxpayerSystemClient(RestTemplate restTemplate, TaxpayerSystemConfigProperties config, MeterRegistry meterRegistry, TokenService tokenService) {
        this.restTemplate = restTemplate;
        this.config = config;
        this.meterRegistry = meterRegistry;
        this.tokenService = tokenService;
    }
    
    /**
     * ارسال صورتحساب به سامانه مودیان با retry logic
     * 
     * @param headerEntity Entity صورتحساب الکترونیکی
     * @return پاسخ سامانه شامل وضعیت ارسال
     */
    public InvoiceSubmissionResponse submitInvoice(ElectronicInvoiceHeaderEntity headerEntity) {
        String correlationId = java.util.UUID.randomUUID().toString();
        MDC.put("corrId", correlationId);
        Timer.Sample timerSample = Timer.start(meterRegistry);
        String url = config.getBaseUrl() + config.getApiPath();
        
        logger.info("Sending invoice {} to taxpayer system at {}", headerEntity.getTaxId(), url);
        
        int maxAttempts = config.getRetry().getMaxAttempts();
        int backoffDelay = config.getRetry().getBackoffDelay();
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                // آماده‌سازی درخواست
                HttpHeaders headers = prepareHeaders();
                Map<String, Object> requestBody = buildInvoiceRequest(headerEntity);
                HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
                
                // ارسال درخواست
                logger.debug("Attempt {}/{} for invoice {}", attempt, maxAttempts, headerEntity.getTaxId());
                ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    request,
                    new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {}
                );
                
                // پردازش پاسخ موفق
                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    logger.info("Invoice {} submitted successfully on attempt {}", headerEntity.getTaxId(), attempt);
                    timerSample.stop(Timer.builder("taxpayer.submit.duration")
                        .tags(Tags.of("status", "success"))
                        .register(meterRegistry));
                    meterRegistry.counter("taxpayer.submit.success").increment();
                    return parseSuccessResponse(response.getBody(), headerEntity.getTaxId());
                } else {
                    logger.warn("Invoice {} submission returned unexpected status: {}", 
                        headerEntity.getTaxId(), response.getStatusCode());
                    
                    // در آخرین تلاش، خطا را برمی‌گردانیم
                    if (attempt == maxAttempts) {
                        timerSample.stop(Timer.builder("taxpayer.submit.duration")
                            .tags(Tags.of("status", "unexpected_status"))
                            .register(meterRegistry));
                        meterRegistry.counter("taxpayer.submit.failure", "reason", "unexpected_status").increment();
                        return InvoiceSubmissionResponse.failure(
                            headerEntity.getTaxId(),
                            "UNEXPECTED_STATUS",
                            "Received unexpected status after " + maxAttempts + " attempts: " + response.getStatusCode()
                        );
                    }
                }
                
            } catch (RestClientException e) {
                logger.warn("Failed to submit invoice {} on attempt {}: {}", 
                    headerEntity.getTaxId(), attempt, e.getMessage());
                
                // در آخرین تلاش یا خطاهای غیرقابل retry، خطا را برمی‌گردانیم
                if (attempt == maxAttempts) {
                    logger.error("Failed to submit invoice {} after {} attempts", headerEntity.getTaxId(), maxAttempts, e);
                    timerSample.stop(Timer.builder("taxpayer.submit.duration")
                        .tags(Tags.of("status", "transmission_error"))
                        .register(meterRegistry));
                    meterRegistry.counter("taxpayer.submit.failure", "reason", "transmission_error").increment();
                    return InvoiceSubmissionResponse.failure(
                        headerEntity.getTaxId(),
                        "TRANSMISSION_ERROR",
                        "Failed to communicate with taxpayer system after " + maxAttempts + " attempts: " + e.getMessage()
                    );
                }
                
                // Backoff delay قبل از تلاش بعدی
                try {
                    Thread.sleep(backoffDelay * attempt); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    timerSample.stop(Timer.builder("taxpayer.submit.duration")
                        .tags(Tags.of("status", "interrupted"))
                        .register(meterRegistry));
                    meterRegistry.counter("taxpayer.submit.failure", "reason", "interrupted").increment();
                    return InvoiceSubmissionResponse.failure(
                        headerEntity.getTaxId(),
                        "TRANSMISSION_INTERRUPTED",
                        "Transmission interrupted during retry delay"
                    );
                }
            } catch (Exception e) {
                logger.error("Unexpected error submitting invoice {}", headerEntity.getTaxId(), e);
                timerSample.stop(Timer.builder("taxpayer.submit.duration")
                    .tags(Tags.of("status", "system_error"))
                    .register(meterRegistry));
                meterRegistry.counter("taxpayer.submit.failure", "reason", "system_error").increment();
                return InvoiceSubmissionResponse.failure(
                    headerEntity.getTaxId(),
                    "SYSTEM_ERROR",
                    "Unexpected system error: " + e.getMessage()
                );
            }
        }
        
        // نباید به اینجا برسد
        timerSample.stop(Timer.builder("taxpayer.submit.duration")
            .tags(Tags.of("status", "max_retries_exceeded"))
            .register(meterRegistry));
        meterRegistry.counter("taxpayer.submit.failure", "reason", "max_retries_exceeded").increment();
        MDC.remove("corrId");
        return InvoiceSubmissionResponse.failure(
            headerEntity.getTaxId(),
            "MAX_RETRIES_EXCEEDED",
            "Maximum retry attempts exceeded"
        );
    }
    
    /**
     * آماده‌سازی HTTP headers با اطلاعات احراز هویت
     */
    private HttpHeaders prepareHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // افزودن headerهای امنیتی در صورت وجود
        if (config.getClientId() != null && !config.getClientId().isBlank()) {
            headers.set("X-Client-Id", config.getClientId());
        }
        if (config.getOrgKeyId() != null && !config.getOrgKeyId().isBlank()) {
            headers.set("X-Org-Key-Id", config.getOrgKeyId());
        }
        
        // Prefer centralized token service (cached); falls back to no Authorization if unavailable
        String bearer = tokenService != null ? tokenService.getBearerToken() : null;
        if (bearer != null && !bearer.isBlank()) {
            headers.set("Authorization", "Bearer " + bearer);
        } else if (tpisTaxApi != null) {
            // Backward-compatible fallback (one-shot) if TokenService not available for any reason
            try {
                Object tokenModel = tpisTaxApi.getClass().getMethod("requestToken").invoke(tpisTaxApi);
                if (tokenModel != null) {
                    String token = (String) tokenModel.getClass().getMethod("getToken").invoke(tokenModel);
                    if (token != null && !token.isBlank()) {
                        headers.set("Authorization", "Bearer " + token);
                    }
                }
            } catch (Exception e) {
                logger.warn("Failed to obtain token from TPIS TaxApi; proceeding without Authorization header: {}", e.getMessage());
            }
        }
        
        return headers;
    }

    /**
     * دریافت توکن دسترسی (شِبه‌ساز برای توسعه)
     */
    public Map<String, Object> requestToken(String clientId) {
        Map<String, Object> token = new HashMap<>();
        token.put("clientId", clientId != null ? clientId : config.getClientId());
        token.put("token", java.util.UUID.randomUUID().toString());
        token.put("expiresIn", 3600);
        return token;
    }

    /**
     * استعلام وضعیت با UID (نیازمند تعریف Endpoint در سامانه مقصد)
     */
    public Map<String, Object> inquiryByUid(java.util.UUID uid) {
        Map<String, Object> m = new HashMap<>();
        m.put("uid", uid != null ? uid.toString() : null);
        m.put("status", "UNKNOWN");
        return m;
    }

    /**
     * استعلام وضعیت با referenceNumber (نیازمند تعریف Endpoint در سامانه مقصد)
     */
    public Map<String, Object> inquiryByReferenceNumber(String referenceNumber) {
        Map<String, Object> m = new HashMap<>();
        m.put("referenceNumber", referenceNumber);
        m.put("status", "UNKNOWN");
        return m;
    }

    /**
     * استعلام وضعیت‌ها در بازه زمانی (نیازمند تعریف Endpoint در سامانه مقصد)
     */
    public java.util.List<Map<String, Object>> inquiryByTimeRange(java.time.OffsetDateTime from,
                                                                 java.time.OffsetDateTime to) {
        return java.util.Collections.emptyList();
    }
    
    /**
     * ساخت body درخواست برای ارسال صورتحساب
     */
    private Map<String, Object> buildInvoiceRequest(ElectronicInvoiceHeaderEntity header) {
        Map<String, Object> request = new HashMap<>();

        // helper to avoid including nulls
        java.util.function.BiConsumer<String, Object> putIfPresent = (k, v) -> { if (v != null) request.put(k, v); };

        // اطلاعات اصلی صورتحساب (null-safe)
        putIfPresent.accept("taxId", header.getTaxId());
        putIfPresent.accept("templateCode", header.getTemplateCode());
        putIfPresent.accept("templateSubtype", header.getTemplateSubtype());
        putIfPresent.accept("inty", header.getInty() != null ? header.getInty().getValue() : null);
        putIfPresent.accept("inno", header.getInno());
        putIfPresent.accept("indatim", header.getIndatim());
        putIfPresent.accept("inp", header.getInp());
        putIfPresent.accept("ins", header.getIns() != null ? header.getIns().getValue() : null);
        putIfPresent.accept("irtaxid", header.getIrtaxid());

        // اطلاعات فروشنده
        putIfPresent.accept("tins", header.getTins());
        putIfPresent.accept("sbc", header.getSbc());

        // اطلاعات خریدار
        putIfPresent.accept("tob", header.getTob() != null ? header.getTob().getValue() : null);
        putIfPresent.accept("bid", header.getBid());
        putIfPresent.accept("tinb", header.getTinb());
        putIfPresent.accept("bbc", header.getBbc());
        putIfPresent.accept("bpc", header.getBpc());
        putIfPresent.accept("bpn", header.getBpn());

        // اطلاعات اختصاصی الگوها
        putIfPresent.accept("ft", header.getFt() != null ? header.getFt().getValue() : null);
        putIfPresent.accept("scln", header.getScln());
        putIfPresent.accept("scc", header.getScc());
        putIfPresent.accept("cdcn", header.getCdcn());
        putIfPresent.accept("cdcd", header.getCdcd());
        putIfPresent.accept("crn", header.getCrn());
        putIfPresent.accept("billid", header.getBillid());
        putIfPresent.accept("billingId", header.getBillingId());
        putIfPresent.accept("tonw", header.getTonw());
        putIfPresent.accept("torv", header.getTorv());
        putIfPresent.accept("tocv", header.getTocv());
        putIfPresent.accept("tinc", header.getTinc());

        // اطلاعات مالی
        putIfPresent.accept("tprdis", header.getTprdis());
        putIfPresent.accept("tdis", header.getTdis());
        putIfPresent.accept("tadis", header.getTadis());
        putIfPresent.accept("tvam", header.getTvam());
        putIfPresent.accept("todam", header.getTodam());
        putIfPresent.accept("tbill", header.getTbill());

        return request;
    }
    
    /**
     * پردازش پاسخ موفق از سامانه
     */
    private InvoiceSubmissionResponse parseSuccessResponse(Map<String, Object> responseBody, String taxId) {
        String status = (String) responseBody.getOrDefault("status", "ACCEPTED");
        String message = (String) responseBody.getOrDefault("message", "Invoice submitted successfully");
        String rejectionCode = (String) responseBody.get("rejectionCode");
        String rejectionDescription = (String) responseBody.get("rejectionDescription");
        
        return new InvoiceSubmissionResponse(
            taxId,
            status,
            message,
            rejectionCode,
            rejectionDescription
        );
    }
    
    /**
     * پاسخ ارسال صورتحساب
     */
    public static class InvoiceSubmissionResponse {
        private final String taxId;
        private final String status;
        private final String message;
        private final String rejectionCode;
        private final String rejectionDescription;
        
        private InvoiceSubmissionResponse(String taxId, String status, String message, 
                                         String rejectionCode, String rejectionDescription) {
            this.taxId = taxId;
            this.status = status;
            this.message = message;
            this.rejectionCode = rejectionCode;
            this.rejectionDescription = rejectionDescription;
        }
        
        public static InvoiceSubmissionResponse success(String taxId, String status, String message) {
            return new InvoiceSubmissionResponse(taxId, status, message, null, null);
        }
        
        public static InvoiceSubmissionResponse failure(String taxId, String rejectionCode, String rejectionDescription) {
            return new InvoiceSubmissionResponse(taxId, "REJECTED", "Submission failed", 
                rejectionCode, rejectionDescription);
        }
        
        public boolean isSuccessful() {
            return "ACCEPTED".equalsIgnoreCase(status) || "SUBMITTED".equalsIgnoreCase(status);
        }
        
        public String getTaxId() {
            return taxId;
        }
        
        public String getStatus() {
            return status;
        }
        
        public String getMessage() {
            return message;
        }
        
        public String getRejectionCode() {
            return rejectionCode;
        }
        
        public String getRejectionDescription() {
            return rejectionDescription;
        }
    }
}

