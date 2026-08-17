package com.edgareldy.springmicroservicestutorial.authservice.security;

import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Component;

/**
 * Generates cryptographically secure random tokens shared by
 * {@code JwtService} (the {@code jti} claim), the activation-token flow, and
 * the password-reset-token flow, so no part of this service ever falls back
 * to a non-cryptographic random source for a security-sensitive token.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Component
public class SecureTokenGenerator {

    private static final int TOKEN_BYTES = 32;

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Returns a fresh 32-byte value from {@link SecureRandom}, Base64
     * URL-safe encoded without padding so the result is directly usable in
     * a URL query parameter (activation/reset links) without escaping.
     */
    public String generate() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
