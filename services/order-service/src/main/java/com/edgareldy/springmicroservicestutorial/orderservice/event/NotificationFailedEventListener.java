package com.edgareldy.springmicroservicestutorial.orderservice.event;

import com.edgareldy.springmicroservicestutorial.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes {@link NotificationFailedEvent} off the {@code notification-events} topic: the
 * choreographed Saga's compensating event, updating {@code Order.status} from {@code PENDING}
 * to {@code CONFIRMATION_FAILED}. No central orchestrator, no distributed transaction: this
 * service only ever reacts by updating its own local state, never by retrying or compensating
 * on {@code notification-service}'s behalf (see README's Saga walkthrough).
 * <p>
 * See {@link OrderConfirmedEventListener}'s own Javadoc for why this listener declares its own
 * distinct {@code groupId} and relies on {@code ErrorHandlingDeserializer} to skip the other
 * event type rather than crashing.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationFailedEventListener {

    private final OrderService orderService;

    @KafkaListener(topics = "notification-events", groupId = "order-service-notification-failed")
    public void onNotificationFailed(NotificationFailedEvent event) {
        log.info("Received NotificationFailedEvent for orderId={}", event.orderId());
        orderService.markConfirmationFailed(event.orderId());
    }
}
