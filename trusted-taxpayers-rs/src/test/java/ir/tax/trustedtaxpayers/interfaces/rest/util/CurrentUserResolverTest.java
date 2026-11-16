package ir.tax.trustedtaxpayers.interfaces.rest.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CurrentUserResolverTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doesNotTrustClientSuppliedXUserHeader() {
        HttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User", "administrator");

        assertNull(CurrentUserResolver.resolveUsername(request));
    }

    @Test
    void resolvesAuthenticatedPrincipalInsteadOfSpoofedHeader() {
        HttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-User", "administrator");
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("real-user", null, java.util.List.of())
        );

        assertEquals("real-user", CurrentUserResolver.resolveUsername(request));
    }
}
