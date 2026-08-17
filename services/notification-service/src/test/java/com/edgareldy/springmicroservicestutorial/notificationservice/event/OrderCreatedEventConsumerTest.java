package com.edgareldy.springmicroservicestutorial.notificationservice.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.edgareldy.springmicroservicestutorial.notificationservice.service.EmailNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pure Mockito unit tests for {@link OrderCreatedEventConsumer}: {@link
 * EmailNotificationService}/{@link OrderConfirmedEventProducer}/{@link
 * NotificationFailedEventProducer} are mocked, no Spring context, no real Kafka broker. Covers
 * this class' own branching logic (nominal path vs. the simulated-failure trigger); the real
 * {@code @KafkaListener} wiring (topic, groupId, deserializer/type-mapping config) is instead
 * exercised end-to-end by {@code NotificationServiceKafkaIntegrationTest}.
 * <p>
 * {@code simulatedFailureProductId} is normally injected via {@code @Value}, wired here
 * directly through {@link ReflectionTestUtils#setField} rather than a constructor argument
 * (it is not one of {@code @RequiredArgsConstructor}'s final fields).
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@ExtendWith(MockitoExtension.class)
class OrderCreatedEventConsumerTest {

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private OrderConfirmedEventProducer orderConfirmedEventProducer;

    @Mock
    private NotificationFailedEventProducer notificationFailedEventProducer;

    private OrderCreatedEventConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderCreatedEventConsumer(
                emailNotificationService, orderConfirmedEventProducer, notificationFailedEventProducer);
        ReflectionTestUtils.setField(consumer, "simulatedFailureProductId", 999L);
    }

    @Test
    void onOrderCreated_nominalProduct_sendsEmailAndPublishesOrderConfirmedEvent() {
        OrderCreatedEvent event = new OrderCreatedEvent(1L, 1L, 1L, 2, 79.80);

        consumer.onOrderCreated(event);

        verify(emailNotificationService).send(any(), any(), any());
        verify(orderConfirmedEventProducer).publish(eq(new OrderConfirmedEvent(1L)));
        verify(notificationFailedEventProducer, never()).publish(any());
    }

    @Test
    void onOrderCreated_simulatedFailureProductId_skipsEmailAndPublishesNotificationFailedEvent() {
        OrderCreatedEvent event = new OrderCreatedEvent(2L, 1L, 999L, 1, 39.90);

        consumer.onOrderCreated(event);

        verify(emailNotificationService, never()).send(any(), any(), any());
        verify(notificationFailedEventProducer).publish(eq(new NotificationFailedEvent(2L)));
        verify(orderConfirmedEventProducer, never()).publish(any());
    }
}
