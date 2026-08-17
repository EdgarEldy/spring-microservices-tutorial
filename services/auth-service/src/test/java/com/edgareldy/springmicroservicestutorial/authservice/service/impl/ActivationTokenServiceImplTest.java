package com.edgareldy.springmicroservicestutorial.authservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springmicroservicestutorial.authservice.entity.ActivationToken;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;
import com.edgareldy.springmicroservicestutorial.authservice.exception.InvalidTokenException;
import com.edgareldy.springmicroservicestutorial.authservice.repository.ActivationTokenRepository;
import com.edgareldy.springmicroservicestutorial.authservice.security.SecureTokenGenerator;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure Mockito unit tests for {@link ActivationTokenServiceImpl}:
 * {@link ActivationTokenRepository} and {@link SecureTokenGenerator} are
 * mocked, no Spring context and no database, complementing the
 * Testcontainers-backed {@code ActivationTokenRepositoryTest}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@ExtendWith(MockitoExtension.class)
class ActivationTokenServiceImplTest {

    @Mock
    private ActivationTokenRepository activationTokenRepository;

    @Mock
    private SecureTokenGenerator secureTokenGenerator;

    @InjectMocks
    private ActivationTokenServiceImpl activationTokenService;

    @Test
    void generate_createsTokenExpiring24HoursFromNow() {
        User user = User.builder().id(1L).email("ada@example.com").build();
        when(secureTokenGenerator.generate()).thenReturn("raw-token");
        when(activationTokenRepository.save(any(ActivationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Instant before = Instant.now();
        ActivationToken created = activationTokenService.generate(user);
        Instant after = Instant.now();

        assertThat(created.getUser()).isEqualTo(user);
        assertThat(created.getToken()).isEqualTo("raw-token");
        assertThat(created.getCreatedAt()).isBetween(before, after);
        // expiresAt must be exactly createdAt + 24h, checked via the elapsed duration
        // rather than a fixed clock, since Instant.now() is called live inside generate().
        assertThat(created.getExpiresAt())
                .isEqualTo(created.getCreatedAt().plus(24, ChronoUnit.HOURS));
    }

    @Test
    void validate_validUnusedUnexpiredToken_marksValidatedAndReturnsUser() {
        User user = User.builder().id(1L).email("ada@example.com").build();
        ActivationToken token = ActivationToken.builder()
                .id(1L)
                .user(user)
                .token("raw-token")
                .createdAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .expiresAt(Instant.now().plus(23, ChronoUnit.HOURS))
                .validatedAt(null)
                .build();
        when(activationTokenRepository.findByToken("raw-token")).thenReturn(Optional.of(token));
        when(activationTokenRepository.save(token)).thenReturn(token);

        User result = activationTokenService.validate("raw-token");

        assertThat(result).isEqualTo(user);
        assertThat(token.getValidatedAt()).isNotNull();
        ArgumentCaptor<ActivationToken> captor = ArgumentCaptor.forClass(ActivationToken.class);
        verify(activationTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getValidatedAt()).isNotNull();
    }

    @Test
    void validate_tokenNotFound_throwsInvalidTokenException() {
        when(activationTokenRepository.findByToken("missing")).thenReturn(Optional.empty());

        assertThatExceptionOfType(InvalidTokenException.class)
                .isThrownBy(() -> activationTokenService.validate("missing"));

        verify(activationTokenRepository, never()).save(any());
    }

    @Test
    void validate_alreadyValidatedToken_throwsInvalidTokenExceptionAndNeverSavesAgain() {
        ActivationToken token = ActivationToken.builder()
                .id(1L)
                .token("raw-token")
                .createdAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .expiresAt(Instant.now().plus(23, ChronoUnit.HOURS))
                .validatedAt(Instant.now().minus(30, ChronoUnit.MINUTES))
                .build();
        when(activationTokenRepository.findByToken("raw-token")).thenReturn(Optional.of(token));

        assertThatExceptionOfType(InvalidTokenException.class)
                .isThrownBy(() -> activationTokenService.validate("raw-token"));

        verify(activationTokenRepository, never()).save(any());
    }

    @Test
    void validate_expiredToken_throwsInvalidTokenExceptionAndNeverSaves() {
        ActivationToken token = ActivationToken.builder()
                .id(1L)
                .token("raw-token")
                .createdAt(Instant.now().minus(25, ChronoUnit.HOURS))
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .validatedAt(null)
                .build();
        when(activationTokenRepository.findByToken("raw-token")).thenReturn(Optional.of(token));

        assertThatExceptionOfType(InvalidTokenException.class)
                .isThrownBy(() -> activationTokenService.validate("raw-token"));

        verify(activationTokenRepository, never()).save(any());
    }
}
