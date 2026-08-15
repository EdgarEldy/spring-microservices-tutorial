package com.edgareldy.springmicroservicestutorial.authservice.service.impl;

import com.edgareldy.springmicroservicestutorial.authservice.entity.ActivationToken;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;
import com.edgareldy.springmicroservicestutorial.authservice.exception.InvalidTokenException;
import com.edgareldy.springmicroservicestutorial.authservice.repository.ActivationTokenRepository;
import com.edgareldy.springmicroservicestutorial.authservice.security.SecureTokenGenerator;
import com.edgareldy.springmicroservicestutorial.authservice.service.ActivationTokenService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link ActivationTokenService} implementation, backed by
 * {@link ActivationTokenRepository}. {@code findByToken} there already holds
 * a pessimistic write lock on the matching row for the rest of the calling
 * transaction, so two concurrent activation attempts for the same token
 * cannot both observe {@code validatedAt == null} before either commits.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Service
@RequiredArgsConstructor
public class ActivationTokenServiceImpl implements ActivationTokenService {

    private static final long EXPIRATION_HOURS = 24;

    private final ActivationTokenRepository activationTokenRepository;
    private final SecureTokenGenerator secureTokenGenerator;

    @Override
    @Transactional
    public ActivationToken generate(User user) {
        Instant now = Instant.now();
        ActivationToken activationToken = ActivationToken.builder()
                .user(user)
                .token(secureTokenGenerator.generate())
                .createdAt(now)
                .expiresAt(now.plus(EXPIRATION_HOURS, ChronoUnit.HOURS))
                .build();
        return activationTokenRepository.save(activationToken);
    }

    @Override
    @Transactional
    public User validate(String rawToken) {
        ActivationToken activationToken = activationTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> new InvalidTokenException("Invalid activation token"));

        if (activationToken.getValidatedAt() != null) {
            throw new InvalidTokenException("Activation token already used");
        }
        if (activationToken.getExpiresAt() != null && activationToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Activation token has expired");
        }

        activationToken.setValidatedAt(Instant.now());
        activationTokenRepository.save(activationToken);
        return activationToken.getUser();
    }
}
