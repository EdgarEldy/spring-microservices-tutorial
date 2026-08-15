package com.edgareldy.springmicroservicestutorial.catalogservice.dto;

/**
 * Read-only view of a {@code Category}, returned from {@code GET
 * /api/v1/catalog/categories} and as part of {@code POST
 * /api/v1/catalog/categories}'s response.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public record CategoryResponse(
        Long id,
        String categoryName
) {
}
