package com.edgareldy.springmicroservicestutorial.authservice.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes {@code auth-service}'s outbound events ({@link UserRegisteredEvent},
 * {@link PasswordResetRequestedEvent}) onto the {@code auth-events} topic.
 * <p>
 * Both events are fire-and-forget from {@code auth-service}'s point of view: no
 * compensating event is consumed back, so failures here never need to roll anything
 * back locally (see the class-level Javadoc on each event for the rationale).
 * <p>
 * <strong>This producer must never be invoked from inside a {@code @Transactional}
 * method.</strong> Publishing before the local transaction commits would let
 * {@code notification-service} react to a user/token that a subsequent rollback could
 * make disappear. The caller (the applicative service, e.g. a future
 * {@code AuthServiceImpl}) is responsible for publishing only after the transactional
 * method has returned, either by calling this producer from a non-transactional
 * caller, or via {@code TransactionSynchronizationManager.registerSynchronization(...)}
 * with an {@code afterCommit()} callback.
 * <p>
 * Neither publish method below waits on or attaches a callback to the
 * {@code Future} that {@link KafkaTemplate#send} returns: a broker that is
 * unreachable (e.g. {@code kafka} not yet added to {@code docker-compose.yml}
 * as of this branch, see the README's "Order of work") fails the send
 * asynchronously, visible only in producer logs, never as an exception the
 * caller of {@code register()}/{@code forgotPassword()} would see.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventProducer {

    /** Single topic carrying every account-related event, consumed by notification-service. */
    private static final String AUTH_EVENTS_TOPIC = "auth-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publishes {@link UserRegisteredEvent} after the registration transaction has
     * committed. Keyed by user id so all events for a given user land on the same
     * partition, preserving per-user ordering.
     */
    public void publishUserRegistered(UserRegisteredEvent event) {
        log.info("Publishing UserRegisteredEvent for userId={}", event.userId());
        kafkaTemplate.send(AUTH_EVENTS_TOPIC, String.valueOf(event.userId()), event);
    }

    /**
     * Publishes {@link PasswordResetRequestedEvent} after the reset-token transaction
     * has committed. Same key/partitioning rationale as {@link #publishUserRegistered}.
     */
    public void publishPasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("Publishing PasswordResetRequestedEvent for userId={}", event.userId());
        kafkaTemplate.send(AUTH_EVENTS_TOPIC, String.valueOf(event.userId()), event);
    }
}
