package com.edgareldy.springmicroservicestutorial.catalogservice.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.edgareldy.springmicroservicestutorial.catalogservice.config.TestcontainersConfig;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Repository-level tests for {@link CategoryRepository}, backed by a real
 * PostgreSQL container ({@link TestcontainersConfig}) rather than an
 * in-memory database, so the {@code categories} table created by
 * {@code V1__init_schema.sql} is exercised against actual PostgreSQL
 * behaviour.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@DataJpaTest
// Spring Boot 4.x's @DataJpaTest slice no longer imports FlywayAutoConfiguration by
// default: without this, V1__init_schema.sql never runs against the Testcontainers
// database and every query below fails with "relation does not exist".
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(TestcontainersConfig.class)
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private static Category newCategory(String name) {
        return Category.builder().categoryName(name).build();
    }

    @Test
    void save_persistsCategoryWithGeneratedId() {
        Category saved = categoryRepository.save(newCategory("Books"));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCategoryName()).isEqualTo("Books");
    }

    @Test
    void findAll_paginated_returnsOnlyRequestedPageAndCorrectTotal() {
        categoryRepository.save(newCategory("Books"));
        categoryRepository.save(newCategory("Electronics"));
        categoryRepository.save(newCategory("Garden"));

        Page<Category> firstPage = categoryRepository.findAll(PageRequest.of(0, 2));

        assertThat(firstPage.getContent()).hasSize(2);
        assertThat(firstPage.getTotalElements()).isEqualTo(3);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
    }
}
