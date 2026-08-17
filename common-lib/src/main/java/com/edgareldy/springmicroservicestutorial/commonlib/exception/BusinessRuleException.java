package com.edgareldy.springmicroservicestutorial.commonlib.exception;

/**
 * Thrown by any service when a request is well-formed but violates a domain
 * rule (e.g. an already-used email, an order placed on an out-of-stock
 * product), so {@link BaseExceptionHandler} can translate it into a uniform
 * 400 response; individual services may still catch it themselves to map it
 * to a more specific status (e.g. 409) when that distinction matters.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}
