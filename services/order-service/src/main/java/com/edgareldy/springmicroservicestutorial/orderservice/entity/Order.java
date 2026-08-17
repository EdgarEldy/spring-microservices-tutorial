package com.edgareldy.springmicroservicestutorial.orderservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity mapping the {@code orders} table in {@code order_db}.
 * <p>
 * {@link #customerId}/{@link #productId} are deliberately plain {@code Long} columns
 * ({@code @Column}), never a JPA relationship and never backed by a foreign key in
 * {@code V1__init_schema.sql}: {@code order_db}, {@code customer_db}, and {@code catalog_db}
 * are three separate databases owned by three separate services, so a cross-service foreign
 * key (or a JPA association that implies one) would mean this service could no longer be
 * deployed, migrated, or scaled independently of {@code customer-service}/{@code
 * catalog-service}. Both values are validated synchronously via {@code CustomerClient}/
 * {@code ProductClient} (OpenFeign) before an {@code Order} is ever persisted, never via a
 * database join.
 * <p>
 * {@link #status} defaults to {@link OrderStatus#PENDING} at creation and only ever
 * transitions forward, never back, driven by the choreographed Saga (see {@link
 * OrderStatus}'s own Javadoc).
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Plain value referencing customer-service's customers.id. Never a FK, never a
    // JPA association: see the class Javadoc above.
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    // Plain value referencing catalog-service's products.id. Same rationale as
    // customerId above.
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "total", nullable = false)
    private double total;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;
}
