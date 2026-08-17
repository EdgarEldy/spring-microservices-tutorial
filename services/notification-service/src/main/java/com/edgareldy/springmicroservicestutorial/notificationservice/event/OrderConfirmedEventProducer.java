package com.edgareldy.springmicroservicestutorial.notificationservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link OrderConfirmedEvent} onto the {@code notification-events} topic: the
 * choreographed Saga's nominal-path outcome, called by {@code OrderCreatedEventConsumer} once
 * the order confirmation "e-mail" has been sent successfully.
 * <p>
 * Unlike {@code order-service}'s own {@code OrderEventProducer} (plain {@code JsonSerializer},
 * no type mapping needed since {@code order-service} is the only consumer of its own event),
 * this producer's {@code spring.json.type.mapping} (configured in {@code application.yml}/
 * {@code notification-service-*.yml}) maps {@link OrderConfirmedEvent}/{@link
 * NotificationFailedEvent} to the short keys {@code order-service}'s own
 * {@code OrderConfirmedEventListener}/{@code NotificationFailedEventListener} were already
 * built to expect (see that service's own config comments): the {@code __TypeId__} header
 * this producer emits is {@code "orderConfirmed"}, not this class' fully-qualified name.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderConfirmedEventProducer {

    private static final String NOTIFICATION_EVENTS_TOPIC = "notification-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(OrderConfirmedEvent event) {
        log.info("Publishing OrderConfirmedEvent for orderId={}", event.orderId());
        kafkaTemplate.send(NOTIFICATION_EVENTS_TOPIC, String.valueOf(event.orderId()), event);
    }
}
