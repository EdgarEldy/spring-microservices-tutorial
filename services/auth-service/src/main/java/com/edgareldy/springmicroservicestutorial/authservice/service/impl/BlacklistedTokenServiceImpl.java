package com.edgareldy.springmicroservicestutorial.authservice.service.impl;

import com.edgareldy.springmicroservicestutorial.authservice.entity.BlacklistedToken;
import com.edgareldy.springmicroservicestutorial.authservice.entity.User;
import com.edgareldy.springmicroservicestutorial.authservice.repository.BlacklistedTokenRepository;
import com.edgareldy.springmicroservicestutorial.authservice.service.BlacklistedTokenService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Default {@link BlacklistedTokenService} implementation, backed by
 * {@link BlacklistedTokenRepository}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Service
@RequiredArgsConstructor
public class BlacklistedTokenServiceImpl implements BlacklistedTokenService {

    private final BlacklistedTokenRepository blacklistedTokenRepository;

    @Override
    @Transactional
    public void blacklist(User user, String rawToken, String jti, Instant expiresAt) {
        Instant now = Instant.now();
        BlacklistedToken blacklistedToken = BlacklistedToken.builder()
                .user(user)
                .token(rawToken)
                .jti(jti)
                .blacklistedAt(now)
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();
        blacklistedTokenRepository.save(blacklistedToken);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlacklisted(String jti) {
        return blacklistedTokenRepository.existsByJti(jti);
    }
}
