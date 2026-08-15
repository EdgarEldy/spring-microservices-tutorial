package com.edgareldy.springmicroservicestutorial.authservice.service;

import com.edgareldy.springmicroservicestutorial.authservice.dto.permission.PermissionRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.permission.PermissionResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Permission;
import java.util.List;

/**
 * Contract for managing {@link Permission}s, standalone from the
 * {@link com.edgareldy.springmicroservicestutorial.authservice.entity.Role}
 * they end up granted to.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public interface PermissionService {

    /**
     * Creates a new resource/action permission pair. Throws
     * {@code BusinessRuleException} if the same pair already exists.
     */
    Permission create(PermissionRequest request);

    /** Returns every permission. */
    List<Permission> findAll();

    /** Deletes a permission. Throws {@code ResourceNotFoundException} if none matches. */
    void delete(Long permissionId);

    /** Maps a {@link Permission} entity to its public {@link PermissionResponse} view. */
    PermissionResponse toResponse(Permission permission);
}
