package ir.tax.trustedtaxpayers.interfaces.rest.filters;

/**
 * Author: Behzad Farajpour
 * Project: TSP
 * Email: farajpour-b@hamfekrangroup.ir
 */

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SecurityContextUsernameFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String previous = MDC.get("username");
        try {
            String username = resolveUsername((HttpServletRequest) request);
            if (username != null && !username.isBlank()) {
                MDC.put("username", username);
            }
            chain.doFilter(request, response);
        } finally {
            if (previous == null) {
                MDC.remove("username");
            } else {
                MDC.put("username", previous);
            }
        }
    }

    private String resolveUsername(HttpServletRequest req) {
        // 1) Custom header (e.g., set by gateway)
        String headerUser = req.getHeader("X-User");
        if (headerUser != null && !headerUser.isBlank()) {
            return headerUser;
        }
        // 2) Spring Security (if present on classpath)
        try {
            Class<?> ctxClass = Class.forName("org.springframework.security.core.context.SecurityContextHolder");
            Object context = ctxClass.getMethod("getContext").invoke(null);
            if (context != null) {
                Object auth = context.getClass().getMethod("getAuthentication").invoke(context);
                if (auth != null) {
                    Object name = auth.getClass().getMethod("getName").invoke(auth);
                    if (name != null) {
                        return String.valueOf(name);
                    }
                }
            }
        } catch (Throwable ignored) {
            // Spring Security not available or not authenticated
        }
        return null;
    }
}


