package com.edgareldy.springmicroservicestutorial.authservice.service;

import com.edgareldy.springmicroservicestutorial.authservice.entity.BlacklistedToken;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;
import java.time.Instant;

/**
 * Contract for recording and checking {@link BlacklistedToken}s, so a JWT
 * that has been explicitly logged out of can be rejected before its natural
 * expiration.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public interface BlacklistedTokenService {

    /** Records the given JWT as blacklisted for the given user, effective immediately. */
    void blacklist(User user, String rawToken, String jti, Instant expiresAt);

    /** Returns whether a JWT with this {@code jti} has been blacklisted. */
    boolean isBlacklisted(String jti);
}
