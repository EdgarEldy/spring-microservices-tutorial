package com.edgareldy.springmicroservicestutorial.catalogservice.repository;

import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Product;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link Product}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * Backs the {@code categoryId} filter on {@code GET
     * /api/v1/catalog/products}.
     */
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    /**
     * Overrides {@link JpaRepository#findById(Object)} to eagerly fetch
     * {@link Product#getCategory()}. {@code ProductResponse} only carries
     * {@code categoryId} (see the dto package), and reading the id off a
     * lazy {@code Category} proxy would not by itself trigger a second
     * select, but this guards against a {@code LazyInitializationException}
     * if the mapping code (or a future change that adds the category's name
     * to the response) ends up running outside the transaction that loaded
     * the product.
     */
    @EntityGraph(attributePaths = "category")
    @Override
    Optional<Product> findById(Long id);
}
