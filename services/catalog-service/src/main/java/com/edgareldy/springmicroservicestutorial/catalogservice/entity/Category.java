package com.edgareldy.springmicroservicestutorial.catalogservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * JPA entity mapping the {@code categories} table in {@code catalog_db}.
 * <p>
 * Deliberately has no {@code @OneToMany} back-reference to {@link Product}:
 * nothing in this branch needs to navigate from a category to its products
 * in memory (listing is done via {@code ProductRepository.findByCategoryId},
 * a direct query), and a bidirectional collection here would only add
 * cascade/lazy-loading complexity (e.g. accidentally loading every product
 * when fetching a category) without a corresponding benefit.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_name", nullable = false)
    private String categoryName;
}
