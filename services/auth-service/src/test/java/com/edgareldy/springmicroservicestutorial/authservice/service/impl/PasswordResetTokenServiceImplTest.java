package com.edgareldy.springmicroservicestutorial.authservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springmicroservicestutorial.authservice.entity.PasswordResetToken;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;
import com.edgareldy.springmicroservicestutorial.authservice.exception.InvalidTokenException;
import com.edgareldy.springmicroservicestutorial.authservice.repository.PasswordResetTokenRepository;
import com.edgareldy.springmicroservicestutorial.authservice.security.SecureTokenGenerator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure Mockito unit tests for {@link PasswordResetTokenServiceImpl}:
 * {@link PasswordResetTokenRepository} and {@link SecureTokenGenerator} are
 * mocked, no Spring context and no database, complementing the
 * Testcontainers-backed {@code PasswordResetTokenRepositoryTest}.
 * <p>
 * {@link #validateAndConsume_expiredToken_deletesBeforeThrowing()} below
 * specifically exercises the "single-use even on a failed attempt" contract
 * documented on {@link PasswordResetTokenServiceImpl#validateAndConsume(String)}:
 * a plain Mockito unit test cannot observe {@code @Transactional(propagation =
 * REQUIRES_NEW)} itself (there is no real transaction manager here, mocks do
 * not participate in transactions), so it cannot prove the delete survives a
 * later rollback of the caller's transaction. What it can and does prove is
 * the actual code path this class is responsible for: {@code delete(...)} is
 * invoked, in order, strictly before {@link InvalidTokenException} is thrown
 * for an expired token, using Mockito's {@link InOrder}. That ordering is the
 * necessary condition for {@code REQUIRES_NEW} to actually help; if the delete
 * happened after the throw (or not at all), no propagation setting could save
 * "single-use" semantics.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@ExtendWith(MockitoExtension.class)
class PasswordResetTokenServiceImplTest {

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private SecureTokenGenerator secureTokenGenerator;

    @InjectMocks
    private PasswordResetTokenServiceImpl passwordResetTokenService;

    @Test
    void generate_createsPasswordResetTokenExpiring1HourFromNow() {
        User user = User.builder().id(1L).email("ada@example.com").build();
        when(secureTokenGenerator.generate()).thenReturn("raw-token");
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Instant before = Instant.now();
        PasswordResetToken created = passwordResetTokenService.generate(user);
        Instant after = Instant.now();

        assertThat(created.getUser()).isEqualTo(user);
        assertThat(created.getToken()).isEqualTo("raw-token");
        assertThat(created.getType()).isEqualTo("PASSWORD_RESET");
        assertThat(created.getExpiryDate())
                .isBetween(before.plus(1, ChronoUnit.HOURS), after.plus(1, ChronoUnit.HOURS));
    }

    @Test
    void validateAndConsume_validUnexpiredToken_deletesAndReturnsUser() {
        User user = User.builder().id(1L).email("ada@example.com").build();
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .user(user)
                .token("raw-token")
                .type("PASSWORD_RESET")
                .expiryDate(Instant.now().plus(30, ChronoUnit.MINUTES))
                .build();
        when(passwordResetTokenRepository.findByToken("raw-token")).thenReturn(Optional.of(token));

        User result = passwordResetTokenService.validateAndConsume("raw-token");

        assertThat(result).isEqualTo(user);
        verify(passwordResetTokenRepository, times(1)).delete(token);
    }

    @Test
    void validateAndConsume_tokenNotFound_throwsInvalidTokenExceptionAndNeverDeletes() {
        when(passwordResetTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThatExceptionOfType(InvalidTokenException.class)
                .isThrownBy(() -> passwordResetTokenService.validateAndConsume("missing"));

        verify(passwordResetTokenRepository, never()).delete(any());
    }

    @Test
    void validateAndConsume_expiredToken_deletesBeforeThrowing() {
        PasswordResetToken token = PasswordResetToken.builder()
                .id(1L)
                .token("raw-token")
                .type("PASSWORD_RESET")
                .expiryDate(Instant.now().minus(1, ChronoUnit.MINUTES))
                .build();
        when(passwordResetTokenRepository.findByToken("raw-token")).thenReturn(Optional.of(token));

        assertThatExceptionOfType(InvalidTokenException.class)
                .isThrownBy(() -> passwordResetTokenService.validateAndConsume("raw-token"));

        // The delete must still have happened exactly once, and strictly before
        // findByToken's result was inspected for expiry, i.e. the token is consumed
        // regardless of whether validation then succeeds or fails.
        InOrder inOrder = inOrder(passwordResetTokenRepository);
        inOrder.verify(passwordResetTokenRepository).findByToken("raw-token");
        inOrder.verify(passwordResetTokenRepository).delete(token);
        verify(passwordResetTokenRepository, times(1)).delete(token);
    }
}
