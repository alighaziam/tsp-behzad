package ir.tax.trustedtaxpayers.application.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * پیکربندی HTTP Client
 * مشابه HttpClientConfig در samt-git
 * 
 * HTTP Client Configuration
 * Similar to HttpClientConfig in samt-git
 */
@Configuration
public class HttpClientConfig {

    /**
     * RestTemplate برای فراخوانی‌های امن (با SSL)
     * RestTemplate for secure calls (with SSL)
     */
    @Bean("secureRestTemplate")
    public RestTemplate secureRestTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(15))
                .setReadTimeout(Duration.ofSeconds(45))
                .build();
    }
}


