package com.ticketmesh.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey key;
    private final SecretKey refreshKey;
    private final long accessExpirationMillis;
    private final long refreshExpirationMillis;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMillis,
                      @Value("${app.jwt.refresh-secret:${app.jwt.secret}}") String refreshSecret,
                      @Value("${app.jwt.refresh-expiration-ms:604800000}") long refreshExpirationMillis) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.refreshKey = refreshSecret.equals(secret)
                ? this.key
                : Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationMillis = expirationMillis;
        this.refreshExpirationMillis = refreshExpirationMillis;
    }

    public String generateAccessToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", TYPE_ACCESS);
        claims.put("role", userDetails.getAuthorities().iterator().next().getAuthority());
        return buildToken(claims, userDetails.getUsername(), key, accessExpirationMillis);
    }

    public String generateRefreshToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", TYPE_REFRESH);
        return buildToken(claims, userDetails.getUsername(), refreshKey, refreshExpirationMillis);
    }

    public String generateAccessToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("type", TYPE_ACCESS);
        if (!claims.containsKey("role")) {
            claims.put("role", userDetails.getAuthorities().iterator().next().getAuthority());
        }
        return buildToken(claims, userDetails.getUsername(), key, accessExpirationMillis);
    }

    /**
     * Generate a special-purpose token (e.g. password reset) with a custom subject
     * and short expiration. The subject is set to a deterministic identifier so
     * the token is bound to the operation rather than the user.
     */
    public String generateOpToken(String operation, String subject, long expirationMillis) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("type", operation);
        return buildToken(claims, subject, key, expirationMillis);
    }

    private String buildToken(Map<String, Object> claims, String subject,
                              SecretKey signingKey, long expirationMillis) {
        Instant now = Instant.now();
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMillis)))
                .signWith(signingKey)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = parseClaims(token, key);
        return resolver.apply(claims);
    }

    public <T> T extractRefreshClaim(String token, Function<Claims, T> resolver) {
        Claims claims = parseClaims(token, refreshKey);
        return resolver.apply(claims);
    }

    private Claims parseClaims(String token, SecretKey signingKey) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isRefreshToken(String token) {
        try {
            String type = extractRefreshClaim(token, c -> c.get("type", String.class));
            return TYPE_REFRESH.equals(type);
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isAccessToken(String token) {
        try {
            String type = extractClaim(token, c -> c.get("type", String.class));
            return TYPE_ACCESS.equals(type);
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractUsername(token);
            return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isRefreshTokenValid(String token, UserDetails userDetails) {
        try {
            String username = extractRefreshClaim(token, Claims::getSubject);
            return username.equals(userDetails.getUsername()) && !isRefreshTokenExpired(token);
        } catch (Exception ex) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private boolean isRefreshTokenExpired(String token) {
        return extractRefreshClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Returns expiration time in milliseconds for access tokens.
     */
    public long getAccessExpirationMillis() {
        return accessExpirationMillis;
    }

    /**
     * Returns expiration time in milliseconds for refresh tokens.
     */
    public long getRefreshExpirationMillis() {
        return refreshExpirationMillis;
    }
}
