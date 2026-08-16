package com.edgareldy.springmicroservicestutorial.orderservice.dto;

import com.edgareldy.springmicroservicestutorial.orderservice.entity.OrderStatus;

/**
 * Read-only view of a single {@code Order}, returned only from
 * {@code GET /api/v1/orders/{id}}: the API Composition pattern, {@code productName} and
 * {@code customerFullName} are resolved live via {@code ProductClient}/{@code CustomerClient}
 * (OpenFeign) at request time, never stored on {@link
 * com.edgareldy.springmicroservicestutorial.orderservice.entity.Order} itself. If either
 * downstream lookup fails, {@code OrderServiceImpl.findById} lets the resulting
 * {@code BusinessRuleException}/404 propagate rather than returning a partially-enriched
 * response.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
public record OrderDetailResponse(
        Long id,
        Long customerId,
        Long productId,
        int quantity,
        double total,
        OrderStatus status,
        String productName,
        String customerFullName
) {
}
