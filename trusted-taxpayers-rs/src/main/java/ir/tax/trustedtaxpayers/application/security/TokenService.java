package ir.tax.trustedtaxpayers.application.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Simple in-memory token cache that fetches token via TPIS SDK (if present) using reflection.
 * Fully self-contained and safe to use without SDK (returns null when not available).
 */
@Service
public class TokenService {

    private static final Logger logger = LoggerFactory.getLogger(TokenService.class);

    @Autowired(required = false)
    @Qualifier("tpisTaxApi")
    private Object tpisTaxApi;

    private final AtomicReference<String> cachedToken = new AtomicReference<>();
    private volatile long tokenExpiresAtEpochSeconds = 0L;

    /**
     * Returns a Bearer token string or null when unavailable. Caches until expiry.
     */
    public String getBearerToken() {
        try {
            if (!isTokenExpired()) {
                return cachedToken.get();
            }

            synchronized (this) {
                if (!isTokenExpired()) {
                    return cachedToken.get();
                }

                String token = requestTokenReflectively();
                cachedToken.set(token);
                return token;
            }
        } catch (Exception e) {
            logger.warn("Token retrieval failed: {}", e.getMessage());
            return null;
        }
    }

    private boolean isTokenExpired() {
        long now = Instant.now().getEpochSecond();
        return cachedToken.get() == null || now >= tokenExpiresAtEpochSeconds;
    }

    private String requestTokenReflectively() {
        if (tpisTaxApi == null) {
            return null;
        }
        try {
            Object tokenModel = tpisTaxApi.getClass().getMethod("requestToken").invoke(tpisTaxApi);
            if (tokenModel == null) {
                return null;
            }
            String token = (String) tokenModel.getClass().getMethod("getToken").invoke(tokenModel);

            long now = Instant.now().getEpochSecond();
            long defaultTtl = 300L; // 5 minutes fallback
            long ttlSeconds = defaultTtl;
            try {
                Object exp = tokenModel.getClass().getMethod("getExpiresIn").invoke(tokenModel);
                if (exp instanceof Number) {
                    ttlSeconds = ((Number) exp).longValue();
                }
            } catch (NoSuchMethodException ignored) {
            }

            // apply a small safety margin (10 seconds)
            tokenExpiresAtEpochSeconds = now + Math.max(0L, ttlSeconds - 10L);

            return (token != null && !token.isBlank()) ? token : null;
        } catch (Exception e) {
            logger.warn("Failed to obtain token via TPIS SDK: {}", e.getMessage());
            return null;
        }
    }
}




