package com.edgareldy.springmicroservicestutorial.authservice.security;

import com.edgareldy.springmicroservicestutorial.authservice.entity.Permission;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Signs the JWTs issued on login and parses/verifies the JWTs presented on
 * every subsequent request, in this service and in every other business
 * service that validates them independently (`api-gateway`,
 * `catalog-service`, `customer-service`, `order-service`).
 * <p>
 * The signing key ({@code jwt.secret}, HMAC) is a single shared secret: it
 * must be configured identically, byte for byte, in every service's
 * `application.yml` (via `config-server`), because each service verifies the
 * token's signature independently rather than delegating verification back
 * to `auth-service`. Rotating this secret invalidates every JWT issued under
 * the previous one and requires updating the shared config in lockstep
 * across all services. There is deliberately no default value here
 * ({@code @Value("${jwt.secret}")}, not {@code @Value("${jwt.secret:...}")}):
 * a missing value must fail service startup loudly rather than silently
 * signing tokens with a guessable fallback.
 * <p>
 * Claims carried by the token: {@code sub} (email), {@code jti} (random id,
 * checked against {@code blacklisted_tokens} on logout), {@code roles}
 * (upper-cased role names), {@code permissions} (upper-cased
 * {@code RESOURCE:ACTION} pairs), {@code iat}, {@code exp}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Component
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;
    private final SecureTokenGenerator secureTokenGenerator;

    public JwtService(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs,
            SecureTokenGenerator secureTokenGenerator) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.secureTokenGenerator = secureTokenGenerator;
    }

    /**
     * Builds and signs a new JWT for the given user. The {@code jti} comes
     * from {@link SecureTokenGenerator} (not a UUID) so it stays consistent
     * with the other token flows in this service, and so it is
     * unguessable enough to be safely stored/matched in
     * {@code blacklisted_tokens} on logout.
     */
    public String generateToken(User user) {
        Instant now = Instant.now();
        List<String> roles = user.getRoles().stream()
                .map(role -> role.getRoleName().toUpperCase(Locale.ROOT))
                .toList();
        List<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(this::permissionClaim)
                .distinct()
                .toList();
        return Jwts.builder()
                .subject(user.getEmail())
                .id(secureTokenGenerator.generate())
                .claim("roles", roles)
                .claim("permissions", permissions)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(key)
                .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    public Instant extractExpiration(String token) {
        return parseClaims(token).getExpiration().toInstant();
    }

    private String permissionClaim(Permission permission) {
        return permission.getResource().toUpperCase(Locale.ROOT) + ":" + permission.getAction().toUpperCase(Locale.ROOT);
    }

    // Verifies the signature (throws JwtException on tampering/expiry) before
    // handing back the payload: callers never see an unverified claim set.
    private Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
