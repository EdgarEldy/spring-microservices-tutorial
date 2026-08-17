package com.edgareldy.springmicroservicestutorial.apigateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Verifies the signature/expiration of JWTs issued by {@code auth-service}, using the same
 * shared HMAC secret. Duplicated from every other business service's class of the same name
 * (see their own Javadoc for the "why not common-lib" rationale, which applies here
 * unchanged). Unlike those services, this one never reads a claim out of the token: the
 * gateway's job per the README is only to reject a request whose token does not verify, the
 * actual claims (roles, permissions) are read back out downstream by whichever service
 * receives the forwarded {@code Authorization} header, per the README's "validated at the
 * gateway and again at each service" rule.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Component
public class JwtService {

    private final SecretKey key;

    public JwtService(@Value("${jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Throws {@link io.jsonwebtoken.JwtException} on a missing/tampered signature or an
     * expired token; returns normally (the parsed claims are discarded) when the token is
     * valid.
     */
    public void validate(String token) {
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }
}
