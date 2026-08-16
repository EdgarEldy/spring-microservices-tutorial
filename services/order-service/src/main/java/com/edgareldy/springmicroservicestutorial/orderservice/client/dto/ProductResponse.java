package com.edgareldy.springmicroservicestutorial.orderservice.client.dto;

/**
 * Local mirror of {@code catalog-service}'s own {@code ProductResponse}, deserialized from
 * {@code GET /api/v1/catalog/products/{id}} via {@link
 * com.edgareldy.springmicroservicestutorial.orderservice.client.ProductClient}. Never the
 * same Java class as {@code catalog-service}'s: services never share domain-specific DTOs
 * (see {@code .claude/CLAUDE.md}'s {@code common-lib} rule), each side of a Feign call keeps
 * its own local shape for the same wire contract.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
public record ProductResponse(
        Long id,
        String productName,
        double unitPrice,
        Long categoryId
) {
}
