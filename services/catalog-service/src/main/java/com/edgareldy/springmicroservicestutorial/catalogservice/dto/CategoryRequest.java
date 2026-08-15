package com.edgareldy.springmicroservicestutorial.catalogservice.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request body for {@code POST /api/v1/catalog/categories}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public record CategoryRequest(
        @NotBlank String categoryName
) {
}
