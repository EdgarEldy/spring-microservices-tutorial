package com.edgareldy.springmicroservicestutorial.orderservice.dto;

import com.edgareldy.springmicroservicestutorial.orderservice.entity.OrderStatus;

/**
 * Read-only view of an {@code Order}, returned from {@code POST /api/v1/orders} and
 * {@code GET /api/v1/orders}. Carries {@code customerId}/{@code productId} as plain ids,
 * never resolved product/customer data: that enrichment only happens on the single-order
 * detail endpoint, {@link OrderDetailResponse}, via {@code GET /api/v1/orders/{id}}, to avoid
 * an extra Feign round trip per row on the paginated listing.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
public record OrderResponse(
        Long id,
        Long customerId,
        Long productId,
        int quantity,
        double total,
        OrderStatus status
) {
}
