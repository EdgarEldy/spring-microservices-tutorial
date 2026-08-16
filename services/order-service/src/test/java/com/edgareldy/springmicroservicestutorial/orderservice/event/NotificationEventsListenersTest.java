package com.edgareldy.springmicroservicestutorial.orderservice.event;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.edgareldy.springmicroservicestutorial.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Real {@code @EmbeddedKafka}-backed test for {@link OrderConfirmedEventListener}/
 * {@link NotificationFailedEventListener}: publishes a real {@link OrderConfirmedEvent}/
 * {@link NotificationFailedEvent} onto the {@code notification-events} topic and verifies
 * each listener actually receives it and reacts, exercising the real deserializer/type-mapping
 * wiring configured in {@code application.yml}/{@code order-service-*.yml}
 * ({@code ErrorHandlingDeserializer} + {@code spring.json.type.mapping}), not just each
 * listener method's own logic in isolation. Backs the README's "embedded/test Kafka broker
 * verifying... both OrderConfirmedEventListener/NotificationFailedEventListener correctly
 * transitioning status" test requirement.
 * <p>
 * {@link OrderService} is {@code @MockitoBean}-replaced: this test only needs to prove the
 * Kafka wiring correctly reaches each listener's {@code orderService.markConfirmed}/{@code
 * markConfirmationFailed} call, not the persistence behind those methods (already covered by
 * {@code OrderServiceImplTest}). Uses the same minimal, hand-picked
 * {@code @SpringBootTest(classes = ...)} context as {@link
 * com.edgareldy.springmicroservicestutorial.orderservice.client.ProductClientTest} (see that
 * class' Javadoc for why): {@code @EnableAutoConfiguration} plus explicitly {@code @Import}ing
 * just the two listener classes, with persistence-related autoconfiguration excluded since
 * this test never touches a database.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = NotificationEventsListenersTest.TestConfig.class)
@EmbeddedKafka(partitions = 1, topics = "notification-events")
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
        "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer",
        "spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer",
        "spring.kafka.consumer.properties.spring.json.trusted.packages=com.edgareldy.springmicroservicestutorial.orderservice.event",
        "spring.kafka.consumer.properties.spring.json.type.mapping=orderConfirmed:com.edgareldy.springmicroservicestutorial.orderservice.event.OrderConfirmedEvent,notificationFailed:com.edgareldy.springmicroservicestutorial.orderservice.event.NotificationFailedEvent",
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer",
        "eureka.client.enabled=false"
})
class NotificationEventsListenersTest {

    @EnableAutoConfiguration(exclude = {
            DataSourceAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class,
            DataJpaRepositoriesAutoConfiguration.class,
            FlywayAutoConfiguration.class
    })
    @Import({OrderConfirmedEventListener.class, NotificationFailedEventListener.class})
    static class TestConfig {
    }

    @MockitoBean
    private OrderService orderService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void orderConfirmedEvent_isConsumedAndTransitionsOrderToConfirmed() {
        kafkaTemplate.send("notification-events", "1", new OrderConfirmedEvent(1L));

        verify(orderService, timeout(10000)).markConfirmed(1L);
    }

    @Test
    void notificationFailedEvent_isConsumedAndTransitionsOrderToConfirmationFailed() {
        kafkaTemplate.send("notification-events", "2", new NotificationFailedEvent(2L));

        verify(orderService, timeout(10000)).markConfirmationFailed(2L);
    }
}
