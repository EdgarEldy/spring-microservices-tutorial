package com.edgareldy.springmicroservicestutorial.commonlib.exception;

import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that each {@code @ExceptionHandler} method inherited from
 * {@link BaseExceptionHandler} maps its exception type to the expected HTTP
 * status and to a failed {@link ApiResponse} envelope: the original exception
 * message for the two mapped exception types, and a fixed generic message for
 * the catch-all case, which deliberately never leaks a raw exception message.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
class BaseExceptionHandlerTest {

    /**
     * Trivial concrete subclass exposing the protected handler methods to the
     * test, the same way a real service's {@code @RestControllerAdvice} would
     * extend {@link BaseExceptionHandler}.
     * <p>
     * Created by Edgar Muhamyangabo on 8/15/26
     * Author : Edgar Muhamyangabo
     * Date : 8/15/26
     * Project : spring-microservices-tutorial
     */
    static class TestExceptionHandler extends BaseExceptionHandler {
    }

    private final TestExceptionHandler handler = new TestExceptionHandler();

    @Test
    void handleResourceNotFound_mapsTo404WithFailedEnvelope() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Product 42 not found");

        ResponseEntity<ApiResponse<Object>> response = handler.handleResourceNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ApiResponse<Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isFalse();
        assertThat(body.message()).isEqualTo("Product 42 not found");
        assertThat(body.data()).isNull();
    }

    @Test
    void handleBusinessRule_mapsTo400WithFailedEnvelope() {
        BusinessRuleException ex = new BusinessRuleException("Email already in use");

        ResponseEntity<ApiResponse<Object>> response = handler.handleBusinessRule(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiResponse<Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isFalse();
        assertThat(body.message()).isEqualTo("Email already in use");
        assertThat(body.data()).isNull();
    }

    @Test
    void handleUnexpected_mapsTo500WithFailedEnvelopeAndGenericMessage() {
        Exception ex = new IllegalStateException("Database connection pool exhausted");

        ResponseEntity<ApiResponse<Object>> response = handler.handleUnexpected(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ApiResponse<Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.success()).isFalse();
        // The unexpected-exception handler deliberately does not leak the raw
        // exception message to the client, unlike the two handlers above.
        assertThat(body.message()).isEqualTo("An unexpected error occurred");
        assertThat(body.data()).isNull();
    }
}
