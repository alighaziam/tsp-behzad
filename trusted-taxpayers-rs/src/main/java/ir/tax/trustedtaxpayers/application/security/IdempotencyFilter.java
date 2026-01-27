package ir.tax.trustedtaxpayers.application.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class IdempotencyFilter extends OncePerRequestFilter {
    private static class Entry { long at; @SuppressWarnings("unused") int status; }
    private final Map<String, Entry> store = new ConcurrentHashMap<>();
    private final long ttlMillis = 5 * 60_000L; // 5 minutes

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (!isIdempotentPath(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        String key = request.getHeader("Idempotency-Key");
        if (key == null || key.isBlank()) {
            response.setStatus(HttpStatus.BAD_REQUEST.value());
            response.getWriter().write("Missing Idempotency-Key header");
            return;
        }
        String scopedKey = request.getMethod() + "|" + request.getRequestURI() + "|" + key;
        long now = Instant.now().toEpochMilli();
        Entry existing = store.get(scopedKey);
        if (existing != null && now - existing.at < ttlMillis) {
            response.setStatus(HttpStatus.CONFLICT.value());
            response.getWriter().write("Duplicate request");
            return;
        }
        filterChain.doFilter(request, response);
        Entry e = new Entry(); e.at = now; e.status = response.getStatus();
        store.put(scopedKey, e);
    }

    private boolean isIdempotentPath(HttpServletRequest request) {
        String p = request.getRequestURI();
        return p.startsWith("/api/invoices/") && (p.endsWith("/issue") || p.endsWith("/cancel") || p.endsWith("/refund"));
    }
}



