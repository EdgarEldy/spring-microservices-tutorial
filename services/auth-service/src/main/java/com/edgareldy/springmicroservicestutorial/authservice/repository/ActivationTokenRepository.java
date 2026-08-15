package com.edgareldy.springmicroservicestutorial.authservice.repository;

import com.edgareldy.springmicroservicestutorial.authservice.entity.ActivationToken;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/**
 * Spring Data JPA repository for {@link ActivationToken}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public interface ActivationTokenRepository extends JpaRepository<ActivationToken, Long> {

    /**
     * Locks the matching row for the rest of the caller's transaction, so
     * two concurrent activation attempts for the same token cannot both
     * read {@code validatedAt == null} before either has set it.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ActivationToken> findByToken(String token);

    void deleteAllByExpiresAtBefore(Instant instant);
}
