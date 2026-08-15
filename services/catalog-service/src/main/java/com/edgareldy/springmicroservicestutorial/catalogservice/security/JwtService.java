package com.edgareldy.springmicroservicestutorial.catalogservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Parses and verifies the JWTs issued by {@code auth-service}, using the
 * same shared HMAC secret. Unlike {@code auth-service}'s own
 * {@code JwtService}, this one never signs a token: {@code catalog-service}
 * has no login endpoint, no {@code users} table, and no
 * {@code UserDetailsService} to load a principal from - it only needs to
 * trust and read the claims a request already carries, per the README's
 * "validated at the gateway and again at each service" rule.
 * <p>
 * Claims read: {@code sub} (email, used as the principal's name), {@code
 * roles} (upper-cased role names, no {@code ROLE_} prefix yet at this
 * point, see {@link JwtAuthFilter}), {@code permissions} (upper-cased
 * {@code RESOURCE:ACTION} pairs). There is deliberately no default value
 * for {@code jwt.secret} ({@code @Value("${jwt.secret}")}, not {@code
 * @Value("${jwt.secret:...}")}): a missing value must fail service startup
 * loudly rather than silently trusting tokens signed under a guessable
 * fallback.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Component
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        List<String> roles = (List<String>) parseClaims(token).get("roles", List.class);
        return roles == null ? Collections.emptyList() : roles;
    }

    @SuppressWarnings("unchecked")
    public List<String> extractPermissions(String token) {
        List<String> permissions = (List<String>) parseClaims(token).get("permissions", List.class);
        return permissions == null ? Collections.emptyList() : permissions;
    }

    // Verifies the signature (throws JwtException on tampering/expiry) before
    // handing back the payload: callers never see an unverified claim set.
    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
