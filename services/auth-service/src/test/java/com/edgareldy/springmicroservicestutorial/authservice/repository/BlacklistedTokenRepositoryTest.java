package com.edgareldy.springmicroservicestutorial.authservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmicroservicestutorial.authservice.config.TestcontainersConfig;
import com.edgareldy.springmicroservicestutorial.authservice.entity.BlacklistedToken;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Repository-level tests for {@link BlacklistedTokenRepository}, backed by a
 * real PostgreSQL container ({@link TestcontainersConfig}), covering the
 * {@code jti} membership check used by the JWT filter and the bulk cleanup
 * of tokens that have already naturally expired.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@DataJpaTest
// Spring Boot 4.x's @DataJpaTest slice no longer imports FlywayAutoConfiguration by
// default (unlike 3.x): without this, the schema created by V1__init_schema.sql would
// never be applied against the Testcontainers database and every query below would fail
// with "relation does not exist".
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(TestcontainersConfig.class)
class BlacklistedTokenRepositoryTest {

    @Autowired
    private BlacklistedTokenRepository blacklistedTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User persistUser(String email) {
        return userRepository.save(User.builder()
                .firstName("Margaret")
                .lastName("Hamilton")
                .email(email)
                .password("hashed-password")
                .enabled(true)
                .accountLocked(false)
                .build());
    }

    @Test
    void existsByJti_trueWhenPresent_falseOtherwise() {
        User user = persistUser("margaret@example.com");
        blacklistedTokenRepository.save(BlacklistedToken.builder()
                .user(user)
                .token("a.b.c")
                .jti("jti-123")
                .blacklistedAt(Instant.now())
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build());

        assertThat(blacklistedTokenRepository.existsByJti("jti-123")).isTrue();
        assertThat(blacklistedTokenRepository.existsByJti("jti-unknown")).isFalse();
    }

    @Test
    void deleteAllByExpiresAtBefore_removesOnlyExpiredTokens() {
        User user = persistUser("hedy@example.com");
        BlacklistedToken expired = blacklistedTokenRepository.save(BlacklistedToken.builder()
                .user(user)
                .token("expired.token.value")
                .jti("jti-expired")
                .blacklistedAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .createdAt(Instant.now().minus(2, ChronoUnit.HOURS))
                .expiresAt(Instant.now().minus(1, ChronoUnit.HOURS))
                .build());
        BlacklistedToken valid = blacklistedTokenRepository.save(BlacklistedToken.builder()
                .user(user)
                .token("valid.token.value")
                .jti("jti-valid")
                .blacklistedAt(Instant.now())
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.HOURS))
                .build());

        blacklistedTokenRepository.deleteAllByExpiresAtBefore(Instant.now());

        assertThat(blacklistedTokenRepository.findById(expired.getId())).isEmpty();
        assertThat(blacklistedTokenRepository.findById(valid.getId())).isPresent();
    }
}
