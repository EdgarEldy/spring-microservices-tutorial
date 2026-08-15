package com.edgareldy.springmicroservicestutorial.customerservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for {@code POST /api/v1/customers}.
 * <p>
 * {@link #userId} is supplied by the caller rather than derived from the
 * caller's own JWT: the token issued by {@code auth-service}
 * ({@code JwtService.generateToken}) only carries {@code sub} (email),
 * {@code roles}, and {@code permissions} as claims, never the numeric
 * {@code users.id} this field needs to reference, and this branch adds no
 * Feign client back to {@code auth-service} to resolve one from the other
 * (that synchronous, cross-service resolution pattern is introduced in
 * {@code feature/order-service}, via {@code CustomerClient}/{@code
 * ProductClient}). The README's own wording for this endpoint, "given an
 * existing userId from auth-service", matches this: the caller already
 * knows its own id (e.g. from {@code GET /api/v1/auth/me}, once that
 * response carries it) and passes it along explicitly. It is still just a
 * plain value here, never validated against {@code auth_db} directly (see
 * {@code Customer}'s class Javadoc): an invalid/unknown {@code userId} is
 * accepted as-is in this branch, the same trade-off the README makes
 * explicit for {@code order-service}'s Feign-validated ids not applying
 * here since no such client exists yet.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public record CustomerRequest(
        @NotNull Long userId,
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank String telephone,
        @NotBlank @Email String email,
        @NotBlank String address
) {
}
