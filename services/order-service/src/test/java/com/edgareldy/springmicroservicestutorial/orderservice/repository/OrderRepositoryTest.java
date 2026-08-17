package com.edgareldy.springmicroservicestutorial.orderservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmicroservicestutorial.orderservice.config.TestcontainersConfig;
import com.edgareldy.springmicroservicestutorial.orderservice.entity.Order;
import com.edgareldy.springmicroservicestutorial.orderservice.entity.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Repository-level tests for {@link OrderRepository}, backed by a real PostgreSQL container
 * ({@link TestcontainersConfig}) rather than an in-memory database, so the {@code orders}
 * table created by {@code V1__init_schema.sql} (including the {@code quantity > 0} CHECK
 * constraint and the {@code status} default) is exercised against actual PostgreSQL behaviour.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@DataJpaTest
// Spring Boot 4.x's @DataJpaTest slice no longer imports FlywayAutoConfiguration by
// default: without this, V1__init_schema.sql never runs against the Testcontainers
// database and every query below fails with "relation does not exist".
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(TestcontainersConfig.class)
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    private static Order newOrder(long customerId, long productId) {
        return Order.builder()
                .customerId(customerId)
                .productId(productId)
                .quantity(2)
                .total(79.80)
                .build();
    }

    @Test
    void save_persistsOrderWithGeneratedIdAndDefaultPendingStatus() {
        Order saved = orderRepository.save(newOrder(1L, 1L));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getTotal()).isEqualTo(79.80);
    }

    @Test
    void findAll_paginated_returnsOnlyRequestedPageAndCorrectTotal() {
        orderRepository.save(newOrder(1L, 1L));
        orderRepository.save(newOrder(1L, 2L));
        orderRepository.save(newOrder(2L, 1L));

        Page<Order> firstPage = orderRepository.findAll(PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }

    @Test
    void save_statusExplicitlySet_persistsThatStatus() {
        Order order = newOrder(1L, 1L);
        order.setStatus(OrderStatus.CONFIRMED);

        Order saved = orderRepository.save(order);

        assertThat(orderRepository.findById(saved.getId()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.CONFIRMED);
    }
}
