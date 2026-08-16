package com.edgareldy.springmicroservicestutorial.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity mapping the {@code idempotency_keys} table in {@code order_db}: backs the
 * Idempotent Consumer pattern on {@code POST /api/v1/orders}. The client supplies
 * {@link #idempotencyKey} via the {@code Idempotency-Key} header; before creating a new
 * {@link Order}, {@code OrderServiceImpl.create} checks whether that key already exists here
 * and, if so, returns the order it already points to instead of creating a duplicate. This
 * covers both Feign's own retry-on-timeout behavior and a client retrying after a dropped
 * connection.
 * <p>
 * {@link #order} is a real {@code @ManyToOne}, unlike {@link Order#customerId}/{@link
 * Order#productId}: {@code orders} and {@code idempotency_keys} live in the same
 * {@code order_db}, so this relationship never crosses a service boundary.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Entity
@Table(name = "idempotency_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class IdempotencyKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
