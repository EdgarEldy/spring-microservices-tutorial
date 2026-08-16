package com.edgareldy.springmicroservicestutorial.notificationservice.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.edgareldy.springmicroservicestutorial.notificationservice.service.EmailNotificationService;
import java.time.Duration;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Real {@code @EmbeddedKafka}-backed test exercising the full choreographed Saga's
 * notification-service half, end to end: publishes a real {@link OrderCreatedEvent}/{@link
 * UserRegisteredEvent}/{@link PasswordResetRequestedEvent} the way {@code order-service}/
 * {@code auth-service}'s own producers actually would, and verifies the correct consumer
 * reacts, exercising the real {@code @KafkaListener}/{@code ErrorHandlingDeserializer}/
 * {@code spring.json.type.mapping} wiring (configured in {@code application.yml}), not just
 * each consumer's own logic in isolation (already covered by the per-consumer Mockito unit
 * tests). Backs the README's "embedded Kafka test verifying the full order-flow choreography
 * in both directions... plus a simpler test confirming UserRegisteredEvent/
 * PasswordResetRequestedEvent trigger the expected EmailNotificationService call" requirement.
 * <p>
 * {@link EmailNotificationService} is {@code @MockitoBean}-replaced: this test only needs to
 * prove the Kafka wiring correctly reaches each consumer's call into that service, not the
 * (trivial, log-only) implementation behind it.
 * <p>
 * <strong>Simulating another service's producer</strong>: {@code order-service}/{@code
 * auth-service}'s real producers use a plain {@code JsonSerializer} with no type mapping, so
 * the {@code __TypeId__} header they emit is their own fully-qualified class name (see {@link
 * OrderCreatedEvent}'s own Javadoc). This test's {@code spring.kafka.producer.properties.
 * spring.json.type.mapping} (see the class-level {@code @TestPropertySource}) is a test-only
 * superset of the real dev/prod producer config: it maps {@code auth-service}/{@code
 * order-service}'s own class names to this module's local event classes (so sending a local
 * {@code new UserRegisteredEvent(...)} through the shared {@link KafkaTemplate} emits the
 * exact header {@code auth-service}'s real producer would), on top of this service's own
 * {@code orderConfirmed}/{@code notificationFailed} short-key mapping the real producers
 * still need. This combined mapping only exists here, for the test's convenience of driving
 * every "producer" through one shared bean; it is never part of the real dev/prod
 * configuration, which each direction only ever needs half of.
 * <p>
 * A raw {@link Consumer} (plain {@link StringDeserializer} for the value, no type mapping) is
 * used to inspect {@code notification-events} directly, reading the {@code __TypeId__} header
 * to distinguish an {@link OrderConfirmedEvent} from a {@link NotificationFailedEvent} without
 * needing this test's own consumer-side type mapping to also cover this module's own output
 * events.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"order-events", "auth-events", "notification-events"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "notification.simulated-failure-product-id=999",
        "eureka.client.enabled=false",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer",
        "spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer",
        "spring.kafka.consumer.properties.spring.json.trusted.packages=com.edgareldy.springmicroservicestutorial.notificationservice.event",
        "spring.kafka.consumer.properties.spring.json.type.mapping=com.edgareldy.springmicroservicestutorial.orderservice.event.OrderCreatedEvent:com.edgareldy.springmicroservicestutorial.notificationservice.event.OrderCreatedEvent,com.edgareldy.springmicroservicestutorial.authservice.event.UserRegisteredEvent:com.edgareldy.springmicroservicestutorial.notificationservice.event.UserRegisteredEvent,com.edgareldy.springmicroservicestutorial.authservice.event.PasswordResetRequestedEvent:com.edgareldy.springmicroservicestutorial.notificationservice.event.PasswordResetRequestedEvent",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "spring.kafka.producer.properties.spring.json.type.mapping=com.edgareldy.springmicroservicestutorial.orderservice.event.OrderCreatedEvent:com.edgareldy.springmicroservicestutorial.notificationservice.event.OrderCreatedEvent,com.edgareldy.springmicroservicestutorial.authservice.event.UserRegisteredEvent:com.edgareldy.springmicroservicestutorial.notificationservice.event.UserRegisteredEvent,com.edgareldy.springmicroservicestutorial.authservice.event.PasswordResetRequestedEvent:com.edgareldy.springmicroservicestutorial.notificationservice.event.PasswordResetRequestedEvent,orderConfirmed:com.edgareldy.springmicroservicestutorial.notificationservice.event.OrderConfirmedEvent,notificationFailed:com.edgareldy.springmicroservicestutorial.notificationservice.event.NotificationFailedEvent"
})
class NotificationServiceKafkaIntegrationTest {

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockitoBean
    private EmailNotificationService emailNotificationService;

    private Consumer<String, String> rawNotificationEventsConsumer;

    @BeforeEach
    void setUpRawConsumer() {
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("raw-test-consumer", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        rawNotificationEventsConsumer =
                new DefaultKafkaConsumerFactory<>(consumerProps, new StringDeserializer(), new StringDeserializer())
                        .createConsumer();
        rawNotificationEventsConsumer.subscribe(java.util.List.of("notification-events"));
    }

    @AfterEach
    void tearDownRawConsumer() {
        rawNotificationEventsConsumer.close();
    }

    @Test
    void orderCreatedEvent_nominal_sendsEmailAndPublishesOrderConfirmedEvent() {
        kafkaTemplate.send("order-events", "1", new OrderCreatedEvent(1L, 1L, 1L, 2, 79.80));

        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(
                rawNotificationEventsConsumer, "notification-events", Duration.ofSeconds(10));

        assertThat(typeId(record)).isEqualTo("orderConfirmed");
        assertThat(record.value()).contains("\"orderId\":1");
        verify(emailNotificationService, timeout(5000)).send(any(), any(), any());
    }

    @Test
    void orderCreatedEvent_simulatedFailureProductId_skipsEmailAndPublishesNotificationFailedEvent() {
        kafkaTemplate.send("order-events", "2", new OrderCreatedEvent(2L, 1L, 999L, 1, 39.90));

        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(
                rawNotificationEventsConsumer, "notification-events", Duration.ofSeconds(10));

        assertThat(typeId(record)).isEqualTo("notificationFailed");
        assertThat(record.value()).contains("\"orderId\":2");
        // Safe to assert "never" here: getSingleRecord already blocked until the listener's
        // synchronous method body (email-or-not, then publish) had fully returned.
        verify(emailNotificationService, never()).send(any(), any(), any());
    }

    @Test
    void userRegisteredEvent_triggersActivationEmail() {
        kafkaTemplate.send("auth-events", "1", new UserRegisteredEvent(1L, "ada@example.com", "Ada", "activation-token"));

        verify(emailNotificationService, timeout(10000)).send(org.mockito.ArgumentMatchers.eq("ada@example.com"), any(), any());
    }

    @Test
    void passwordResetRequestedEvent_triggersResetEmail() {
        kafkaTemplate.send("auth-events", "1", new PasswordResetRequestedEvent(1L, "ada@example.com", "reset-token"));

        verify(emailNotificationService, timeout(10000)).send(org.mockito.ArgumentMatchers.eq("ada@example.com"), any(), any());
    }

    private static String typeId(ConsumerRecord<String, String> record) {
        Header header = record.headers().lastHeader("__TypeId__");
        return header == null ? null : new String(header.value());
    }
}
