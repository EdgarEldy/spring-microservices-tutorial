package com.edgareldy.springmicroservicestutorial.orderservice.event;

/**
 * Consumed by {@code OrderConfirmedEventListener} off the {@code notification-events} Kafka
 * topic: the choreographed Saga's nominal-path outcome, published by {@code
 * notification-service} (its own {@code OrderConfirmedEventProducer}, once the confirmation
 * "e-mail" is logged successfully) after handling this service's {@link OrderCreatedEvent}.
 * <p>
 * This is a local, order-service-owned copy of the wire shape, not a shared class with
 * {@code notification-service}: services never share domain-specific event/DTO code (see
 * {@code .claude/CLAUDE.md}'s {@code common-lib} rule), each side of a Kafka contract keeps
 * its own record matching the same JSON payload.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
public record OrderConfirmedEvent(
        Long orderId
) {
}
