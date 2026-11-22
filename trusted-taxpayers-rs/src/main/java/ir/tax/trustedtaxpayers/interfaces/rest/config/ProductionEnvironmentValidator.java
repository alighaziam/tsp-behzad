package ir.tax.trustedtaxpayers.interfaces.rest.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Fails production startup when deployment-owned integrations are incomplete or unsafe.
 * This validates configuration only; it never connects to external systems.
 */
@Component
@Profile("prod")
public final class ProductionEnvironmentValidator implements InitializingBean {

    private static final List<String> REQUIRED = List.of(
            "spring.datasource.url",
            "spring.datasource.username",
            "spring.datasource.password",
            "spring.security.oauth2.resourceserver.jwt.issuer-uri",
            "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
            "taxpayer-system.base-url",
            "taxpayer-system.client-id",
            "taxpayer-system.org-key-id",
            "taxpayer-system.org-public-key",
            "tax.sdk.base-url",
            "tax.sdk.memory-id",
            "tax.sdk.client-id",
            "tax.sdk.private-key-path",
            "spring.mail.host",
            "spring.mail.username",
            "spring.mail.password",
            "app.public-base-url"
    );

    private static final List<String> HTTPS_ENDPOINTS = List.of(
            "spring.security.oauth2.resourceserver.jwt.issuer-uri",
            "spring.security.oauth2.resourceserver.jwt.jwk-set-uri",
            "taxpayer-system.base-url",
            "tax.sdk.base-url",
            "app.public-base-url"
    );

    private final Environment environment;

    public ProductionEnvironmentValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() {
        validate();
    }

    public void validate() {
        List<String> missing = new ArrayList<>();
        for (String key : REQUIRED) {
            if (!hasText(environment.getProperty(key))) {
                missing.add(key);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Missing required production configuration properties: "
                    + String.join(", ", missing));
        }

        for (String key : HTTPS_ENDPOINTS) {
            String value = environment.getProperty(key);
            try {
                URI uri = URI.create(value);
                if (!"https".equalsIgnoreCase(uri.getScheme())
                        || !hasText(uri.getHost()) || uri.getUserInfo() != null) {
                    throw new IllegalArgumentException();
                }
            } catch (RuntimeException exception) {
                throw new IllegalStateException(
                        "Production endpoint must be an absolute HTTPS URL without embedded credentials: " + key);
            }
        }

        Path privateKey = Path.of(environment.getRequiredProperty("tax.sdk.private-key-path"))
                .toAbsolutePath().normalize();
        if (!Files.isRegularFile(privateKey) || !Files.isReadable(privateKey)) {
            throw new IllegalStateException(
                    "Tax SDK private key must be a readable regular file mounted into the production runtime.");
        }

        if (Boolean.parseBoolean(environment.getProperty("service.access.webhook.enabled", "false"))
                && !hasText(environment.getProperty("service.access.webhook.allowed-hosts"))) {
            throw new IllegalStateException(
                    "Outbound webhooks cannot be enabled without an explicit destination host allow-list.");
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
