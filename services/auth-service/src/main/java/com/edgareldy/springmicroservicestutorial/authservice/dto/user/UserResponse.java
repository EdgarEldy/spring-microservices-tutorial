package com.edgareldy.springmicroservicestutorial.authservice.dto.user;

import java.util.List;

/**
 * Read-only view of a {@code User} exposed by the API (e.g. {@code GET /api/v1/auth/me}),
 * deliberately excluding the password hash and any other credential material.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public record UserResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        boolean enabled,
        boolean accountLocked,
        List<String> roles
) {
}
