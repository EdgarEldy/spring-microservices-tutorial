package com.edgareldy.springmicroservicestutorial.authservice.dto.user;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for updating the mutable, non-credential part of a user's profile
 * (first/last name); email and password changes go through dedicated flows.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public record UpdateProfileRequest(

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName
) {
}
