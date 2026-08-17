package com.edgareldy.springmicroservicestutorial.notificationservice.event;

/**
 * Published on the {@code notification-events} topic by {@code OrderConfirmedEventProducer}
 * once {@code OrderCreatedEventConsumer} has successfully "sent" the order confirmation
 * e-mail: the choreographed Saga's nominal-path outcome, symmetric with
 * {@link NotificationFailedEvent}. Consumed back by {@code order-service}'s own
 * {@code OrderConfirmedEventListener} (a separate, local copy of this same wire shape there,
 * see this service's own {@link OrderCreatedEvent} Javadoc for why classes are never shared
 * across services), which transitions {@code Order.status} from {@code PENDING} to
 * {@code CONFIRMED}.
 * <p>
 * Named at the past tense, describing a fact that already happened. Carries only
 * {@code orderId}: {@code order-service} already has every other field it needs locally, this
 * event just tells it which order to transition.
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
