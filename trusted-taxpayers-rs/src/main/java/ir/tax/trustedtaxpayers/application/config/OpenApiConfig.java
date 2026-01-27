package ir.tax.trustedtaxpayers.application.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import ir.tax.trustedtaxpayers.application.metrics.RequestMetricsInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Trusted Taxpayers API")
                        .version("v1")
                        .description("API سیستم مودیان معتمد مالیاتی - این API امکان مدیریت صورتحساب‌های الکترونیکی، مودیان، محصولات و سایر اطلاعات مالیاتی را فراهم می‌کند.")
                        .contact(new Contact()
                                .name("Trusted Taxpayers Team")
                                .email("support@trustedtaxpayers.ir"))
                        .license(new License()
                                .name("Proprietary")
                                .url("https://trustedtaxpayers.ir")))
                .externalDocs(new ExternalDocumentation()
                        .description("مستندات کامل API")
                        .url("https://trustedtaxpayers.ir/docs"));
    }
    
    private final RequestMetricsInterceptor requestMetricsInterceptor;

    public OpenApiConfig(RequestMetricsInterceptor requestMetricsInterceptor) {
        this.requestMetricsInterceptor = requestMetricsInterceptor;
    }

    @Bean
    public WebMvcConfigurer webMvcConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOriginPatterns("*")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);
            }

            @Override
            public void addInterceptors(InterceptorRegistry registry) {
                registry.addInterceptor(requestMetricsInterceptor)
                        .addPathPatterns("/api/**")
                        .excludePathPatterns("/actuator/**", "/swagger-ui/**", "/v3/api-docs/**");
            }
        };
    }
}



