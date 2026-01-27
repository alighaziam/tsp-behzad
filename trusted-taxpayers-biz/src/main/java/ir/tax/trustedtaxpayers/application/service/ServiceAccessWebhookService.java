package ir.tax.trustedtaxpayers.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ServiceRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Service for sending webhooks for service access events
 * سرویس برای ارسال webhook برای رویدادهای دسترسی به خدمات
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceAccessWebhookService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ServiceRepository serviceRepository;
    private final TaxpayerRepository taxpayerRepository;

    /**
     * Send webhook for service access event
     * ارسال webhook برای رویداد دسترسی به خدمات
     */
    @Async
    public CompletableFuture<Void> sendServiceAccessWebhook(
            String webhookUrl,
            UUID taxpayerId,
            String serviceCode,
            String eventType,
            Map<String, Object> data) {
        
        return CompletableFuture.runAsync(() -> {
            try {
                Map<String, Object> payload = new HashMap<>();
                payload.put("eventType", eventType);
                payload.put("timestamp", LocalDateTime.now().toString());
                payload.put("taxpayerId", taxpayerId != null ? taxpayerId.toString() : null);
                payload.put("serviceCode", serviceCode);
                payload.put("data", data);

                // Get service info
                serviceRepository.findByCode(serviceCode).ifPresent(service -> {
                    Map<String, Object> serviceInfo = new HashMap<>();
                    serviceInfo.put("id", service.getId().toString());
                    serviceInfo.put("code", service.getCode());
                    serviceInfo.put("name", service.getName());
                    serviceInfo.put("category", service.getCategory());
                    payload.put("service", serviceInfo);
                });

                // Get taxpayer info
                if (taxpayerId != null) {
                    taxpayerRepository.findById(taxpayerId).ifPresent(taxpayer -> {
                        Map<String, Object> taxpayerInfo = new HashMap<>();
                        taxpayerInfo.put("id", taxpayer.getId().toString());
                        taxpayerInfo.put("name", taxpayer.getName());
                        taxpayerInfo.put("taxpayerNo", taxpayer.getTaxId());
                        payload.put("taxpayer", taxpayerInfo);
                    });
                }

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("User-Agent", "TrustedTaxpayers-ServiceAccess/1.0");

                HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

                ResponseEntity<String> response = restTemplate.postForEntity(
                        webhookUrl,
                        request,
                        String.class
                );

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.debug("Webhook sent successfully to {} for event {}: {}", 
                            webhookUrl, eventType, serviceCode);
                } else {
                    log.warn("Webhook sent with non-2xx status to {} for event {}: {} - Status: {}", 
                            webhookUrl, eventType, serviceCode, response.getStatusCode());
                }
            } catch (Exception e) {
                log.error("Failed to send webhook to {} for event {}: {}", 
                        webhookUrl, eventType, serviceCode, e);
                // Don't throw exception - webhook failures shouldn't break the flow
            }
        });
    }

    /**
     * Send webhook for quota alert
     * ارسال webhook برای هشدار سهمیه
     */
    @Async
    public CompletableFuture<Void> sendQuotaAlertWebhook(
            String webhookUrl,
            UUID taxpayerId,
            String serviceCode,
            ServiceQuotaInfo quota) {
        
        Map<String, Object> data = new HashMap<>();
        data.put("used", quota.used());
        data.put("limit", quota.limitValue());
        data.put("remaining", quota.remaining());
        data.put("usagePercentage", quota.usagePercentage());
        data.put("status", quota.status());
        data.put("limitUnit", quota.limitUnit());

        return sendServiceAccessWebhook(
                webhookUrl,
                taxpayerId,
                serviceCode,
                "QUOTA_ALERT",
                data
        );
    }

    /**
     * Send webhook for limit exceeded
     * ارسال webhook برای تجاوز از محدودیت
     */
    @Async
    public CompletableFuture<Void> sendLimitExceededWebhook(
            String webhookUrl,
            UUID taxpayerId,
            String serviceCode,
            ServiceQuotaInfo quota) {
        
        Map<String, Object> data = new HashMap<>();
        data.put("used", quota.used());
        data.put("limit", quota.limitValue());
        data.put("remaining", quota.remaining());
        data.put("usagePercentage", quota.usagePercentage());
        data.put("status", quota.status());
        data.put("limitUnit", quota.limitUnit());

        return sendServiceAccessWebhook(
                webhookUrl,
                taxpayerId,
                serviceCode,
                "LIMIT_EXCEEDED",
                data
        );
    }

    /**
     * Send webhook for service access granted
     * ارسال webhook برای دسترسی موفق
     */
    @Async
    public CompletableFuture<Void> sendAccessGrantedWebhook(
            String webhookUrl,
            UUID taxpayerId,
            String serviceCode) {
        
        return sendServiceAccessWebhook(
                webhookUrl,
                taxpayerId,
                serviceCode,
                "ACCESS_GRANTED",
                Map.of()
        );
    }

    /**
     * Send webhook for service access denied
     * ارسال webhook برای دسترسی رد شده
     */
    @Async
    public CompletableFuture<Void> sendAccessDeniedWebhook(
            String webhookUrl,
            UUID taxpayerId,
            String serviceCode,
            String reason) {
        
        return sendServiceAccessWebhook(
                webhookUrl,
                taxpayerId,
                serviceCode,
                "ACCESS_DENIED",
                Map.of("reason", reason != null ? reason : "Unknown")
        );
    }

    /**
     * Send webhook for service access alert
     * ارسال webhook برای هشدار دسترسی به خدمات
     * This is a convenience method that can be used from ServiceAccessAlertService
     */
    public void sendServiceAccessAlert(
            UUID taxpayerId,
            String serviceCode,
            String eventType,
            String status,
            Map<String, Object> metadata) {
        
        // Get webhook URL from configuration
        // For now, we'll log the alert instead of sending webhook
        // In production, this should be configured via application properties
        String webhookUrl = System.getProperty("service.access.webhook.url");
        
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Webhook URL not configured, skipping webhook for event: {}", eventType);
            return;
        }
        
        Map<String, Object> data = new HashMap<>();
        if (metadata != null) {
            data.putAll(metadata);
        }
        data.put("status", status);
        data.put("eventType", eventType);
        
        sendServiceAccessWebhook(
                webhookUrl,
                taxpayerId,
                serviceCode,
                eventType,
                data
        );
    }
}

