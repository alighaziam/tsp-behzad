package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ServiceRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Outbound webhook delivery is disabled by default. When explicitly enabled,
 * destinations must be HTTPS hosts in the configured allow-list and resolve
 * only to public IP addresses. Redirects are disabled.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceAccessWebhookService {

    private final RestTemplate restTemplate = createRestTemplate();
    private final ServiceRepository serviceRepository;
    private final TaxpayerRepository taxpayerRepository;

    @Value("${service.access.webhook.enabled:false}")
    private boolean enabled;

    @Value("${service.access.webhook.allowed-hosts:}")
    private String allowedHosts;

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) throws IOException {
                super.prepareConnection(connection, httpMethod);
                connection.setInstanceFollowRedirects(false);
            }
        };
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(10000);
        return new RestTemplate(factory);
    }

    @Async
    public CompletableFuture<Void> sendServiceAccessWebhook(
            String webhookUrl,
            UUID taxpayerId,
            String serviceCode,
            String eventType,
            Map<String, Object> data) {

        if (!enabled) {
            log.debug("Outbound service-access webhooks are disabled; skipping event {}", eventType);
            return CompletableFuture.completedFuture(null);
        }
        if (!isAllowedWebhookDestination(webhookUrl)) {
            log.warn("Blocked outbound webhook destination for event {}", eventType);
            return CompletableFuture.completedFuture(null);
        }

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("eventType", eventType);
            payload.put("timestamp", LocalDateTime.now().toString());
            payload.put("taxpayerId", taxpayerId != null ? taxpayerId.toString() : null);
            payload.put("serviceCode", serviceCode);
            payload.put("data", data);

            serviceRepository.findByCode(serviceCode).ifPresent(service -> {
                Map<String, Object> serviceInfo = new HashMap<>();
                serviceInfo.put("id", service.getId().toString());
                serviceInfo.put("code", service.getCode());
                serviceInfo.put("name", service.getName());
                serviceInfo.put("category", service.getCategory());
                payload.put("service", serviceInfo);
            });

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
            ResponseEntity<String> response = restTemplate.postForEntity(
                    webhookUrl, new HttpEntity<>(payload, headers), String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.debug("Webhook delivered for event {} and service {}", eventType, serviceCode);
            } else {
                log.warn("Webhook returned status {} for event {} and service {}",
                        response.getStatusCode(), eventType, serviceCode);
            }
        } catch (Exception e) {
            log.error("Webhook delivery failed for event {} and service {}", eventType, serviceCode, e);
        }
        return CompletableFuture.completedFuture(null);
    }

    boolean isAllowedWebhookDestination(String rawUrl) {
        if (!enabled || rawUrl == null || rawUrl.isBlank() || allowedHosts == null || allowedHosts.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(rawUrl.trim());
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme())
                    || host == null || host.isBlank()
                    || uri.getUserInfo() != null
                    || uri.getFragment() != null
                    || (uri.getPort() != -1 && uri.getPort() != 443)) {
                return false;
            }

            Set<String> allowList = Arrays.stream(allowedHosts.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
            if (!allowList.contains(host.toLowerCase())) {
                return false;
            }

            InetAddress[] addresses = InetAddress.getAllByName(host);
            return addresses.length > 0 && Arrays.stream(addresses).allMatch(this::isPublicAddress);
        } catch (Exception e) {
            log.warn("Unable to validate webhook destination", e);
            return false;
        }
    }

    private boolean isPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return false;
        }
        byte[] bytes = address.getAddress();
        if (bytes.length == 16) {
            return (bytes[0] & 0xFE) != 0xFC; // reject IPv6 unique-local fc00::/7
        }
        int a = bytes[0] & 0xFF;
        int b = bytes[1] & 0xFF;
        int c = bytes[2] & 0xFF;
        return a != 0 && a != 10 && a != 127 && a < 224
                && !(a == 100 && b >= 64 && b <= 127)
                && !(a == 169 && b == 254)
                && !(a == 172 && b >= 16 && b <= 31)
                && !(a == 192 && b == 0)
                && !(a == 192 && b == 168)
                && !(a == 198 && (b == 18 || b == 19 || b == 51 && c == 100))
                && !(a == 203 && b == 0 && c == 113);
    }

    @Async
    public CompletableFuture<Void> sendQuotaAlertWebhook(
            String webhookUrl, UUID taxpayerId, String serviceCode, ServiceQuotaInfo quota) {
        Map<String, Object> data = new HashMap<>();
        data.put("used", quota.used());
        data.put("limit", quota.limitValue());
        data.put("remaining", quota.remaining());
        data.put("usagePercentage", quota.usagePercentage());
        data.put("status", quota.status());
        data.put("limitUnit", quota.limitUnit());
        return sendServiceAccessWebhook(webhookUrl, taxpayerId, serviceCode, "QUOTA_ALERT", data);
    }

    @Async
    public CompletableFuture<Void> sendLimitExceededWebhook(
            String webhookUrl, UUID taxpayerId, String serviceCode, ServiceQuotaInfo quota) {
        Map<String, Object> data = new HashMap<>();
        data.put("used", quota.used());
        data.put("limit", quota.limitValue());
        data.put("remaining", quota.remaining());
        data.put("usagePercentage", quota.usagePercentage());
        data.put("status", quota.status());
        data.put("limitUnit", quota.limitUnit());
        return sendServiceAccessWebhook(webhookUrl, taxpayerId, serviceCode, "LIMIT_EXCEEDED", data);
    }

    @Async
    public CompletableFuture<Void> sendAccessGrantedWebhook(
            String webhookUrl, UUID taxpayerId, String serviceCode) {
        return sendServiceAccessWebhook(webhookUrl, taxpayerId, serviceCode, "ACCESS_GRANTED", Map.of());
    }

    @Async
    public CompletableFuture<Void> sendAccessDeniedWebhook(
            String webhookUrl, UUID taxpayerId, String serviceCode, String reason) {
        return sendServiceAccessWebhook(webhookUrl, taxpayerId, serviceCode, "ACCESS_DENIED",
                Map.of("reason", reason != null ? reason : "Unknown"));
    }

    @Async
    public void sendServiceAccessAlert(
            UUID taxpayerId, String serviceCode, String eventType, String status, Map<String, Object> metadata) {
        String webhookUrl = System.getProperty("service.access.webhook.url");
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.debug("Webhook URL not configured, skipping event {}", eventType);
            return;
        }
        Map<String, Object> data = new HashMap<>();
        if (metadata != null) {
            data.putAll(metadata);
        }
        data.put("status", status);
        data.put("eventType", eventType);
        sendServiceAccessWebhook(webhookUrl, taxpayerId, serviceCode, eventType, data);
    }
}
