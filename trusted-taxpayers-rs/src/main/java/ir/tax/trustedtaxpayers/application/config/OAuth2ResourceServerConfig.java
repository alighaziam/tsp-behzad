package ir.tax.trustedtaxpayers.application.config;

import ir.tax.trustedtaxpayers.application.security.JwtAuthFilter;
import ir.tax.trustedtaxpayers.application.security.JwtService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * پیکربندی OAuth2 Resource Server
 * OAuth2 Resource Server Configuration
 * <p>
 * مشابه samt-git/contract-rs/config/OAuth2ResourceServerConfig
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class OAuth2ResourceServerConfig {

    private final Environment environment;

    public OAuth2ResourceServerConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           @Qualifier("corsConfigurationSource") CorsConfigurationSource corsConfigurationSource,
                                           JwtAuthFilter jwtAuthFilter) throws Exception {
        log.info("Configuring security filter chain with JWT authentication");
        http.anonymous(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exception -> exception.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/api/auth/**",
                                "/api/v1/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/api/base-info/**"
                        ).permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                // Security Headers توسط SecurityHeadersFilter اضافه می‌شوند
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .contentTypeOptions(contentTypeOptions -> {
                        })
                        .httpStrictTransportSecurity(hsts -> hsts
                                .maxAgeInSeconds(31536000)));

        return http.build();
    }

    /**
     * CORS Configuration - مشابه samt
     * در Production، origins از Environment Variable خوانده می‌شود
     * در Development، از "*" استفاده می‌شود
     */
    @Bean
    @Primary
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // تعیین origins بر اساس profile
        String[] activeProfiles = environment.getActiveProfiles();
        boolean isProduction = Arrays.asList(activeProfiles).contains("prod");

        if (isProduction) {
            // در Production، origins از Environment Variable خوانده می‌شود
            String allowedOrigins = environment.getProperty("cors.allowed-origins", "");
            if (allowedOrigins != null && !allowedOrigins.isBlank()) {
                List<String> origins = Arrays.asList(allowedOrigins.split(","));
                configuration.setAllowedOrigins(origins.stream()
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList());
                log.info("CORS configured for Production with {} allowed origins", origins.size());
            } else {
                // Fail closed: never start production with a placeholder or wildcard CORS origin.
                throw new IllegalStateException(
                        "cors.allowed-origins must be configured with explicit trusted origins in Production"
                );
            }
        } else {
            // در Development، از "*" استفاده می‌شود
            configuration.setAllowedOriginPatterns(List.of("*"));
            log.info("CORS configured for Development (allowing all origins)");
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type", "X-User"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Password Encoder برای رمزگذاری رمزعبورها
     * Password Encoder for password encryption
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthFilter jwtAuthFilter(JwtService jwtService) {
        return new JwtAuthFilter(jwtService);
    }
}


