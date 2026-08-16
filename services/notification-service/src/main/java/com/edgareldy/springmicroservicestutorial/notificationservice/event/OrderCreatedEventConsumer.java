package com.edgareldy.springmicroservicestutorial.notificationservice.event;

import com.edgareldy.springmicroservicestutorial.notificationservice.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link OrderCreatedEvent} off the {@code order-events} topic and triggers the order
 * confirmation "e-mail" via {@link EmailNotificationService}, then publishes exactly one of the
 * choreographed Saga's two outcome events back onto {@code notification-events}: {@link
 * OrderConfirmedEvent} on success, {@link NotificationFailedEvent} on failure. Never both,
 * never neither, so {@code order-service} always eventually hears back one way or the other
 * (see README's Saga walkthrough).
 * <p>
 * {@code notification.simulated-failure-product-id} deliberately triggers the failure path for
 * a specific {@code productId}, standing in for the README's "simulated failure (configurable,
 * e.g. a specific product name)" - {@link OrderCreatedEvent} carries {@code productId}, not a
 * product name, so this branch simulates the failure by id instead, functionally equivalent
 * for demo purposes. Defaults to {@code -1}, an id no real product ever has, so the failure
 * path stays dormant unless explicitly configured for a demo/test.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreatedEventConsumer {

    private final EmailNotificationService emailNotificationService;
    private final OrderConfirmedEventProducer orderConfirmedEventProducer;
    private final NotificationFailedEventProducer notificationFailedEventProducer;

    @Value("${notification.simulated-failure-product-id:-1}")
    private Long simulatedFailureProductId;

    @KafkaListener(topics = "order-events", groupId = "notification-service-order-created")
    public void onOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for orderId={}", event.orderId());

        if (event.productId().equals(simulatedFailureProductId)) {
            log.warn("Simulated notification failure for orderId={} (productId={})", event.orderId(), event.productId());
            notificationFailedEventProducer.publish(new NotificationFailedEvent(event.orderId()));
            return;
        }

        // A placeholder recipient address: this service is database-less and calls no other
        // service to resolve customerId into a real e-mail (that would reintroduce the
        // synchronous coupling this asynchronous flow exists to avoid). Harmless here since
        // "sending" only ever means logging, never a real delivery.
        emailNotificationService.send(
                "customer-" + event.customerId() + "@example.com",
                "Order confirmation",
                "Your order #" + event.orderId() + " (total: " + event.total() + ") has been confirmed.");
        orderConfirmedEventProducer.publish(new OrderConfirmedEvent(event.orderId()));
    }
}
