package com.edgareldy.springmicroservicestutorial.authservice.event;

/**
 * Fact published on the {@code auth-events} topic once a new user has been registered
 * (disabled, pending activation) and its owning transaction has already committed.
 * Carries only what {@code notification-service} needs to send the activation e-mail,
 * never the full {@code User} entity. Fire-and-forget: a failed activation e-mail does
 * not need to undo the account creation, so this event has no compensating action.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public record UserRegisteredEvent(
        Long userId,
        String email,
        String firstName,
        String activationToken
) {
}
