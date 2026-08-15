package com.edgareldy.springmicroservicestutorial.catalogservice.service;

import com.edgareldy.springmicroservicestutorial.catalogservice.dto.ProductRequest;
import com.edgareldy.springmicroservicestutorial.catalogservice.dto.ProductResponse;
import com.edgareldy.springmicroservicestutorial.catalogservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contract for managing {@link Product} records exposed under
 * {@code /api/v1/catalog/products}, including the detail endpoint
 * {@code order-service} resolves via OpenFeign.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public interface ProductService {

    /**
     * Creates a new product. Throws {@code ResourceNotFoundException} if
     * {@code request.categoryId()} does not match an existing category.
     */
    Product create(ProductRequest request);

    /**
     * Returns a page of products, backing {@code GET /api/v1/catalog/products}.
     * When {@code categoryId} is non-null, results are restricted to that
     * category; otherwise every product is returned.
     */
    Page<Product> findAll(Pageable pageable, Long categoryId);

    /**
     * Looks up a product by id. Throws {@code ResourceNotFoundException} if
     * none matches. Backs {@code GET /api/v1/catalog/products/{id}}, the
     * endpoint {@code order-service} calls via Feign, so its response shape
     * must stay stable.
     */
    Product findById(Long id);

    /** Maps a {@link Product} entity to its public {@link ProductResponse} view. */
    ProductResponse toResponse(Product product);
}
