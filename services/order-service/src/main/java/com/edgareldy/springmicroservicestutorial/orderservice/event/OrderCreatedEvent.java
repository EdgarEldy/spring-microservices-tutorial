package com.edgareldy.springmicroservicestutorial.orderservice.event;

/**
 * Published on the {@code order-events} Kafka topic by {@code OrderServiceImpl.create},
 * strictly after the local transaction that persisted the {@code Order}/{@code
 * IdempotencyKey} has committed (never before: a consumer reacting to an order that turns
 * out not to exist would be an inconsistency this rule exists specifically to prevent).
 * {@code notification-service}'s {@code OrderCreatedEventConsumer} is the sole consumer,
 * triggering the choreographed Saga (see README's "Saga in detail: order confirmation").
 * <p>
 * Named at the past tense, describing a fact that already happened, per this project's
 * event-naming convention. Carries only the ids/data a consumer actually needs to log a
 * simulated confirmation e-mail, never the full {@code Order} entity.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
public record OrderCreatedEvent(
        Long orderId,
        Long customerId,
        Long productId,
        int quantity,
        double total
) {
}
