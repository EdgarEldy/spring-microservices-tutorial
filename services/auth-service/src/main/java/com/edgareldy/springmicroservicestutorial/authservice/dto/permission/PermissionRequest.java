package com.edgareldy.springmicroservicestutorial.authservice.dto.permission;

import jakarta.validation.constraints.NotBlank;

/**
 * Payload for creating a {@code Permission} as a resource/action pair (e.g.
 * {@code PRODUCT}/{@code WRITE}).
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public record PermissionRequest(

        @NotBlank(message = "Resource is required")
        String resource,

        @NotBlank(message = "Action is required")
        String action
) {
}
