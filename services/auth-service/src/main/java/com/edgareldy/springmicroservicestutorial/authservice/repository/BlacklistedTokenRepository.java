package com.edgareldy.springmicroservicestutorial.authservice.repository;

import com.edgareldy.springmicroservicestutorial.authservice.entity.BlacklistedToken;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link BlacklistedToken}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public interface BlacklistedTokenRepository extends JpaRepository<BlacklistedToken, Long> {

    boolean existsByJti(String jti);

    void deleteAllByExpiresAtBefore(Instant instant);
}
