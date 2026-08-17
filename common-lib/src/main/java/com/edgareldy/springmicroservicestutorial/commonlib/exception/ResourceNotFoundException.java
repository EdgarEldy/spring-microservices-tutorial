package com.edgareldy.springmicroservicestutorial.commonlib.exception;

/**
 * Thrown by any service when a requested resource does not exist, so
 * {@link BaseExceptionHandler} can translate it into a uniform 404 response.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
