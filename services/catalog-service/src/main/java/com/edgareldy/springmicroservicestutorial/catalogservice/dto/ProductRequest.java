package com.edgareldy.springmicroservicestutorial.catalogservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request body for {@code POST /api/v1/catalog/products}. {@code unitPrice}
 * is validated {@code > 0} here at the API boundary via {@link Positive},
 * ahead of and in addition to the database's own {@code CHECK} constraint
 * (V1__init_schema.sql).
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public record ProductRequest(
        @NotBlank String productName,
        @Positive double unitPrice,
        @NotNull Long categoryId
) {
}
