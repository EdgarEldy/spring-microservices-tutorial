package com.edgareldy.springmicroservicestutorial.orderservice.event;

/**
 * Consumed by {@code NotificationFailedEventListener} off the {@code notification-events}
 * Kafka topic: the choreographed Saga's compensating event, published by {@code
 * notification-service} (its own {@code NotificationFailedEventProducer}) when the simulated
 * confirmation "e-mail" fails, after handling this service's {@link OrderCreatedEvent}. No
 * central orchestrator, no distributed transaction: this service only ever reacts by updating
 * its own local {@code Order.status}, never by retrying or compensating on
 * {@code notification-service}'s behalf.
 * <p>
 * This is a local, order-service-owned copy of the wire shape, not a shared class with
 * {@code notification-service}: same rationale as {@link OrderConfirmedEvent}'s own Javadoc.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
public record NotificationFailedEvent(
        Long orderId
) {
}
