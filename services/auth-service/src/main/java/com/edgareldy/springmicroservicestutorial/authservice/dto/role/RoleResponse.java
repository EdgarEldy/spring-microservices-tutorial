package com.edgareldy.springmicroservicestutorial.authservice.dto.role;

import com.edgareldy.springmicroservicestutorial.authservice.dto.permission.PermissionResponse;
import java.util.List;

/**
 * Read-only view of a {@code Role}, including the flattened list of permissions
 * granted through it.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public record RoleResponse(
        Long id,
        String roleName,
        List<PermissionResponse> permissions
) {
}
