package com.edgareldy.springmicroservicestutorial.authservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmicroservicestutorial.authservice.config.TestcontainersConfig;
import com.edgareldy.springmicroservicestutorial.authservice.entity.PasswordResetToken;
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
 * Repository-level tests for {@link PasswordResetTokenRepository}, backed by
 * a real PostgreSQL container ({@link TestcontainersConfig}), covering the
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
class PasswordResetTokenRepositoryTest {

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private User persistUser(String email) {
        return userRepository.save(User.builder()
                .firstName("Katherine")
                .lastName("Johnson")
                .email(email)
                .password("hashed-password")
                .enabled(true)
                .accountLocked(false)
                .build());
    }

    @Test
    void findByToken_existingToken_returnsTokenWithAssociatedUser() {
        User user = persistUser("katherine@example.com");
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .token("reset-token-123")
                .type("PASSWORD_RESET")
                .expiryDate(Instant.now().plus(1, ChronoUnit.HOURS))
                .build());

        Optional<PasswordResetToken> found = passwordResetTokenRepository.findByToken("reset-token-123");

        assertThat(found).isPresent();
        assertThat(found.get().getUser().getEmail()).isEqualTo("katherine@example.com");
        assertThat(found.get().getType()).isEqualTo("PASSWORD_RESET");
    }

    @Test
    void findByToken_unknownToken_returnsEmpty() {
        assertThat(passwordResetTokenRepository.findByToken("does-not-exist")).isEmpty();
    }

    @Test
    void deleteAllByExpiryDateBefore_removesOnlyExpiredTokens() {
        User user = persistUser("dorothy@example.com");
        PasswordResetToken expired = passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .token("expired-reset-token")
                .type("PASSWORD_RESET")
                .expiryDate(Instant.now().minus(1, ChronoUnit.HOURS))
                .build());
        PasswordResetToken valid = passwordResetTokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .token("valid-reset-token")
                .type("PASSWORD_RESET")
                .expiryDate(Instant.now().plus(1, ChronoUnit.HOURS))
                .build());

        passwordResetTokenRepository.deleteAllByExpiryDateBefore(Instant.now());

        assertThat(passwordResetTokenRepository.findById(expired.getId())).isEmpty();
        assertThat(passwordResetTokenRepository.findById(valid.getId())).isPresent();
    }
}
