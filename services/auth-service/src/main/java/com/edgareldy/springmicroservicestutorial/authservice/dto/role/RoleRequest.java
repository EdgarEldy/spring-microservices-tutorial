package com.edgareldy.springmicroservicestutorial.authservice.dto.role;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for creating or renaming a {@code Role}, identified solely by its name;
 * permission assignment is handled through a separate endpoint.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public record RoleRequest(

        @NotBlank(message = "Role name is required")
        String roleName
) {
}
