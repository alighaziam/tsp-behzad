package ir.tax.trustedtaxpayers.interfaces.rest.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Resolves the current principal exclusively from the server-side SecurityContext.
 * Client-supplied identity headers such as X-User are untrusted and must never
 * be used to establish an authenticated identity.
 */
public final class CurrentUserResolver {

    private CurrentUserResolver() {
    }

    public static String resolveUsername(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return null;
        }
        return authentication.getName();
    }
}
