package ir.tax.trustedtaxpayers.interfaces.rest.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class CurrentUserResolver {

    private CurrentUserResolver() {
    }

    public static String resolveUsername(HttpServletRequest request) {
        if (request != null) {
            String headerUser = request.getHeader("X-User");
            if (headerUser != null && !headerUser.isBlank()) {
                return headerUser;
            }
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return null;
    }
}

