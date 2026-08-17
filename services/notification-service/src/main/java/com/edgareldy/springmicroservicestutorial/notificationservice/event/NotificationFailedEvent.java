package com.edgareldy.springmicroservicestutorial.notificationservice.event;

/**
 * Published on the {@code notification-events} topic by {@code NotificationFailedEventProducer}
 * when {@code OrderCreatedEventConsumer}'s simulated e-mail send fails: the choreographed
 * Saga's compensating event, symmetric with {@link OrderConfirmedEvent}. Consumed back by
 * {@code order-service}'s own {@code NotificationFailedEventListener}, which transitions
 * {@code Order.status} from {@code PENDING} to {@code CONFIRMATION_FAILED}. No central
 * orchestrator, no distributed transaction: this event only ever tells {@code order-service}
 * to update its own local state, never a retry/compensation this service performs itself.
 * <p>
 * Named at the past tense. Carries only {@code orderId}, same rationale as
 * {@link OrderConfirmedEvent}.
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
