package com.edgareldy.springmicroservicestutorial.catalogservice.exception;

import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.BaseExceptionHandler;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Activates {@link BaseExceptionHandler}'s {@code ResourceNotFoundException}/
 * {@code BusinessRuleException}/catch-all mappings for {@code catalog-service}, and adds
 * the same two mappings {@code auth-service}'s {@code AuthExceptionHandler} needed:
 * {@code common-lib} has no handler for a {@code @Valid} failure or a
 * {@code @PreAuthorize}/JWT-filter denial, so without these two methods here they would
 * both fall through to the catch-all 500. Also maps the generic
 * {@code AuthenticationException} (an unauthenticated request denied by
 * {@code JwtAuthFilter}/{@code SecurityConfig}) to 401, which {@code auth-service} covers
 * through its own login-specific handlers but this service never authenticates a
 * username/password pair, so no equivalent exists here to reuse.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@RestControllerAdvice
public class CatalogExceptionHandler extends BaseExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CatalogExceptionHandler.class);

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
     * Maps a {@code @PreAuthorize} denial to a 403. {@link AccessDeniedException} thrown by
     * the method-security AOP interceptor is caught by {@code DispatcherServlet}'s own
     * exception resolver (which checks {@code @ControllerAdvice} beans) before it can ever
     * reach a security-filter-level handler, so this handler is what actually produces the
     * 403 here. Also covers {@code AuthorizationDeniedException}, which extends this class.
     */
    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<ApiResponse<Object>> handleAccessDenied(AccessDeniedException ex) {
        log.debug("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.error("Access denied"));
    }

    /**
     * Maps a fully unauthenticated request denied by {@code SecurityConfig}'s authorization
     * rules to 401 (e.g. {@code AuthenticationCredentialsNotFoundException} when
     * {@code hasRole('ADMIN')} is evaluated with no {@code Authentication} present at all).
     */
    @ExceptionHandler(AuthenticationException.class)
    protected ResponseEntity<ApiResponse<Object>> handleAuthentication(AuthenticationException ex) {
        log.debug("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Authentication required"));
    }
}
