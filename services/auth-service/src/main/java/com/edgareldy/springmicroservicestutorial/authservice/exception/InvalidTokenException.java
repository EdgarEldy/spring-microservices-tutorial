package com.edgareldy.springmicroservicestutorial.authservice.exception;

/**
 * Thrown when an activation token or a password-reset token presented by a
 * caller cannot be honored: not found, already validated/consumed, or past
 * its expiration. Deliberately not one of {@code common-lib}'s base
 * exceptions ({@code ResourceNotFoundException}/{@code BusinessRuleException})
 * since a token failure is specific to this service's token flows, not a
 * generic "missing resource" or "business rule" case shared across services.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }

    public InvalidTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
