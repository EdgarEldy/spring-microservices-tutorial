package com.edgareldy.springmicroservicestutorial.authservice.service;

import com.edgareldy.springmicroservicestutorial.authservice.entity.PasswordResetToken;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;

/**
 * Contract for issuing and consuming {@link PasswordResetToken}s, the
 * single-use link that lets a user set a new password without knowing the
 * old one.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public interface PasswordResetTokenService {

    /**
     * Generates and persists a new {@code PASSWORD_RESET} token for the
     * given user, expiring 1 hour from now.
     */
    PasswordResetToken generate(User user);

    /**
     * Consumes a raw token value: the matching row is deleted immediately,
     * single-use even on a failed attempt, then throws
     * {@code InvalidTokenException} if it did not exist or had already
     * expired; otherwise returns the owning {@link User}.
     */
    User validateAndConsume(String rawToken);
}
