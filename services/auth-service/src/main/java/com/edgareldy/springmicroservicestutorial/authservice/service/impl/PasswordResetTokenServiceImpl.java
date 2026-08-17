package com.edgareldy.springmicroservicestutorial.authservice.service.impl;

import com.edgareldy.springmicroservicestutorial.authservice.entity.PasswordResetToken;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;
import com.edgareldy.springmicroservicestutorial.authservice.exception.InvalidTokenException;
import com.edgareldy.springmicroservicestutorial.authservice.repository.PasswordResetTokenRepository;
import com.edgareldy.springmicroservicestutorial.authservice.security.SecureTokenGenerator;
import com.edgareldy.springmicroservicestutorial.authservice.service.PasswordResetTokenService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link PasswordResetTokenService} implementation, backed by
 * {@link PasswordResetTokenRepository}. {@code findByToken} there already
 * holds a pessimistic write lock on the matching row for the rest of the
 * calling transaction, so two concurrent reset attempts for the same token
 * cannot both read it before either has deleted/consumed it.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Service
@RequiredArgsConstructor
public class PasswordResetTokenServiceImpl implements PasswordResetTokenService {

    private static final String TOKEN_TYPE = "PASSWORD_RESET";
    private static final long EXPIRATION_HOURS = 1;

    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final SecureTokenGenerator secureTokenGenerator;

    @Override
    @Transactional
    public PasswordResetToken generate(User user) {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .user(user)
                .token(secureTokenGenerator.generate())
                .type(TOKEN_TYPE)
                .expiryDate(Instant.now().plus(EXPIRATION_HOURS, ChronoUnit.HOURS))
                .build();
        return passwordResetTokenRepository.save(resetToken);
    }

    /**
     * Runs in its own physical transaction ({@code REQUIRES_NEW}), not
     * joining the caller's ({@code AuthServiceImpl.resetPassword()}'s):
     * the delete below must commit on its own even when this method then
     * throws {@link InvalidTokenException}, since a {@code RuntimeException}
     * unwinding through a participating ({@code REQUIRED}) transaction would
     * mark the whole shared transaction rollback-only and silently undo the
     * delete, defeating "single-use even on a failed attempt". Isolating the
     * consume step in its own transaction also means a token, once
     * successfully consumed here, stays consumed even if the password
     * update that follows in the caller fails for an unrelated reason.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public User validateAndConsume(String rawToken) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid password reset token"));

        // Single-use even on a failed attempt: delete first, so a token that
        // turns out to be expired can never be retried against.
        passwordResetTokenRepository.delete(resetToken);

        if (resetToken.getExpiryDate().isBefore(Instant.now())) {
            throw new InvalidTokenException("Password reset token has expired");
        }

        return resetToken.getUser();
    }
}
