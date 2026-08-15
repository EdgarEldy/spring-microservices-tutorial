package com.edgareldy.springmicroservicestutorial.catalogservice.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.edgareldy.springmicroservicestutorial.catalogservice.config.TestcontainersConfig;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Category;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

/**
 * Repository-level tests for {@link ProductRepository}, backed by a real
 * PostgreSQL container ({@link TestcontainersConfig}): the {@code
 * categoryId} filter, the {@code @EntityGraph}-driven eager fetch of {@link
 * Product#getCategory()} on {@code findById}, and the database-level {@code
 * CHECK (unit_price > 0)} constraint from {@code V1__init_schema.sql} all
 * depend on genuine PostgreSQL behaviour that an in-memory database would
 * not faithfully reproduce.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@DataJpaTest
@ImportAutoConfiguration(FlywayAutoConfiguration.class)
@Import(TestcontainersConfig.class)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private EntityManager entityManager;

    private Category persistCategory(String name) {
        return categoryRepository.saveAndFlush(Category.builder().categoryName(name).build());
    }

    private static Product newProduct(Category category, String name, double price) {
        return Product.builder().category(category).productName(name).unitPrice(price).build();
    }

    @Test
    void save_persistsProductWithMandatoryCategoryForeignKey() {
        Category category = persistCategory("Books");

        Product saved = productRepository.save(newProduct(category, "Clean Code", 39.90));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCategory().getId()).isEqualTo(category.getId());
        assertThat(saved.getUnitPrice()).isEqualTo(39.90);
    }

    @Test
    void findByCategoryId_paginated_excludesProductsFromOtherCategories() {
        Category books = persistCategory("Books");
        Category electronics = persistCategory("Electronics");
        productRepository.save(newProduct(books, "Clean Code", 39.90));
        productRepository.save(newProduct(books, "Effective Java", 45.00));
        productRepository.save(newProduct(electronics, "Headphones", 99.99));

        Page<Product> booksPage = productRepository.findByCategoryId(books.getId(), PageRequest.of(0, 10));

        assertThat(booksPage.getTotalElements()).isEqualTo(2);
        assertThat(booksPage.getContent())
                .extracting(Product::getProductName)
                .containsExactlyInAnyOrder("Clean Code", "Effective Java")
                .doesNotContain("Headphones");
    }

    @Test
    void findById_entityGraphLoadsCategoryEagerlyAfterFlushAndClear() {
        Category category = persistCategory("Books");
        Product product = productRepository.save(newProduct(category, "Clean Code", 39.90));

        // Flush + clear forces the next read through a fresh Hibernate
        // session/persistence context, proving the @EntityGraph fetch (not
        // an already-loaded first-level cache entry) supplies the category,
        // same pattern as UserRepositoryTest in auth-service.
        entityManager.flush();
        entityManager.clear();

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();

        // No LazyInitializationException expected here: category must already be
        // initialized by the @EntityGraph on findById, since no open session is
        // guaranteed once this method returns to a caller running outside the
        // original transaction.
        assertThat(reloaded.getCategory().getCategoryName()).isEqualTo("Books");
    }

    @Test
    void findById_absentId_returnsEmptyOptional() {
        Optional<Product> found = productRepository.findById(999_999L);

        assertThat(found).isEmpty();
    }

    @Test
    void persistAndFlush_zeroOrNegativeUnitPrice_violatesDatabaseCheckConstraint() {
        // Product carries no @Positive/@DecimalMin at the entity level (only
        // ProductRequest does, at the API boundary), so persisting via
        // EntityManager directly bypasses any Jakarta Bean Validation and
        // exercises the database CHECK (unit_price > 0) constraint itself
        // (V1__init_schema.sql), not ProductServiceImpl or ProductRequest.
        Category category = persistCategory("Books");
        Product zeroPriced = newProduct(category, "Free Sample", 0.0);

        assertThatExceptionOfType(PersistenceException.class)
                .isThrownBy(() -> {
                    entityManager.persist(zeroPriced);
                    entityManager.flush();
                });
    }

    @Test
    void persistAndFlush_negativeUnitPrice_violatesDatabaseCheckConstraint() {
        Category category = persistCategory("Books");
        Product negativePriced = newProduct(category, "Broken Price", -10.0);

        assertThatExceptionOfType(PersistenceException.class)
                .isThrownBy(() -> {
                    entityManager.persist(negativePriced);
                    entityManager.flush();
                });
    }
}
