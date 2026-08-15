package com.edgareldy.springmicroservicestutorial.catalogservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA entity mapping the {@code products} table in {@code catalog_db},
 * including the {@code /api/v1/catalog/products/{id}} detail endpoint
 * {@code order-service} resolves via OpenFeign.
 * <p>
 * {@link #category} is a real {@code @ManyToOne} foreign key, not a plain id
 * column: {@code categories} and {@code products} live in the same database
 * (see README's "catalog_db relationship"), unlike {@code order_service}'s
 * {@code customerId}/{@code productId}, which cross a service boundary and
 * are deliberately plain values. {@code unitPrice} is a primitive
 * {@code double} (never boxed), matching the README's {@code FLOAT NOT NULL}
 * column; the {@code > 0} rule is enforced both by a database
 * {@code CHECK} constraint (V1__init_schema.sql) and, earlier, by
 * {@code @Positive} on {@code ProductRequest} so a bad value is rejected at
 * the API boundary rather than only surfacing as a database error.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "unit_price", nullable = false)
    private double unitPrice;
}
