package com.edgareldy.springmicroservicestutorial.commonlib.dto;

import java.time.Instant;

/**
 * Generic envelope wrapping every business service's HTTP response body, so
 * every endpoint in the system returns the same success/message/data/timestamp
 * shape regardless of which service produced it.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Instant timestamp
) {

    /**
     * Builds a successful response carrying the given payload and a human
     * readable message, stamped with the current instant.
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, Instant.now());
    }

    /**
     * Builds a failed response with no payload, carrying only the error
     * message and the current instant.
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, Instant.now());
    }
}
