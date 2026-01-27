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
        String secret = System.getenv().getOrDefault("APP_JWT_SECRET", "bXktdmVyeS1sb25nLXNlY3JldC1mb3ItZGV2LXVzZS1vbmx5LWFhYWFh");
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
    }

    public String generate(String subject, long ttlMillis, Map<String, Object> claims) {
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
        return Jwts.parserBuilder().setSigningKey(key()).build().parseClaimsJws(token).getBody();
    }
}



