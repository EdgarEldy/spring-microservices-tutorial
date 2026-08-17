package com.edgareldy.springmicroservicestutorial.orderservice.exception;

import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.BaseExceptionHandler;
import feign.FeignException;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Activates {@link BaseExceptionHandler}'s {@code ResourceNotFoundException}/
 * {@code BusinessRuleException}/catch-all mappings for {@code order-service}, and adds the
 * same three mappings every other business service needs: {@code common-lib} has no handler
 * for a {@code @Valid} failure or a security-filter-level denial, so without these methods
 * here they would both fall through to the catch-all 500. {@link #handleMissingHeader} is
 * this service's own addition, since {@code POST /api/v1/orders} is the only endpoint in the
 * project requiring a mandatory header ({@code Idempotency-Key}).
 * <p>
 * {@link #handleFeignException} is this service's addition, backing the README's "explicit
 * handling of a Feign call failing" requirement: {@code OrderServiceImpl} already translates
 * every {@code FeignException} it catches around a {@code ProductClient}/{@code CustomerClient}
 * call into a {@code ResourceNotFoundException} (product/customer genuinely missing) or a
 * {@code BusinessRuleException} (the downstream service errored/timed out) with a clear
 * message, so in practice a raw {@code FeignException} should never reach this handler at all.
 * It stays here anyway as a last-resort safety net, in case a future Feign call site forgets
 * to wrap its own call: without this, an unwrapped {@code FeignException} would otherwise fall
 * through to the generic catch-all and leak a raw upstream stack trace in the 500 response.
 * <p>
 * Created by Edgar Muhamyangabo on 8/16/26
 * Author : Edgar Muhamyangabo
 * Date : 8/16/26
 * Project : spring-microservices-tutorial
 */
@RestControllerAdvice
public class OrderExceptionHandler extends BaseExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderExceptionHandler.class);

    /**
     * Maps a {@code @Valid} request body failure to a 400, collecting every field error into
     * one readable message rather than letting it fall through to the catch-all 500.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ApiResponse<Object>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.debug("Validation failed: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(message));
    }

    /**
     * Maps a missing required {@code Idempotency-Key} header to a 400, rather than letting
     * the resulting {@code MissingRequestHeaderException} fall through to the catch-all 500.
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    protected ResponseEntity<ApiResponse<Object>> handleMissingHeader(MissingRequestHeaderException ex) {
        log.debug("Missing required header: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Missing required header: " + ex.getHeaderName()));
    }

    /**
     * Maps an access-control denial to a 403. Also covers
     * {@code AuthorizationDeniedException}, which extends this class.
     */
    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        log.debug("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied"));
    }

    /**
     * Maps a fully unauthenticated request denied by {@code SecurityConfig}'s
     * {@code authenticated()} rule (e.g. a missing/invalid JWT caught by
     * {@code JwtAuthFilter}) to 401.
     */
    @ExceptionHandler(AuthenticationException.class)
    protected ResponseEntity<ApiResponse<Object>> handleAuthentication(AuthenticationException ex) {
        log.debug("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Authentication required"));
    }

    /**
     * Last-resort safety net for a {@code FeignException} that reaches this handler
     * unwrapped (see class Javadoc); maps it to a 502 with a clear message instead of
     * leaking the upstream service's raw response body/stack trace.
     */
    @ExceptionHandler(FeignException.class)
    protected ResponseEntity<ApiResponse<Object>> handleFeignException(FeignException ex) {
        log.error("Unhandled Feign call failure", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiResponse.error("A downstream service call failed"));
    }
}
