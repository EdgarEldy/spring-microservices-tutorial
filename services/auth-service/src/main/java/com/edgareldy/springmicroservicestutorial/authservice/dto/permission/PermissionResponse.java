package com.edgareldy.springmicroservicestutorial.authservice.dto.permission;

/**
 * Read-only view of a {@code Permission}, returned standalone or nested inside a
 * {@code RoleResponse}.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public record PermissionResponse(
        Long id,
        String resource,
        String action
) {
}
