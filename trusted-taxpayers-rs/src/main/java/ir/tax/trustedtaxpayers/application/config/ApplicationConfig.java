package ir.tax.trustedtaxpayers.application.config;

/**
 * Author: Behzad Farajpour
 * Project: TSP
 * Email: farajpour-b@hamfekrangroup.ir
 */

import ir.tax.trustedtaxpayers.application.usecase.AssignRoleUseCase;
import ir.tax.trustedtaxpayers.application.usecase.CreateUserUseCase;
import ir.tax.trustedtaxpayers.application.usecase.SearchTaxpayersUseCase;
import ir.tax.trustedtaxpayers.application.usecase.SearchUsersUseCase;
import ir.tax.trustedtaxpayers.core.util.UuidStringConverter;
import ir.tax.trustedtaxpayers.externalService.config.TaxpayerSystemConfigProperties;
import ir.tax.trustedtaxpayers.interfaces.rest.config.UuidArgumentResolver;
import ir.tax.trustedtaxpayers.model.ports.TaxpayerQueryPort;
import ir.tax.trustedtaxpayers.model.ports.UserQueryPort;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.format.FormatterRegistry;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * پیکربندی اصلی اپلیکیشن
 * مشابه AppConfig در samt-git
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider", dateTimeProviderRef = "dateTimeProvider")
@EnableAsync
@Import({
    HttpClientConfig.class,
    OAuth2ResourceServerConfig.class,
    MethodSecurityConfig.class
})
public class ApplicationConfig implements AsyncConfigurer, WebMvcConfigurer {
    
    private final UuidArgumentResolver uuidArgumentResolver;
    
    public ApplicationConfig(UuidArgumentResolver uuidArgumentResolver) {
        this.uuidArgumentResolver = uuidArgumentResolver;
    }
    
    @Bean
    public SearchTaxpayersUseCase searchTaxpayersUseCase(TaxpayerQueryPort taxpayerQueryPort) {
        return new SearchTaxpayersUseCase(taxpayerQueryPort);
    }

    @Bean
    public SearchUsersUseCase searchUsersUseCase(UserQueryPort userQueryPort) {
        return new SearchUsersUseCase(userQueryPort);
    }

    @Bean
    public CreateUserUseCase createUserUseCase(ir.tax.trustedtaxpayers.application.service.UserRoleService userRoleService) {
        return new CreateUserUseCase(userRoleService);
    }

    @Bean
    public AssignRoleUseCase assignRoleUseCase(ir.tax.trustedtaxpayers.application.service.UserRoleService userRoleService) {
        return new AssignRoleUseCase(userRoleService);
    }
    
    @Bean
    public AuditorAware<String> auditorProvider() {
        return new AuditorAwareImpl();
    }

    @Bean
    public org.springframework.data.auditing.DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(OffsetDateTime.now());
    }
    
    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder, TaxpayerSystemConfigProperties config) {
        return builder
            .setConnectTimeout(Duration.ofMillis(config.getTimeout().getConnect()))
            .setReadTimeout(Duration.ofMillis(config.getTimeout().getRead()))
            .build();
    }
    
    /**
     * Thread pool برای async operations (ارسال صورتحساب‌ها)
     */
    @Bean(name = "asyncExecutor")
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("invoice-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
    
    @Override
    public Executor getAsyncExecutor() {
        return asyncExecutor();
    }
    
    /**
     * Converter برای تبدیل String (encoded UUID) به UUID
     * این converter به Spring اجازه می‌دهد که encoded UUID strings را به UUID تبدیل کند
     */
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new Converter<String, UUID>() {
            @Override
            public UUID convert(String source) {
                if (source == null || source.isBlank()) {
                    return null;
                }
                
                // ابتدا سعی می‌کنیم به صورت encoded UUID decode کنیم
                try {
                    return UuidStringConverter.decode(source);
                } catch (IllegalArgumentException e) {
                    // اگر decode نشد، سعی می‌کنیم به صورت استاندارد UUID parse کنیم
                    try {
                        return UUID.fromString(source);
                    } catch (IllegalArgumentException ex) {
                        throw new IllegalArgumentException(
                            "Invalid UUID format: " + source + ". Expected encoded UUID or standard UUID format.", ex);
                    }
                }
            }
        });
    }

    @Bean
    @Primary
    public com.fasterxml.jackson.databind.ObjectMapper objectMapper() {
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        mapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // مطمئن می‌شویم که default typing فعال نیست - ObjectMapper به صورت پیش‌فرض default typing ندارد
        // فقط redisObjectMapper دارای default typing است که برای HTTP استفاده نمی‌شود
        return mapper;
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.modules(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
            builder.featuresToDisable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            builder.findModulesViaServiceLoader(true);
        };
    }


    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(uuidArgumentResolver);
    }
}



