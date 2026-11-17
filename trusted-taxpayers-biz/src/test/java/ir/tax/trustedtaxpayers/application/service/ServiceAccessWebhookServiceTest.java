package ir.tax.trustedtaxpayers.application.service;

import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.ServiceRepository;
import ir.tax.trustedtaxpayers.infrastructure.jpa.repository.TaxpayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class ServiceAccessWebhookServiceTest {

    private ServiceRepository serviceRepository;
    private TaxpayerRepository taxpayerRepository;
    private ServiceAccessWebhookService service;

    @BeforeEach
    void setUp() {
        serviceRepository = mock(ServiceRepository.class);
        taxpayerRepository = mock(TaxpayerRepository.class);
        service = new ServiceAccessWebhookService(serviceRepository, taxpayerRepository);
    }

    @Test
    void outboundWebhooksAreDisabledByDefault() {
        service.sendServiceAccessWebhook("https://example.com/hook", UUID.randomUUID(),
                "INVOICE", "ACCESS_GRANTED", Map.of());

        verifyNoInteractions(serviceRepository, taxpayerRepository);
    }

    @Test
    void rejectsHttpEvenWhenHostIsAllowListed() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "allowedHosts", "example.com");

        assertFalse(service.isAllowedWebhookDestination("http://example.com/hook"));
    }

    @Test
    void rejectsHostsNotInAllowListBeforeDnsLookup() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "allowedHosts", "trusted.example");

        assertFalse(service.isAllowedWebhookDestination("https://attacker.example/hook"));
    }

    @Test
    void rejectsLoopbackEvenWhenAllowListed() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "allowedHosts", "localhost");

        assertFalse(service.isAllowedWebhookDestination("https://localhost/hook"));
    }

    @Test
    void rejectsCredentialsAndNonStandardPorts() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "allowedHosts", "example.com");

        assertFalse(service.isAllowedWebhookDestination("https://user:pass@example.com/hook"));
        assertFalse(service.isAllowedWebhookDestination("https://example.com:8443/hook"));
    }
}
