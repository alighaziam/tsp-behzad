package ir.tax.trustedtaxpayers.interfaces.rest.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductionEnvironmentValidatorTest {

    @Test
    void acceptsCompleteHttpsConfigurationAndReadableSigningKey() throws Exception {
        Path key = Files.createTempFile("tax-sdk-test-", ".pem");
        try {
            Files.writeString(key, "test-only-key-material");
            MockEnvironment environment = completeEnvironment(key);
            assertDoesNotThrow(() -> new ProductionEnvironmentValidator(environment).validate());
        } finally {
            Files.deleteIfExists(key);
        }
    }

    @Test
    void rejectsMissingProductionConfiguration() throws Exception {
        Path key = Files.createTempFile("tax-sdk-test-", ".pem");
        try {
            MockEnvironment environment = completeEnvironment(key);
            environment.setProperty("tax.sdk.client-id", " ");
            assertThrows(IllegalStateException.class,
                    () -> new ProductionEnvironmentValidator(environment).validate());
        } finally {
            Files.deleteIfExists(key);
        }
    }

    @Test
    void rejectsInsecureExternalEndpoints() throws Exception {
        Path key = Files.createTempFile("tax-sdk-test-", ".pem");
        try {
            MockEnvironment environment = completeEnvironment(key);
            environment.setProperty("taxpayer-system.base-url", "http://tax.example.test");
            assertThrows(IllegalStateException.class,
                    () -> new ProductionEnvironmentValidator(environment).validate());
        } finally {
            Files.deleteIfExists(key);
        }
    }

    @Test
    void rejectsEnabledWebhookWithoutExplicitHostAllowList() throws Exception {
        Path key = Files.createTempFile("tax-sdk-test-", ".pem");
        try {
            MockEnvironment environment = completeEnvironment(key);
            environment.setProperty("service.access.webhook.enabled", "true");
            environment.setProperty("service.access.webhook.allowed-hosts", "");
            assertThrows(IllegalStateException.class,
                    () -> new ProductionEnvironmentValidator(environment).validate());
        } finally {
            Files.deleteIfExists(key);
        }
    }

    private MockEnvironment completeEnvironment(Path key) {
        return new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:oracle:thin:@db.example.test:1521/service")
                .withProperty("spring.datasource.username", "app")
                .withProperty("spring.datasource.password", "test-only")
                .withProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", "https://id.example.test/issuer")
                .withProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", "https://id.example.test/jwks")
                .withProperty("taxpayer-system.base-url", "https://tax.example.test")
                .withProperty("taxpayer-system.client-id", "test-client")
                .withProperty("taxpayer-system.org-key-id", "test-key")
                .withProperty("taxpayer-system.org-public-key", "test-public-key")
                .withProperty("tax.sdk.base-url", "https://sdk.example.test")
                .withProperty("tax.sdk.memory-id", "test-memory")
                .withProperty("tax.sdk.client-id", "test-sdk-client")
                .withProperty("tax.sdk.private-key-path", key.toString());
    }
}
