package com.edgareldy.springmicroservicestutorial.orderservice.service;

import com.edgareldy.springmicroservicestutorial.orderservice.dto.OrderDetailResponse;
import com.edgareldy.springmicroservicestutorial.orderservice.dto.OrderRequest;
import com.edgareldy.springmicroservicestutorial.orderservice.dto.OrderResponse;
import com.edgareldy.springmicroservicestutorial.orderservice.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contract for managing {@link Order}s exposed under {@code /api/v1/orders}. Every method
 * returns a DTO directly rather than the entity: nothing outside this service ever needs the
 * raw {@link Order} afterward, the same reasoning already applied to every other business
 * service in this project.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
public interface OrderService {

    /**
     * Creates a new order, idempotently: if {@code idempotencyKey} already matches an
     * existing {@code idempotency_keys} row, returns the order that key already points to
     * instead of creating a duplicate. Otherwise, validates {@code customerId}/
     * {@code productId} synchronously via Feign, computes {@code total}, persists the order
     * as {@code PENDING} and the idempotency key in the same local transaction, then
     * publishes {@code OrderCreatedEvent} only after that transaction commits.
     */
    OrderResponse create(OrderRequest request, String idempotencyKey);

    /** Returns a page of every order, backing {@code GET /api/v1/orders}. */
    Page<OrderResponse> findAll(Pageable pageable);

    /**
     * Looks up a single order by id, enriched with product/customer data resolved live via
     * Feign (the API Composition pattern). Throws {@code ResourceNotFoundException} if no
     * order matches, or if either downstream lookup itself comes back not-found.
     */
    OrderDetailResponse findById(Long id);

    /**
     * Transitions an order from {@code PENDING} to {@code CONFIRMED}: the only path that ever
     * reaches {@code CONFIRMED}, driven exclusively by {@code OrderConfirmedEventListener}
     * reacting to the choreographed Saga's nominal-path outcome event. Not exposed through
     * {@code OrderController}: no client ever calls this directly.
     */
    void markConfirmed(Long orderId);

    /**
     * Transitions an order from {@code PENDING} to {@code CONFIRMATION_FAILED}, driven
     * exclusively by {@code NotificationFailedEventListener} reacting to the choreographed
     * Saga's compensating event. Not exposed through {@code OrderController}.
     */
    void markConfirmationFailed(Long orderId);
}
