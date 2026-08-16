package com.edgareldy.springmicroservicestutorial.orderservice.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springmicroservicestutorial.orderservice.config.TestcontainersConfig;
import com.edgareldy.springmicroservicestutorial.orderservice.entity.IdempotencyKey;
import com.edgareldy.springmicroservicestutorial.orderservice.entity.Order;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Repository-level tests for {@link IdempotencyKeyRepository}, backed by a real PostgreSQL
 * container ({@link TestcontainersConfig}), exercising the {@code idempotency_keys} table's
 * {@code UNIQUE} constraint on {@code idempotency_key} and its real foreign key to
 * {@code orders}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@DataJpaTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(TestcontainersConfig.class)
class IdempotencyKeyRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    private Order persistOrder() {
        return orderRepository.save(Order.builder()
                .customerId(1L)
                .productId(1L)
                .quantity(1)
                .total(39.90)
                .build());
    }

    @Test
    void findByIdempotencyKey_found_returnsTheMatchingKey() {
        Order order = persistOrder();
        idempotencyKeyRepository.save(IdempotencyKey.builder()
                .idempotencyKey("key-1")
                .order(order)
                .createdAt(Instant.now())
                .build());

        Optional<IdempotencyKey> found = idempotencyKeyRepository.findByIdempotencyKey("key-1");

        assertThat(found).isPresent();
        assertThat(found.get().getOrder().getId()).isEqualTo(order.getId());
    }

    @Test
    void findByIdempotencyKey_notFound_returnsEmpty() {
        assertThat(idempotencyKeyRepository.findByIdempotencyKey("nonexistent")).isEmpty();
    }

    @Test
    void save_duplicateIdempotencyKey_violatesUniqueConstraint() {
        Order firstOrder = persistOrder();
        idempotencyKeyRepository.save(IdempotencyKey.builder()
                .idempotencyKey("key-1")
                .order(firstOrder)
                .createdAt(Instant.now())
                .build());
        idempotencyKeyRepository.flush();

        Order secondOrder = persistOrder();
        assertThatThrownBy(() -> {
                    idempotencyKeyRepository.save(IdempotencyKey.builder()
                            .idempotencyKey("key-1")
                            .order(secondOrder)
                            .createdAt(Instant.now())
                            .build());
                    idempotencyKeyRepository.flush();
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
