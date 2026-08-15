package com.edgareldy.springmicroservicestutorial.authservice.event;

/**
 * Fact published on the {@code auth-events} topic once a password-reset token has been
 * generated and its owning transaction has already committed. Carries only what
 * {@code notification-service} needs to send the reset e-mail, never the full
 * {@code User} entity. Fire-and-forget, same rationale as {@link UserRegisteredEvent}:
 * a failed reset e-mail does not need to undo the token being issued.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public record PasswordResetRequestedEvent(
        Long userId,
        String email,
        String resetToken
) {
}
