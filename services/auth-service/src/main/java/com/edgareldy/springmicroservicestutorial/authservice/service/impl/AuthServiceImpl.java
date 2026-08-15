package com.edgareldy.springmicroservicestutorial.authservice.service.impl;

import com.edgareldy.springmicroservicestutorial.authservice.dto.auth.AuthResponse;
import com.edgareldy.springmicroservicestutorial.authservice.dto.auth.LoginRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.auth.RegisterRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.user.UserResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.ActivationToken;
import com.edgareldy.springmicroservicestutorial.authservice.entity.PasswordResetToken;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;
import com.edgareldy.springmicroservicestutorial.authservice.event.AuthEventProducer;
import com.edgareldy.springmicroservicestutorial.authservice.event.PasswordResetRequestedEvent;
import com.edgareldy.springmicroservicestutorial.authservice.event.UserRegisteredEvent;
import com.edgareldy.springmicroservicestutorial.authservice.security.JwtService;
import com.edgareldy.springmicroservicestutorial.authservice.service.ActivationTokenService;
import com.edgareldy.springmicroservicestutorial.authservice.service.AuthService;
import com.edgareldy.springmicroservicestutorial.authservice.service.BlacklistedTokenService;
import com.edgareldy.springmicroservicestutorial.authservice.service.PasswordResetTokenService;
import com.edgareldy.springmicroservicestutorial.authservice.service.UserService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Default {@link AuthService} implementation, orchestrating {@link UserService},
 * {@link ActivationTokenService}, {@link PasswordResetTokenService},
 * {@link BlacklistedTokenService}, {@link JwtService}, and {@link AuthEventProducer}
 * to implement every {@code /api/v1/auth/**} flow.
 * <p>
 * <strong>Publish-after-commit design decision</strong>: {@link #register(RegisterRequest)}
 * and {@link #forgotPassword(String)} each persist state and must publish a
 * Kafka event ({@code UserRegisteredEvent}/{@code PasswordResetRequestedEvent})
 * describing it, but never before or during the persisting transaction (see
 * {@link AuthEventProducer}'s class Javadoc). This class implements that rule
 * with {@code TransactionSynchronizationManager.registerSynchronization(...)},
 * registered from inside the still-active {@code @Transactional} method,
 * with an {@code afterCommit()} callback that calls into
 * {@link AuthEventProducer}. This is the approach chosen over extracting the
 * persistence step into a separate {@code private @Transactional} method
 * called from a non-transactional {@code register()}/{@code forgotPassword()}:
 * that alternative looks reasonable but is actually broken in Spring, since
 * declarative transactions are applied via a proxy, and a private method
 * called from within the same class bypasses the proxy entirely (Spring's
 * well-known "self-invocation" pitfall) - the {@code @Transactional} on that
 * private method would silently do nothing. Routing the publish through
 * {@code registerSynchronization}/{@code afterCommit()} from inside the one
 * public, proxied, {@code @Transactional} method avoids that trap entirely
 * and keeps the whole flow (persist + schedule the publish) in a single
 * method, which is also straightforward to unit-test by driving a real
 * transaction (e.g. with {@code TransactionTemplate}) and asserting the
 * producer is invoked only once that transaction has actually committed,
 * never on rollback.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final ActivationTokenService activationTokenService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final BlacklistedTokenService blacklistedTokenService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuthEventProducer authEventProducer;

    /**
     * Persists the new user and its activation token in one local
     * transaction, then schedules {@code UserRegisteredEvent} to be
     * published only once that transaction commits (see this class'
     * Javadoc for why {@code registerSynchronization}/{@code afterCommit()}
     * is used here instead of a separate private transactional method).
     */
    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        User user = userService.createUser(request);
        ActivationToken activationToken = activationTokenService.generate(user);

        UserRegisteredEvent event = new UserRegisteredEvent(
                user.getId(), user.getEmail(), user.getFirstName(), activationToken.getToken());
        publishAfterCommit(() -> authEventProducer.publishUserRegistered(event));

        return userService.toResponse(user);
    }

    @Override
    @Transactional
    public void activateAccount(String token) {
        User user = activationTokenService.validate(token);
        userService.enableAccount(user.getId());
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        // Delegated entirely to Spring Security: DisabledException (account not
        // yet activated), LockedException, and BadCredentialsException are all
        // allowed to propagate as-is rather than being pre-checked by hand here,
        // so the authentication decision stays in exactly one place.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = (User) authentication.getPrincipal();
        return new AuthResponse(jwtService.generateToken(user));
    }

    @Override
    @Transactional
    public void logout(String rawToken) {
        String email = jwtService.extractUsername(rawToken);
        String jti = jwtService.extractJti(rawToken);
        Instant expiresAt = jwtService.extractExpiration(rawToken);
        User user = userService.findByEmail(email);
        blacklistedTokenService.blacklist(user, rawToken, jti, expiresAt);
    }

    @Override
    public UserResponse me() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findByEmail(email);
        return userService.toResponse(user);
    }

    /**
     * Same publish-after-commit approach as {@link #register(RegisterRequest)},
     * for {@code PasswordResetRequestedEvent}.
     * <p>
     * Looks the user up by email and lets {@code ResourceNotFoundException}
     * propagate if none exists, deliberately the same observable behavior
     * as {@code spring-security-tutorial}'s reference implementation, even
     * though it technically reveals whether a given email is registered. A
     * stricter implementation would return success regardless to avoid
     * email enumeration; left as-is here to match the tutorial's reference
     * behavior rather than silently diverging from it.
     */
    @Override
    @Transactional
    public void forgotPassword(String email) {
        User user = userService.findByEmail(email);
        PasswordResetToken resetToken = passwordResetTokenService.generate(user);

        PasswordResetRequestedEvent event =
                new PasswordResetRequestedEvent(user.getId(), user.getEmail(), resetToken.getToken());
        publishAfterCommit(() -> authEventProducer.publishPasswordResetRequested(event));
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = passwordResetTokenService.validateAndConsume(token);
        userService.updatePassword(user.getId(), newPassword);
    }

    /**
     * Schedules {@code action} to run only once the currently active
     * transaction commits. Must be called from within an already-active
     * {@code @Transactional} method (see this class' Javadoc). Falls back
     * to running {@code action} immediately if no transaction is active,
     * which should not happen in production given every caller here is
     * itself {@code @Transactional}, but keeps this helper safe to call
     * from a test driving these methods outside of a transaction.
     */
    private void publishAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }
}
