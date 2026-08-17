package com.edgareldy.springmicroservicestutorial.catalogservice.dto;

/**
 * Read-only view of a {@code Product}, returned from {@code GET
 * /api/v1/catalog/products}, {@code GET /api/v1/catalog/products/{id}}
 * (the endpoint {@code order-service} resolves via OpenFeign), and {@code
 * POST /api/v1/catalog/products}'s response.
 * <p>
 * Carries {@code categoryId} rather than the category's name or a nested
 * {@code CategoryResponse}: {@code order-service}, the endpoint's main
 * consumer, only ever needs the id to resolve total price/validate the
 * product, and keeping this DTO flat avoids coupling it to how much of
 * {@code Category} a future caller might want.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public record ProductResponse(
        Long id,
        String productName,
        double unitPrice,
        Long categoryId
) {
}
