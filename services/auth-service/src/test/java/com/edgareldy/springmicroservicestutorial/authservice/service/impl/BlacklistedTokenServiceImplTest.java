package com.edgareldy.springmicroservicestutorial.authservice.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springmicroservicestutorial.authservice.entity.BlacklistedToken;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;
import com.edgareldy.springmicroservicestutorial.authservice.repository.BlacklistedTokenRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pure Mockito unit tests for {@link BlacklistedTokenServiceImpl}:
 * {@link BlacklistedTokenRepository} is mocked, no Spring context and no
 * database, complementing the Testcontainers-backed
 * {@code BlacklistedTokenRepositoryTest}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@ExtendWith(MockitoExtension.class)
class BlacklistedTokenServiceImplTest {

    @Mock
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @InjectMocks
    private BlacklistedTokenServiceImpl blacklistedTokenService;

    @Test
    void blacklist_savesTokenWithGivenUserJtiAndExpiry() {
        User user = User.builder().id(1L).email("ada@example.com").build();
        Instant expiresAt = Instant.now().plus(1, ChronoUnit.HOURS);
        when(blacklistedTokenRepository.save(any(BlacklistedToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        blacklistedTokenService.blacklist(user, "raw-jwt", "jti-123", expiresAt);

        ArgumentCaptor<BlacklistedToken> captor = ArgumentCaptor.forClass(BlacklistedToken.class);
        verify(blacklistedTokenRepository).save(captor.capture());
        BlacklistedToken saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getToken()).isEqualTo("raw-jwt");
        assertThat(saved.getJti()).isEqualTo("jti-123");
        assertThat(saved.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(saved.getBlacklistedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void isBlacklisted_delegatesToExistsByJti() {
        when(blacklistedTokenRepository.existsByJti("jti-123")).thenReturn(true);
        when(blacklistedTokenRepository.existsByJti("jti-999")).thenReturn(false);

        assertThat(blacklistedTokenService.isBlacklisted("jti-123")).isTrue();
        assertThat(blacklistedTokenService.isBlacklisted("jti-999")).isFalse();
    }
}
