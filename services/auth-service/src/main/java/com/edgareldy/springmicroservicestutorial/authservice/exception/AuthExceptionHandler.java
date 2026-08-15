package com.edgareldy.springmicroservicestutorial.authservice.exception;

import com.edgareldy.springmicroservicestutorial.commonlib.dto.ApiResponse;
import com.edgareldy.springmicroservicestutorial.commonlib.exception.BaseExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * `auth-service`-specific exception mappings, layered on top of
 * {@link BaseExceptionHandler}'s generic {@code ResourceNotFoundException}/
 * {@code BusinessRuleException}/catch-all cases: invalid or expired tokens,
 * the unique-email race condition, and the Spring Security exceptions thrown
 * by {@code AuthenticationManager.authenticate(...)} during login.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
@RestControllerAdvice
public class AuthExceptionHandler extends BaseExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(AuthExceptionHandler.class);

    /** Maps an invalid/expired/already-consumed activation or reset token to a 400. */
    @ExceptionHandler(InvalidTokenException.class)
    protected ResponseEntity<ApiResponse<Object>> handleInvalidToken(InvalidTokenException ex) {
        log.debug("Invalid token: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(ex.getMessage()));
    }

    /**
     * Maps a unique-constraint violation to a 409 CONFLICT. {@code UserServiceImpl.createUser}
     * already checks {@code existsByEmailIgnoreCase} before inserting, but that check and the
     * insert are two separate statements: two concurrent registrations for the same email can
     * both pass the check before either commits. The database-level functional unique index
     * {@code idx_users_email_lower} (see the Flyway migration) is the real guard against a
     * duplicate row in that race, and it fails the second insert with a
     * {@link DataIntegrityViolationException} rather than silently allowing it; this handler
     * turns that into the same user-facing 409 a non-racing duplicate registration would
     * otherwise never reach (the applicative check normally wins first).
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.debug("Data integrity violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("Email already in use"));
    }

    /**
     * Wrong password: deliberately generic so the response never reveals whether the email or
     * the password was the part that didn't match, which would otherwise let a caller enumerate
     * registered addresses one guess at a time.
     */
    @ExceptionHandler(BadCredentialsException.class)
    protected ResponseEntity<ApiResponse<Object>> handleBadCredentials(BadCredentialsException ex) {
        log.debug("Bad credentials on login attempt");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid credentials"));
    }

    /** The account exists and the password matched, but {@code enabled} is still false. */
    @ExceptionHandler(DisabledException.class)
    protected ResponseEntity<ApiResponse<Object>> handleDisabled(DisabledException ex) {
        log.debug("Login attempt on a disabled account");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error("Account not yet activated"));
    }

    /** The account exists and the password matched, but {@code accountLocked} is true. */
    @ExceptionHandler(LockedException.class)
    protected ResponseEntity<ApiResponse<Object>> handleLocked(LockedException ex) {
        log.debug("Login attempt on a locked account");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Account is locked"));
    }

    /**
     * Fallback for any other {@link AuthenticationException} Spring Security's
     * {@code AuthenticationManager} might throw that isn't one of the three specific cases
     * above; kept generic on purpose, for the same reason {@link #handleBadCredentials} is.
     */
    @ExceptionHandler(AuthenticationException.class)
    protected ResponseEntity<ApiResponse<Object>> handleAuthentication(AuthenticationException ex) {
        log.debug("Authentication failed: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Invalid credentials"));
    }
}
