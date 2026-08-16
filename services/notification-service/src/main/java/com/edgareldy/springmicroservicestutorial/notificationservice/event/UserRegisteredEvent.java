package com.edgareldy.springmicroservicestutorial.notificationservice.event;

/**
 * Consumed by {@code UserRegisteredEventConsumer} off the {@code auth-events} topic: published
 * by {@code auth-service}'s {@code AuthEventProducer} once a new user has been registered
 * (disabled, pending activation) and its owning transaction has committed. Fire-and-forget: a
 * failed activation e-mail does not need to undo the account creation, so there is no
 * compensating event back to {@code auth-service}.
 * <p>
 * Local, notification-service-owned copy of {@code auth-service}'s own
 * {@code UserRegisteredEvent} shape, same rationale as {@link OrderCreatedEvent}'s own Javadoc.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
public record UserRegisteredEvent(
        Long userId,
        String email,
        String firstName,
        String activationToken
) {
}
