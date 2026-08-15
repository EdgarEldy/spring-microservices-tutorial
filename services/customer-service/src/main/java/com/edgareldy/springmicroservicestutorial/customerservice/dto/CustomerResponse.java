package com.edgareldy.springmicroservicestutorial.customerservice.dto;

/**
 * Read-only view of a {@code Customer}, returned from {@code GET
 * /api/v1/customers/{id}} and as part of {@code POST}/{@code PUT
 * /api/v1/customers}'s responses. {@link #userId} is passed through exactly
 * as stored, still an unresolved plain value at this point: enriching it
 * into an actual name/email from {@code auth-service} is a caller's
 * responsibility (via that service's own API), never something this
 * response does on its behalf.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public record CustomerResponse(
        Long id,
        Long userId,
        String firstName,
        String lastName,
        String telephone,
        String email,
        String address
) {
}
