package ir.tax.trustedtaxpayers.biz.impl;

import ir.tax.trustedtaxpayers.biz.GenericBiz;
import ir.tax.trustedtaxpayers.config.TokenConfig;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestTemplate;

import java.util.Locale;

/**
 * کلاس پایه برای تمام Manager Implementation ها
 * مشابه GenericImpl در samt-git
 * 
 * Base class for all Manager Implementations
 * Similar to GenericImpl in samt-git
 */
public abstract class GenericImpl implements GenericBiz {

    @Autowired
    protected RestTemplate restTemplate;

    @Autowired(required = false)
    @Qualifier("clientToken")
    protected TokenConfig.BackendToken clientToken;

    @Autowired(required = false)
    @Qualifier("platformToken")
    protected TokenConfig.BackendToken platformToken;

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
    protected String issuerUri;

    @Value("${security.oauth2.client.client-id}")
    protected String clientId;

    @Value("${security.oauth2.client.client-secret}")
    protected String clientSecret;

    @Autowired(required = false)
    private MessageSource messageSource;

    /**
     * دریافت Client Token برای Locale مشخص
     * Get Client Token for specific Locale
     */
    protected TokenConfig.BackendToken clientToken(Locale locale) {
        if (clientToken != null && clientToken.isValid()) {
            return clientToken;
        }
        // اگر token منقضی شده، می‌توان refresh کرد
        return clientToken != null ? clientToken : new TokenConfig.BackendToken();
    }

    /**
     * دریافت Platform Token
     * Get Platform Token
     */
    protected TokenConfig.BackendToken platformToken(Locale locale) {
        if (platformToken != null && platformToken.isValid()) {
            return platformToken;
        }
        return platformToken != null ? platformToken : new TokenConfig.BackendToken();
    }

    /**
     * دریافت پیام از message source
     * Get message from message source
     */
    protected String getMessage(String key, HttpServletRequest request, Object... args) {
        if (key == null) {
            return null;
        }
        if (messageSource == null) {
            return args == null || args.length == 0 ? key : String.format(java.util.Locale.ROOT, key, args);
        }
        Locale locale = request != null && request.getLocale() != null ? request.getLocale() : Locale.getDefault();
        try {
            return messageSource.getMessage(key, args, key, locale);
        } catch (Exception ignored) {
            return args == null || args.length == 0 ? key : String.format(java.util.Locale.ROOT, key, args);
        }
    }

    /**
     * دریافت پیام از message source با Locale
     * Get message from message source with Locale
     */
    protected String getMessage(String key, Locale locale, Object... args) {
        if (key == null) {
            return null;
        }
        if (messageSource == null) {
            return args == null || args.length == 0 ? key : String.format(java.util.Locale.ROOT, key, args);
        }
        try {
            return messageSource.getMessage(key, args, key, locale != null ? locale : Locale.getDefault());
        } catch (Exception ignored) {
            return args == null || args.length == 0 ? key : String.format(java.util.Locale.ROOT, key, args);
        }
    }

    /**
     * استخراج User ID از JWT token
     * Extract User ID from JWT token
     */
    protected String getUserIdFromToken(JwtAuthenticationToken token) {
        if (token == null || token.getToken() == null) {
            return null;
        }
        
        // ابتدا از claim "sub" استفاده کن
        Object userId = token.getToken().getClaims().get("sub");
        if (userId != null) {
            return userId.toString();
        }
        
        // اگر نبود از "user_id" استفاده کن
        userId = token.getToken().getClaims().get("user_id");
        if (userId != null) {
            return userId.toString();
        }
        
        return token.getName();
    }

    /**
     * استخراج Email از JWT token
     * Extract Email from JWT token
     */
    protected String getEmailFromToken(JwtAuthenticationToken token) {
        Object email = getClaimFromToken(token, "email");
        return email != null ? email.toString() : null;
    }

    /**
     * استخراج Roles از JWT token
     * Extract Roles from JWT token
     */
    protected java.util.List<String> getRolesFromToken(JwtAuthenticationToken token) {
        if (token == null || token.getAuthorities() == null) {
            return java.util.Collections.emptyList();
        }
        
        return token.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .collect(java.util.stream.Collectors.toList());
    }
}


