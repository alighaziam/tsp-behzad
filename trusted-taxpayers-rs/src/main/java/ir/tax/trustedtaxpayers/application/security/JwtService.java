package ir.tax.trustedtaxpayers.application.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    // provide base64 secret via env APP_JWT_SECRET (>=256-bit for HS256)
    private Key key() {
        String secret = System.getenv("APP_JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("APP_JWT_SECRET must be configured; no default signing secret is allowed");
        }
        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        } catch (RuntimeException e) {
            throw new IllegalStateException("APP_JWT_SECRET must be valid Base64 and contain at least 256 bits", e);
        }
    }

    public String generate(String subject, long ttlMillis, Map<String, Object> claims) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("JWT subject must not be blank");
        }
        if (ttlMillis <= 0) {
            throw new IllegalArgumentException("JWT time-to-live must be greater than zero");
        }
        if (claims == null) {
            throw new IllegalArgumentException("JWT claims must not be null");
        }
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setSubject(subject)
                .addClaims(claims)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + ttlMillis))
                .signWith(key(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims parse(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token).getBody();
        if (claims.getExpiration() == null) {
            throw new IllegalArgumentException("JWT expiration claim is required");
        }
        if (claims.getSubject() == null || claims.getSubject().isBlank()) {
            throw new IllegalArgumentException("JWT subject claim is required");
        }
        return claims;
    }
}



