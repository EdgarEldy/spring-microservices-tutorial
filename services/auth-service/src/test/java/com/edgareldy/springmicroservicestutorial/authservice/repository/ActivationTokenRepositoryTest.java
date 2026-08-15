package com.edgareldy.springmicroservicestutorial.authservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmicroservicestutorial.authservice.config.TestcontainersConfig;
import com.edgareldy.springmicroservicestutorial.authservice.entity.ActivationToken;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Repository-level tests for {@link ActivationTokenRepository}, backed by a
 * real PostgreSQL container ({@link TestcontainersConfig}), covering the
 * token lookup (with its associated {@link User}) and the bulk cleanup of
 * expired tokens.
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
class ActivationTokenRepositoryTest {

    @Autowired
    private ActivationTokenRepository activationTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User persistUser(String email) {
        return userRepository.save(User.builder()
                .firstName("Grace")
                .lastName("Hopper")
                .email(email)
                .password("hashed-password")
                .enabled(false)
                .accountLocked(false)
                .build());
    }

    @Test
    void findByToken_existingToken_returnsTokenWithAssociatedUser() {
        User user = persistUser("grace@example.com");
        activationTokenRepository.save(ActivationToken.builder()
                .user(user)
                .token("activation-token-123")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build());

        Optional<ActivationToken> found = activationTokenRepository.findByToken("activation-token-123");

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("grace@example.com");
    }

    @Test
    void findByToken_unknownToken_returnsEmpty() {
        assertThat(activationTokenRepository.findByToken("does-not-exist")).isEmpty();
    }

    @Test
    void deleteAllByExpiresAtBefore_removesOnlyExpiredTokens() {
        User user = persistUser("alan@example.com");
        ActivationToken expired = activationTokenRepository.save(ActivationToken.builder()
                .user(user)
                .token("expired-token")
                .createdAt(Instant.now().minus(2, ChronoUnit.DAYS))
                .expiresAt(Instant.now().minus(1, ChronoUnit.DAYS))
                .build());
        ActivationToken valid = activationTokenRepository.save(ActivationToken.builder()
                .user(user)
                .token("valid-token")
                .createdAt(Instant.now())
                .expiresAt(Instant.now().plus(1, ChronoUnit.DAYS))
                .build());

        activationTokenRepository.deleteAllByExpiresAtBefore(Instant.now());

        assertThat(activationTokenRepository.findById(expired.getId())).isEmpty();
        assertThat(activationTokenRepository.findById(valid.getId())).isPresent();
    }
}
