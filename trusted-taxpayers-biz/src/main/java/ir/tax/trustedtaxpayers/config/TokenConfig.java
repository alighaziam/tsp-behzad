package ir.tax.trustedtaxpayers.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * پیکربندی توکن‌های Backend
 * مشابه TokenConfig در samt-git
 * 
 * Backend Token Configuration
 * Similar to TokenConfig in samt-git
 */
@Slf4j
@Configuration
public class TokenConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    private String issuerUri;

    @Value("${security.oauth2.client.client-id}")
    private String clientId;

    @Value("${security.oauth2.client.client-secret}")
    private String clientSecret;

    @Value("${security.oauth2.client.username:}")
    private String username;

    @Value("${security.oauth2.client.password:}")
    private String password;

    @Value("${security.oauth2.client.scope:openid}")
    private String scope;

    /**
     * Client Token برای فراخوانی سرویس‌های دیگر
     * Client Token for calling other services
     */
    @Bean(name = "clientToken")
    public BackendToken clientToken(RestTemplate restTemplate) {
        try {
            log.info("Initializing client token with issuer: {}", issuerUri);
            return new BackendToken(issuerUri, clientId, clientSecret, username, password, scope, restTemplate);
        } catch (Exception e) {
            log.error("Failed to initialize client token", e);
            return new BackendToken(); // Empty token
        }
    }

    /**
     * Platform Token برای فراخوانی Platform API
     * Platform Token for calling Platform API
     */
    @Bean(name = "platformToken")
    public BackendToken platformToken(
            @Value("${platform.security.oauth2.client.url:}") String platformUrl,
            @Value("${platform.security.oauth2.client.client-id:}") String platformClientId,
            @Value("${platform.security.oauth2.client.client-secret:}") String platformClientSecret,
            @Value("${platform.security.oauth2.client.username:}") String platformUsername,
            @Value("${platform.security.oauth2.client.password:}") String platformPassword,
            @Value("${platform.security.oauth2.client.scope:openid}") String platformScope,
            RestTemplate restTemplate) {
        
        if (platformUrl == null || platformUrl.isEmpty()) {
            log.warn("Platform OAuth2 URL not configured, returning empty token");
            return new BackendToken();
        }
        
        try {
            log.info("Initializing platform token with URL: {}", platformUrl);
            return new BackendToken(platformUrl, platformClientId, platformClientSecret, 
                    platformUsername, platformPassword, platformScope, restTemplate);
        } catch (Exception e) {
            log.error("Failed to initialize platform token", e);
            return new BackendToken();
        }
    }

    /**
     * کلاس Backend Token برای مدیریت توکن‌های OAuth2
     * Backend Token class for managing OAuth2 tokens
     * Token is loaded lazily on first access to avoid startup failures
     */
    @Data
    public static class BackendToken {
        private String tokenValue;
        private String tokenType;
        private Long expiresIn;
        private Instant expiresAt;
        private String refreshToken;
        private String scope;
        
        // Fields for lazy loading
        private final String issuerUri;
        private final String clientId;
        private final String clientSecret;
        private final String username;
        private final String password;
        private final String scopeParam;
        private final RestTemplate restTemplate;
        private volatile boolean initialized = false;
        private final Object lock = new Object();

        /**
         * Constructor for empty token (when OAuth2 is not configured)
         */
        public BackendToken() {
            this.issuerUri = null;
            this.clientId = null;
            this.clientSecret = null;
            this.username = null;
            this.password = null;
            this.scopeParam = null;
            this.restTemplate = null;
            this.tokenValue = "";
            this.initialized = true; // Mark as initialized to avoid lazy loading
        }

        /**
         * Constructor with OAuth2 configuration (token will be loaded lazily)
         */
        public BackendToken(String issuerUri, String clientId, String clientSecret, 
                          String username, String password, String scope, RestTemplate restTemplate) {
            this.issuerUri = issuerUri;
            this.clientId = clientId;
            this.clientSecret = clientSecret;
            this.username = username;
            this.password = password;
            this.scopeParam = scope;
            this.restTemplate = restTemplate;
            this.tokenValue = "";
            
            // Try to initialize token, but don't fail if OAuth2 server is not available
            try {
                initializeToken();
            } catch (Exception e) {
                log.warn("Could not initialize token during startup (OAuth2 server may not be available). " +
                        "Token will be loaded on first access. Error: {}", e.getMessage());
            }
        }

        /**
         * Initialize token by making HTTP request to OAuth2 server
         */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private void initializeToken() {
            if (issuerUri == null || restTemplate == null) {
                return; // Empty token configuration
            }
            
            try {
                // ساخت URL توکن
                String tokenUrl = issuerUri + "/protocol/openid-connect/token";
                
                // ساخت درخواست
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                headers.set("Authorization", "Basic " + 
                        Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes()));

                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                
                // استفاده از password grant یا client_credentials
                if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
                    body.add("grant_type", "password");
                    body.add("username", username);
                    body.add("password", password);
                } else {
                    body.add("grant_type", "client_credentials");
                }
                
                body.add("scope", scopeParam);

                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

                // ارسال درخواست
                ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);
                
                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    Map<String, Object> responseBody = response.getBody();
                    this.tokenValue = (String) responseBody.get("access_token");
                    this.tokenType = (String) responseBody.get("token_type");
                    this.refreshToken = (String) responseBody.get("refresh_token");
                    this.scope = (String) responseBody.get("scope");
                    
                    Object expiresInObj = responseBody.get("expires_in");
                    if (expiresInObj != null) {
                        this.expiresIn = expiresInObj instanceof Integer ? 
                                ((Integer) expiresInObj).longValue() : (Long) expiresInObj;
                        this.expiresAt = Instant.now().plusSeconds(this.expiresIn);
                    }
                    
                    log.info("Backend token obtained successfully. Expires in: {} seconds", expiresIn);
                } else {
                    log.warn("Failed to obtain backend token. Status: {}", response.getStatusCode());
                    this.tokenValue = "";
                }
            } catch (Exception e) {
                log.warn("Error obtaining backend token (will retry on access): {}", e.getMessage());
                this.tokenValue = "";
                throw e; // Re-throw to allow retry on first access
            }
        }

        /**
         * Ensure token is initialized (lazy loading)
         */
        private void ensureInitialized() {
            if (initialized) {
                return;
            }
            
            synchronized (lock) {
                if (initialized) {
                    return;
                }
                
                try {
                    initializeToken();
                    initialized = true;
                } catch (Exception e) {
                    log.warn("Failed to initialize token on first access: {}", e.getMessage());
                    initialized = true; // Mark as initialized to avoid infinite retries
                }
            }
        }

        /**
         * Get token value (with lazy loading)
         */
        public String getTokenValue() {
            if (!initialized) {
                ensureInitialized();
            }
            return tokenValue;
        }

        public boolean isExpired() {
            return expiresAt != null && Instant.now().isAfter(expiresAt);
        }

        public boolean isValid() {
            if (!initialized) {
                ensureInitialized();
            }
            return tokenValue != null && !tokenValue.isEmpty() && !isExpired();
        }
        
        /**
         * Refresh token if expired
         */
        public void refreshIfNeeded() {
            if (isExpired() || !isValid()) {
                synchronized (lock) {
                    initialized = false; // Allow re-initialization
                    ensureInitialized();
                }
            }
        }
    }
}


