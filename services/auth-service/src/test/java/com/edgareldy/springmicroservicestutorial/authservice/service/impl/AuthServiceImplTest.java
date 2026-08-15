package com.edgareldy.springmicroservicestutorial.authservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.edgareldy.springmicroservicestutorial.authservice.service.BlacklistedTokenService;
import com.edgareldy.springmicroservicestutorial.authservice.service.PasswordResetTokenService;
import com.edgareldy.springmicroservicestutorial.authservice.service.UserService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Pure Mockito unit tests for {@link AuthServiceImpl}: every collaborator
 * ({@link UserService}, {@link ActivationTokenService},
 * {@link PasswordResetTokenService}, {@link BlacklistedTokenService},
 * {@link JwtService}, {@link AuthenticationManager}, {@link AuthEventProducer})
 * is mocked, no Spring application context is started.
 * <p>
 * <strong>How the publish-after-commit tests work</strong> (the single most
 * important thing tested in this class, per this branch's README task list):
 * {@code AuthServiceImpl.register()}/{@code forgotPassword()} do not call
 * {@link AuthEventProducer} directly. Instead they call
 * {@code TransactionSynchronizationManager.registerSynchronization(...)} with
 * an {@code afterCommit()} callback that calls the producer, so the producer
 * is only ever reached once whatever real {@code PlatformTransactionManager}
 * is driving the surrounding {@code @Transactional} method decides to commit.
 * A pure Mockito test has no real transaction manager and cannot itself
 * commit or roll back anything; three approaches were considered to still
 * exercise this behavior meaningfully:
 * <ol>
 *     <li>Stand up a real {@code PlatformTransactionManager} (e.g.
 *     {@code DataSourceTransactionManager} over an in-memory H2/HSQLDB
 *     datasource used purely to drive the transaction lifecycle) and run
 *     {@code register()} inside a {@code TransactionTemplate}. This would
 *     work, but adds a new test dependency (an embedded JDBC driver this
 *     module does not otherwise need, since its real persistence tests use
 *     Testcontainers PostgreSQL, not H2) and a fair amount of setup for a
 *     unit test whose only job is to check the shape of one method's control
 *     flow.</li>
 *     <li>{@code @SpringBootTest} with a minimal context and a real
 *     transaction manager. Correct, but this is exactly the kind of test this
 *     class is deliberately not: it would pull in a Spring context, defeating
 *     the "pure Mockito, no Spring context" pattern used for every other test
 *     in this package, for a scenario that does not actually need a real
 *     database or bean wiring to prove its point.</li>
 *     <li><strong>(chosen)</strong> Drive
 *     {@code TransactionSynchronizationManager} directly, exactly the way
 *     Spring's own {@code TransactionInterceptor} would around the
 *     {@code @Transactional} method, without any
 *     {@code PlatformTransactionManager} or real transaction at all:
 *     {@code initSynchronization()} before calling {@code register()}/
 *     {@code forgotPassword()} (this is what makes
 *     {@code TransactionSynchronizationManager.isSynchronizationActive()}
 *     return {@code true} inside {@code AuthServiceImpl.publishAfterCommit},
 *     the same condition a real {@code @Transactional} proxy would have
 *     satisfied), assert the producer has not been invoked yet (proving the
 *     call is deferred, not immediate), then manually invoke
 *     {@code afterCommit()} on every registered
 *     {@link TransactionSynchronization} (simulating what Spring's
 *     {@code TransactionSynchronizationUtils.invokeAfterCommit} does once the
 *     real transaction manager commits) and assert the producer is invoked
 *     exactly once, with the right event payload, only then. This is the
 *     narrowest possible test of the actual contract
 *     ("schedule for after commit, do not call directly") without dragging in
 *     a transaction manager or a Spring context, and it is also literally how
 *     the production code was written to be tested, per this class'
 *     {@code publishAfterCommit} Javadoc.</li>
 * </ol>
 * A companion test per publishing method also exercises the rollback path
 * (invoking {@code afterCompletion(STATUS_ROLLED_BACK)} instead of
 * {@code afterCommit()}) to make the "never before, and never instead of,
 * commit" half of the contract explicit, not just "eventually called".
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private ActivationTokenService activationTokenService;

    @Mock
    private PasswordResetTokenService passwordResetTokenService;

    @Mock
    private BlacklistedTokenService blacklistedTokenService;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private AuthEventProducer authEventProducer;

    @InjectMocks
    private AuthServiceImpl authService;

    @AfterEach
    void cleanUpTransactionSynchronizationAndSecurityContext() {
        // Defensive cleanup: if a test fails/throws before reaching its own
        // clearSynchronization()/clearContext() call, later tests in this class
        // (and in the same JVM/thread, since both are ThreadLocal-backed) must
        // not inherit leftover synchronizations or an authenticated principal.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        SecurityContextHolder.clearContext();
    }

    // --- register() ---------------------------------------------------

    @Test
    void register_nominal_createsUserAndActivationTokenAndReturnsResponse() {
        RegisterRequest request = new RegisterRequest("Ada", "Lovelace", "ada@example.com", "raw-password");
        User user = User.builder().id(1L).firstName("Ada").lastName("Lovelace").email("ada@example.com").build();
        ActivationToken activationToken = ActivationToken.builder().token("activation-token").build();
        UserResponse response = new UserResponse(1L, "Ada", "Lovelace", "ada@example.com", false, false, List.of());

        when(userService.createUser(request)).thenReturn(user);
        when(activationTokenService.generate(user)).thenReturn(activationToken);
        when(userService.toResponse(user)).thenReturn(response);

        UserResponse result = authService.register(request);

        assertThat(result).isEqualTo(response);
        verify(userService).createUser(request);
        verify(activationTokenService).generate(user);
    }

    @Test
    void register_publishesUserRegisteredEvent_onlyAfterTransactionCommits() {
        RegisterRequest request = new RegisterRequest("Ada", "Lovelace", "ada@example.com", "raw-password");
        User user = User.builder().id(1L).firstName("Ada").lastName("Lovelace").email("ada@example.com").build();
        ActivationToken activationToken = ActivationToken.builder().token("activation-token").build();
        UserResponse response = new UserResponse(1L, "Ada", "Lovelace", "ada@example.com", false, false, List.of());

        when(userService.createUser(request)).thenReturn(user);
        when(activationTokenService.generate(user)).thenReturn(activationToken);
        when(userService.toResponse(user)).thenReturn(response);

        TransactionSynchronizationManager.initSynchronization();
        try {
            authService.register(request);

            // (a) Before the transaction commits, the producer must never have been called.
            verify(authEventProducer, never()).publishUserRegistered(any());

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(1);
            synchronizations.forEach(TransactionSynchronization::afterCommit);

            // (b) Once (and only once) the transaction has committed, the producer is
            // invoked exactly once, with the expected event payload.
            ArgumentCaptor<UserRegisteredEvent> captor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
            verify(authEventProducer, times(1)).publishUserRegistered(captor.capture());
            UserRegisteredEvent published = captor.getValue();
            assertThat(published.userId()).isEqualTo(1L);
            assertThat(published.email()).isEqualTo("ada@example.com");
            assertThat(published.firstName()).isEqualTo("Ada");
            assertThat(published.activationToken()).isEqualTo("activation-token");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void register_rolledBackTransaction_neverPublishesUserRegisteredEvent() {
        RegisterRequest request = new RegisterRequest("Ada", "Lovelace", "ada@example.com", "raw-password");
        User user = User.builder().id(1L).firstName("Ada").lastName("Lovelace").email("ada@example.com").build();
        ActivationToken activationToken = ActivationToken.builder().token("activation-token").build();
        UserResponse response = new UserResponse(1L, "Ada", "Lovelace", "ada@example.com", false, false, List.of());

        when(userService.createUser(request)).thenReturn(user);
        when(activationTokenService.generate(user)).thenReturn(activationToken);
        when(userService.toResponse(user)).thenReturn(response);

        TransactionSynchronizationManager.initSynchronization();
        try {
            authService.register(request);

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            // Simulate a rollback: the real transaction manager would call
            // afterCompletion(STATUS_ROLLED_BACK) and never afterCommit() at all.
            synchronizations.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

            verify(authEventProducer, never()).publishUserRegistered(any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // --- activateAccount() ---------------------------------------------------

    @Test
    void activateAccount_validatesTokenAndEnablesOwningAccount() {
        User user = User.builder().id(1L).build();
        when(activationTokenService.validate("raw-token")).thenReturn(user);

        authService.activateAccount("raw-token");

        verify(activationTokenService).validate("raw-token");
        verify(userService).enableAccount(1L);
    }

    // --- login() ---------------------------------------------------

    @Test
    void login_delegatesToAuthenticationManagerAndReturnsSignedToken() {
        LoginRequest request = new LoginRequest("ada@example.com", "raw-password");
        User principal = User.builder().id(1L).email("ada@example.com").build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtService.generateToken(principal)).thenReturn("signed-jwt");

        AuthResponse response = authService.login(request);

        assertThat(response).isEqualTo(new AuthResponse("signed-jwt"));

        ArgumentCaptor<UsernamePasswordAuthenticationToken> captor =
                ArgumentCaptor.forClass(UsernamePasswordAuthenticationToken.class);
        verify(authenticationManager).authenticate(captor.capture());
        assertThat(captor.getValue().getPrincipal()).isEqualTo("ada@example.com");
        assertThat(captor.getValue().getCredentials()).isEqualTo("raw-password");
        verify(jwtService).generateToken(principal);
    }

    // --- logout() ---------------------------------------------------

    @Test
    void logout_extractsClaimsAndBlacklistsToken() {
        String rawToken = "raw-jwt";
        User user = User.builder().id(1L).email("ada@example.com").build();
        Instant expiresAt = Instant.now().plusSeconds(3600);

        when(jwtService.extractUsername(rawToken)).thenReturn("ada@example.com");
        when(jwtService.extractJti(rawToken)).thenReturn("jti-123");
        when(jwtService.extractExpiration(rawToken)).thenReturn(expiresAt);
        when(userService.findByEmail("ada@example.com")).thenReturn(user);

        authService.logout(rawToken);

        verify(blacklistedTokenService).blacklist(user, rawToken, "jti-123", expiresAt);
    }

    // --- me() ---------------------------------------------------

    @Test
    void me_readsAuthenticatedEmailFromSecurityContextAndReturnsProfile() {
        User user = User.builder().id(1L).email("ada@example.com").build();
        UserResponse response = new UserResponse(1L, "Ada", "Lovelace", "ada@example.com", true, false, List.of());
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "ada@example.com", null, List.of());
        SecurityContext securityContext = new SecurityContextImpl(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(userService.findByEmail("ada@example.com")).thenReturn(user);
        when(userService.toResponse(user)).thenReturn(response);

        UserResponse result = authService.me();

        assertThat(result).isEqualTo(response);
    }

    // --- forgotPassword() ---------------------------------------------------

    @Test
    void forgotPassword_publishesPasswordResetRequestedEvent_onlyAfterTransactionCommits() {
        User user = User.builder().id(1L).email("ada@example.com").build();
        PasswordResetToken resetToken = PasswordResetToken.builder().token("reset-token").build();

        when(userService.findByEmail("ada@example.com")).thenReturn(user);
        when(passwordResetTokenService.generate(user)).thenReturn(resetToken);

        TransactionSynchronizationManager.initSynchronization();
        try {
            authService.forgotPassword("ada@example.com");

            // (c) Symmetric to register(): not called before commit...
            verify(authEventProducer, never()).publishPasswordResetRequested(any());

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertThat(synchronizations).hasSize(1);
            synchronizations.forEach(TransactionSynchronization::afterCommit);

            // ...called exactly once, with the right payload, once committed.
            ArgumentCaptor<PasswordResetRequestedEvent> captor =
                    ArgumentCaptor.forClass(PasswordResetRequestedEvent.class);
            verify(authEventProducer, times(1)).publishPasswordResetRequested(captor.capture());
            PasswordResetRequestedEvent published = captor.getValue();
            assertThat(published.userId()).isEqualTo(1L);
            assertThat(published.email()).isEqualTo("ada@example.com");
            assertThat(published.resetToken()).isEqualTo("reset-token");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void forgotPassword_rolledBackTransaction_neverPublishesPasswordResetRequestedEvent() {
        User user = User.builder().id(1L).email("ada@example.com").build();
        PasswordResetToken resetToken = PasswordResetToken.builder().token("reset-token").build();

        when(userService.findByEmail("ada@example.com")).thenReturn(user);
        when(passwordResetTokenService.generate(user)).thenReturn(resetToken);

        TransactionSynchronizationManager.initSynchronization();
        try {
            authService.forgotPassword("ada@example.com");

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            synchronizations.forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

            verify(authEventProducer, never()).publishPasswordResetRequested(any());
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    // --- resetPassword() ---------------------------------------------------

    @Test
    void resetPassword_consumesTokenAndUpdatesOwningUsersPassword() {
        User user = User.builder().id(1L).build();
        when(passwordResetTokenService.validateAndConsume("raw-token")).thenReturn(user);

        authService.resetPassword("raw-token", "new-raw-password");

        verify(passwordResetTokenService).validateAndConsume("raw-token");
        verify(userService).updatePassword(1L, "new-raw-password");
    }
}
