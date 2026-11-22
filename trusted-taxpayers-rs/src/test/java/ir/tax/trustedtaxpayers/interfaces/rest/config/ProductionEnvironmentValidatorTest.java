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
        Path key = createKey();
        try {
            MockEnvironment environment = completeEnvironment(key);
            assertDoesNotThrow(() -> new ProductionEnvironmentValidator(environment).validate());
        } finally {
            Files.deleteIfExists(key);
        }
    }

    @Test
    void rejectsMissingProductionConfiguration() throws Exception {
        Path key = createKey();
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
    void rejectsPlaceholderOrUnresolvedProductionSecrets() throws Exception {
        Path key = createKey();
        try {
            MockEnvironment placeholder = completeEnvironment(key)
                    .withProperty("spring.datasource.password", "test-only");
            assertThrows(IllegalStateException.class,
                    () -> new ProductionEnvironmentValidator(placeholder).validate());

            MockEnvironment unresolved = completeEnvironment(key)
                    .withProperty("spring.mail.password", "${MAIL_PASSWORD}");
            assertThrows(IllegalStateException.class,
                    () -> new ProductionEnvironmentValidator(unresolved).validate());
        } finally {
            Files.deleteIfExists(key);
        }
    }

    @Test
    void rejectsInsecureExternalEndpoints() throws Exception {
        Path key = createKey();
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
    void rejectsInvalidMailPort() throws Exception {
        Path key = createKey();
        try {
            MockEnvironment environment = completeEnvironment(key);
            environment.setProperty("spring.mail.port", "70000");
            assertThrows(IllegalStateException.class,
                    () -> new ProductionEnvironmentValidator(environment).validate());
        } finally {
            Files.deleteIfExists(key);
        }
    }

    @Test
    void rejectsEmptySigningKeyFile() throws Exception {
        Path key = Files.createTempFile("tax-sdk-empty-", ".pem");
        try {
            assertThrows(IllegalStateException.class,
                    () -> new ProductionEnvironmentValidator(completeEnvironment(key)).validate());
        } finally {
            Files.deleteIfExists(key);
        }
    }

    @Test
    void acceptsExplicitWebhookHostAllowList() throws Exception {
        Path key = createKey();
        try {
            MockEnvironment environment = completeEnvironment(key)
                    .withProperty("service.access.webhook.enabled", "true")
                    .withProperty("service.access.webhook.allowed-hosts", "hooks.example.test,10.20.30.40");
            assertDoesNotThrow(() -> new ProductionEnvironmentValidator(environment).validate());
        } finally {
            Files.deleteIfExists(key);
        }
    }

    @Test
    void rejectsWildcardOrUrlWebhookAllowListEntries() throws Exception {
        Path key = createKey();
        try {
            MockEnvironment environment = completeEnvironment(key)
                    .withProperty("service.access.webhook.enabled", "true")
                    .withProperty("service.access.webhook.allowed-hosts", "*.example.test,https://other.example.test");
            assertThrows(IllegalStateException.class,
                    () -> new ProductionEnvironmentValidator(environment).validate());
        } finally {
            Files.deleteIfExists(key);
        }
    }

    private Path createKey() throws Exception {
        Path key = Files.createTempFile("tax-sdk-test-", ".pem");
        Files.writeString(key, "test-only-key-material");
        return key;
    }

    private MockEnvironment completeEnvironment(Path key) {
        return new MockEnvironment()
                .withProperty("spring.datasource.url", "jdbc:oracle:thin:@db.example.test:1521/service")
                .withProperty("spring.datasource.username", "app")
                .withProperty("spring.datasource.password", "integration-test-secret-value")
                .withProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri", "https://id.example.test/issuer")
                .withProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri", "https://id.example.test/jwks")
                .withProperty("taxpayer-system.base-url", "https://tax.example.test")
                .withProperty("taxpayer-system.client-id", "test-client")
                .withProperty("taxpayer-system.org-key-id", "test-key")
                .withProperty("taxpayer-system.org-public-key", "test-public-key")
                .withProperty("tax.sdk.base-url", "https://sdk.example.test")
                .withProperty("tax.sdk.memory-id", "test-memory")
                .withProperty("tax.sdk.client-id", "test-sdk-client")
                .withProperty("tax.sdk.private-key-path", key.toString())
                .withProperty("spring.mail.host", "smtp.example.test")
                .withProperty("spring.mail.port", "587")
                .withProperty("spring.mail.username", "noreply@example.test")
                .withProperty("spring.mail.password", "integration-test-secret-value")
                .withProperty("app.public-base-url", "https://app.example.test");
    }
}
