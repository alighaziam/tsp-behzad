package ir.tax.trustedtaxpayers.application.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.springframework.security.web.context.RequestAttributeSecurityContextRepository.DEFAULT_REQUEST_ATTR_NAME;

public class JwtAuthFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthFilter.class);
    private final JwtService jwtService;
    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();
    public JwtAuthFilter(JwtService jwtService) { this.jwtService = jwtService; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String auth = Optional.ofNullable(request.getHeader("Authorization")).orElse("");
        if (auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                Claims claims = jwtService.parse(token);
                String sub = claims.getSubject();
                @SuppressWarnings("unchecked") List<String> roles = (List<String>) claims.getOrDefault("roles", List.of());
                var authToken = new UsernamePasswordAuthenticationToken(sub, null,
                        roles.stream().map(SimpleGrantedAuthority::new).toList());
                //SecurityContextHolder.getContext().setAuthentication(authToken);
                securityContextHolderStrategy.getContext().setAuthentication(authToken);
                securityContextHolderStrategy.getDeferredContext().get().setAuthentication(authToken);
                request.setAttribute(DEFAULT_REQUEST_ATTR_NAME, securityContextHolderStrategy.getContext());
            } catch (Exception e) {
                logger.debug("Failed to parse JWT token: {}", e.getMessage());
                // Continue filter chain without authentication - let downstream filters handle it
            }
        }
        filterChain.doFilter(request, response);
    }
}



