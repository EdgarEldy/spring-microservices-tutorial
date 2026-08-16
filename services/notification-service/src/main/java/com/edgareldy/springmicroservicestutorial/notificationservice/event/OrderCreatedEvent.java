package com.edgareldy.springmicroservicestutorial.notificationservice.event;

/**
 * Consumed by {@code OrderCreatedEventConsumer} off the {@code order-events} topic: published
 * by {@code order-service}'s {@code OrderEventProducer} strictly after that service's order-
 * creation transaction has committed. Triggers this service's half of the choreographed Saga
 * (see README's "Saga in detail: order confirmation").
 * <p>
 * This is a local, notification-service-owned copy of the wire shape, not a shared class with
 * {@code order-service}: services never share domain-specific event/DTO code (see
 * {@code .claude/CLAUDE.md}'s {@code common-lib} rule), each side of a Kafka contract keeps its
 * own record matching the same JSON payload. {@code order-service}'s producer uses a plain
 * {@code JsonSerializer} with no {@code spring.json.type.mapping}, so the {@code __TypeId__}
 * header it emits is that service's own fully-qualified class name; this service's consumer
 * config maps that exact name to this class (see {@code application.yml}/
 * {@code notification-service-*.yml}).
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
