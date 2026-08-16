package com.edgareldy.springmicroservicestutorial.orderservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request body for {@code POST /api/v1/orders}. Deliberately carries no idempotency key:
 * that value comes from the {@code Idempotency-Key} request header instead, read directly by
 * {@code OrderController}, never duplicated into the JSON body.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
public record OrderRequest(
        @NotNull Long customerId,
        @NotNull Long productId,
        @Positive int quantity
) {
}
