package com.edgareldy.springmicroservicestutorial.orderservice.event;

import com.edgareldy.springmicroservicestutorial.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link OrderConfirmedEvent} off the {@code notification-events} topic: the
 * choreographed Saga's nominal-path outcome, updating {@code Order.status} from
 * {@code PENDING} to {@code CONFIRMED}. This is the only path that ever reaches
 * {@code CONFIRMED}; an order left {@code PENDING} simply means this listener has not fired
 * yet (see README's Saga walkthrough).
 * <p>
 * Declares its own {@code groupId}, distinct from {@link NotificationFailedEventListener}'s:
 * {@code notification-events} carries both event types, and each listener needs its own full
 * copy of the topic (a shared consumer group would split partitions between the two listener
 * classes, so only one of them would ever see any given record). {@code spring.json.type.mapping}
 * (configured in {@code application.yml}) means a {@code NotificationFailedEvent}-typed record
 * arriving on this listener's group still deserializes successfully (the type-id header maps to
 * a real class), the failure only happens one step later, when Spring Kafka tries to bind that
 * {@code NotificationFailedEvent} instance to this method's {@code OrderConfirmedEvent}
 * parameter; {@code DefaultErrorHandler}'s default backoff retries that binding failure a
 * handful of times before giving up and moving past the record, never crashing the listener
 * container, but also never truly "skipping immediately" - this cross-delivery case is a known,
 * accepted rough edge for this branch (see README's Saga walkthrough, which never actually
 * routes a {@code NotificationFailedEvent} to this listener in practice), not something this
 * branch adds dedicated non-retryable-exception handling for.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConfirmedEventListener {

    private final OrderService orderService;

    @KafkaListener(topics = "notification-events", groupId = "order-service-order-confirmed")
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        log.info("Received OrderConfirmedEvent for orderId={}", event.orderId());
        orderService.markConfirmed(event.orderId());
    }
}
