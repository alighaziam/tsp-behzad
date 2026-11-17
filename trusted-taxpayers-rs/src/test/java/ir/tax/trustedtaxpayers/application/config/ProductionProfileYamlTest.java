package ir.tax.trustedtaxpayers.application.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.env.PropertySource;

class ProductionProfileYamlTest {

    @Test
    void exposesProductionSecurityAndWebhookPropertiesAtExpectedPaths() throws Exception {
        var sources = new YamlPropertySourceLoader().load(
                "application-prod", new ClassPathResource("application-prod.yml"));

        assertThat(sources).hasSize(1);
        PropertySource<?> source = sources.get(0);

        assertThat(source.getProperty("spring.security.oauth2.resourceserver.jwt.issuer-uri"))
                .isEqualTo("${OAUTH2_ISSUER_URI}");
        assertThat(source.getProperty("spring.security.oauth2.resourceserver.jwt.jwk-set-uri"))
                .isEqualTo("${OAUTH2_JWK_SET_URI}");
        assertThat(source.getProperty("service.access.webhook.enabled"))
                .isEqualTo("${SERVICE_ACCESS_WEBHOOK_ENABLED:false}");
        assertThat(source.getProperty("service.access.webhook.allowed-hosts"))
                .isEqualTo("${SERVICE_ACCESS_WEBHOOK_ALLOWED_HOSTS:}");
        assertThat(source.getProperty("server.port")).isEqualTo("${PORT:8080}");

        assertThat(source.getProperty("spring.security.oauth2.resourceservice")).isNull();
        assertThat(source.getProperty("server.jwt.issuer-uri")).isNull();
        assertThat(source.getProperty("access.webhook.enabled")).isNull();
    }
}
