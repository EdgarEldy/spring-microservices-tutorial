package com.edgareldy.springmicroservicestutorial.notificationservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@link NotificationFailedEvent} onto the {@code notification-events} topic: the
 * choreographed Saga's compensating event, called by {@code OrderCreatedEventConsumer} when
 * the order confirmation "e-mail" fails (a simulated, configurable failure for this tutorial).
 * See {@link OrderConfirmedEventProducer}'s own Javadoc for the {@code spring.json.type.mapping}
 * rationale, which applies here identically (short key {@code "notificationFailed"}).
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationFailedEventProducer {

    private static final String NOTIFICATION_EVENTS_TOPIC = "notification-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publish(NotificationFailedEvent event) {
        log.info("Publishing NotificationFailedEvent for orderId={}", event.orderId());
        kafkaTemplate.send(NOTIFICATION_EVENTS_TOPIC, String.valueOf(event.orderId()), event);
    }
}
