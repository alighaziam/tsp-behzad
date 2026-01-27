package ir.tax.trustedtaxpayers.biz;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Locale;

/**
 * Interface پایه برای تمام Manager ها
 * مشابه GenericBiz در samt-git
 * 
 * Base Interface for all Managers
 * Similar to GenericBiz in samt-git
 */
public interface GenericBiz {

    /**
     * دریافت Locale از request
     * Get Locale from request
     */
    default Locale getLocale(HttpServletRequest request) {
        if (request != null && request.getLocale() != null) {
            return request.getLocale();
        }
        return Locale.forLanguageTag("fa-IR");
    }

    /**
     * دریافت نام کاربر از JWT token
     * Get username from JWT token
     */
    default String getUsernameFromToken(JwtAuthenticationToken token) {
        if (token != null && token.getName() != null) {
            return token.getName();
        }
        return "anonymous";
    }

    /**
     * دریافت claim از JWT token
     * Get claim from JWT token
     */
    default Object getClaimFromToken(JwtAuthenticationToken token, String claimName) {
        if (token != null && token.getToken() != null && token.getToken().getClaims() != null) {
            return token.getToken().getClaims().get(claimName);
        }
        return null;
    }

    /**
     * بررسی داشتن authority خاص
     * Check if has specific authority
     */
    default boolean hasAuthority(JwtAuthenticationToken token, String authority) {
        if (token != null && token.getAuthorities() != null) {
            return token.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals(authority));
        }
        return false;
    }
}


