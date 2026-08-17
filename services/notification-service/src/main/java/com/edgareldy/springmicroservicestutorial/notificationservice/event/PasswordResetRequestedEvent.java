package com.edgareldy.springmicroservicestutorial.notificationservice.event;

/**
 * Consumed by {@code PasswordResetRequestedEventConsumer} off the {@code auth-events} topic:
 * published by {@code auth-service}'s {@code AuthEventProducer} once a password-reset token
 * has been generated and its owning transaction has committed. Fire-and-forget, same rationale
 * as {@link UserRegisteredEvent}: a failed reset e-mail does not need to undo the token being
 * issued.
 * <p>
 * Local, notification-service-owned copy of {@code auth-service}'s own
 * {@code PasswordResetRequestedEvent} shape, same rationale as {@link OrderCreatedEvent}'s own
 * Javadoc.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
public record PasswordResetRequestedEvent(
        Long userId,
        String email,
        String resetToken
) {
}
