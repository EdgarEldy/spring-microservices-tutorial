package com.edgareldy.springmicroservicestutorial.orderservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@code order-service}'s outbound event, {@link OrderCreatedEvent}, onto the
 * {@code order-events} topic.
 * <p>
 * <strong>This producer must never be invoked from inside a {@code @Transactional}
 * method.</strong> Publishing before the local transaction commits would let {@code
 * notification-service} react to an order that a subsequent rollback could make disappear.
 * {@code OrderServiceImpl.create} is responsible for publishing only after its transactional
 * method has returned, via {@code TransactionSynchronizationManager.registerSynchronization(...)}
 * with an {@code afterCommit()} callback, the same approach {@code auth-service}'s
 * {@code AuthServiceImpl} already uses (see that class' own Javadoc for why a private
 * {@code @Transactional} helper method would not work here, Spring's self-invocation pitfall).
 * <p>
 * The send method below does not wait on or attach a callback to the {@code Future} that
 * {@link KafkaTemplate#send} returns: a broker that is unreachable fails the send
 * asynchronously, visible only in producer logs, never as an exception the caller of
 * {@code create()} would see.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    /** Single topic carrying every order-related event, consumed by notification-service. */
    private static final String ORDER_EVENTS_TOPIC = "order-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes {@link OrderCreatedEvent} after the order-creation transaction has committed.
     * Keyed by order id so every event for a given order lands on the same partition,
     * preserving ordering (relevant once this order later transitions status via the Saga's
     * outcome events).
     */
    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Publishing OrderCreatedEvent for orderId={}", event.orderId());
        kafkaTemplate.send(ORDER_EVENTS_TOPIC, String.valueOf(event.orderId()), event);
    }
}
