package com.edgareldy.springmicroservicestutorial.authservice.service;

import com.edgareldy.springmicroservicestutorial.authservice.dto.role.RoleRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.role.RoleResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Role;
import java.util.List;

/**
 * Contract for managing {@link Role}s and the {@link com.edgareldy.springmicroservicestutorial.authservice.entity.Permission}s
 * granted through them.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public interface RoleService {

    /** Creates a new role. Throws {@code BusinessRuleException} if the name is already in use. */
    Role create(RoleRequest request);

    /** Returns every role. */
    List<Role> findAll();

    /** Looks up a role by id. Throws {@code ResourceNotFoundException} if none matches. */
    Role findById(Long roleId);

    /** Grants the given permission to the given role, returning the updated role. */
    Role addPermission(Long roleId, Long permissionId);

    /** Revokes the given permission from the given role, returning the updated role. */
    Role removePermission(Long roleId, Long permissionId);

    /** Deletes a role. Throws {@code ResourceNotFoundException} if none matches. */
    void delete(Long roleId);

    /** Maps a {@link Role} entity to its public {@link RoleResponse} view, including its permissions. */
    RoleResponse toResponse(Role role);
}
