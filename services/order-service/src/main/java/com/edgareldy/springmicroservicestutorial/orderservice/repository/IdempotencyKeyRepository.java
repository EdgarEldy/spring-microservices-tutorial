package com.edgareldy.springmicroservicestutorial.orderservice.repository;

import com.edgareldy.springmicroservicestutorial.orderservice.entity.IdempotencyKey;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link IdempotencyKey}, queried once per
 * {@code POST /api/v1/orders} before anything else happens, to detect a duplicate request.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, Long> {

    Optional<IdempotencyKey> findByIdempotencyKey(String idempotencyKey);
}
