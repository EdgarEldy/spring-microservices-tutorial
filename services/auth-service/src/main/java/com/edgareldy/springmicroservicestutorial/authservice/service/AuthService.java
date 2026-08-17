package com.edgareldy.springmicroservicestutorial.authservice.service;

import com.edgareldy.springmicroservicestutorial.authservice.dto.auth.AuthResponse;
import com.edgareldy.springmicroservicestutorial.authservice.dto.auth.LoginRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.auth.RegisterRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.user.UserResponse;

/**
 * Contract for the application-level authentication flows exposed by
 * {@code /api/v1/auth/**}: registration, activation, login, logout, the
 * current user's profile, and the password-reset flow. This is the only
 * contract in this service allowed to orchestrate calls across
 * {@link UserService}, {@link ActivationTokenService},
 * {@link PasswordResetTokenService}, {@link BlacklistedTokenService}, and
 * {@code AuthEventProducer}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public interface AuthService {

    /**
     * Creates a disabled user and its activation token, then publishes
     * {@code UserRegisteredEvent} only after that local transaction has
     * committed.
     */
    UserResponse register(RegisterRequest request);

    /** Validates the given activation token and enables the owning account. */
    void activateAccount(String token);

    /** Authenticates the given credentials and returns a signed JWT. */
    AuthResponse login(LoginRequest request);

    /** Blacklists the given JWT so it can no longer be used to authenticate. */
    void logout(String rawToken);

    /** Returns the profile of the currently authenticated user. */
    UserResponse me();

    /**
     * Generates a password-reset token for the given email, then publishes
     * {@code PasswordResetRequestedEvent} only after that local transaction
     * has committed.
     */
    void forgotPassword(String email);

    /** Consumes the given reset token and sets the new password. */
    void resetPassword(String token, String newPassword);
}
