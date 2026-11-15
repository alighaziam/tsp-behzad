package ir.tax.trustedtaxpayers.interfaces.rest;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Shared authentication check for mobile compatibility endpoints.
 * Authentication is established by JwtAuthFilter; controllers must not
 * implement their own hard-coded bearer-token checks.
 */
public final class MobileCompatSecurity {

    private MobileCompatSecurity() {
    }

    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
