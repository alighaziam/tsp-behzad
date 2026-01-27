package ir.tax.trustedtaxpayers.application.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;

/**
 * Custom JWT Decoder برای اعتبارسنجی توکن‌ها
 * مشابه HamfekranJwtDecoder در samt-git
 * 
 * Custom JWT Decoder for token validation
 * Similar to HamfekranJwtDecoder in samt-git
 */
@Slf4j
public class CustomJwtDecoder implements JwtDecoder {

    private final NimbusJwtDecoder delegate;
    private final String issuerUri;
    private final RestTemplate restTemplate;

    public CustomJwtDecoder(String jwkSetUri, String issuerUri) {
        this.issuerUri = issuerUri;
        this.restTemplate = new RestTemplate();
        this.delegate = NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        
        // اضافه کردن validator های سفارشی
        OAuth2TokenValidator<Jwt> customValidator = new OAuth2TokenValidator<Jwt>() {
            @Override
            public OAuth2TokenValidatorResult validate(Jwt token) {
                // اعتبارسنجی زمان انقضا
                Instant expiresAt = token.getExpiresAt();
                if (expiresAt != null && Instant.now().isAfter(expiresAt)) {
                    log.warn("Token has expired at: {}", expiresAt);
                    return OAuth2TokenValidatorResult.failure(
                        new org.springframework.security.oauth2.core.OAuth2Error("invalid_token", "Token has expired", null)
                    );
                }
                
                // اعتبارسنجی issuer
                if (issuerUri != null && !issuerUri.isEmpty()) {
                    String tokenIssuer = token.getIssuer() != null ? token.getIssuer().toString() : null;
                    if (tokenIssuer != null && !tokenIssuer.equals(issuerUri)) {
                        log.warn("Invalid issuer. Expected: {}, Got: {}", issuerUri, tokenIssuer);
                    }
                }
                
                return OAuth2TokenValidatorResult.success();
            }
        };
        
        OAuth2TokenValidator<Jwt> validators = new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefault(),
            customValidator
        );
        
        this.delegate.setJwtValidator(validators);
    }

    @Override
    public Jwt decode(String token) throws JwtException {
        try {
            log.debug("Decoding JWT token");
            Jwt jwt = delegate.decode(token);
            
            // اضافه کردن اطلاعات اضافی در صورت نیاز
            log.debug("Token decoded successfully. Subject: {}, Issuer: {}", 
                    jwt.getSubject(), 
                    jwt.getIssuer());
            
            return jwt;
        } catch (JwtException e) {
            log.error("Failed to decode JWT token: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * دریافت اطلاعات عمومی از issuer
     * Get public information from issuer
     */
    public Map<String, Object> getIssuerInfo() {
        try {
            if (issuerUri != null && !issuerUri.isEmpty()) {
                String wellKnownUrl = issuerUri + "/.well-known/openid-configuration";
                @SuppressWarnings("unchecked")
                Map<String, Object> config = restTemplate.getForObject(wellKnownUrl, Map.class);
                log.debug("Retrieved issuer configuration from: {}", wellKnownUrl);
                return config;
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve issuer configuration: {}", e.getMessage());
        }
        return null;
    }
}


