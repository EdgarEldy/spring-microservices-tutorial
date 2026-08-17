package com.edgareldy.springmicroservicestutorial.authservice.service;

import com.edgareldy.springmicroservicestutorial.authservice.dto.role.RoleRequest;
import com.edgareldy.springmicroservicestutorial.authservice.dto.role.RoleResponse;
import com.edgareldy.springmicroservicestutorial.authservice.entity.Role;
import java.util.List;

/**
 * Contract for managing {@link Role}s and the {@link com.edgareldy.springmicroservicestutorial.authservice.entity.Permission}s
 * granted through them. Unlike {@link UserService}, most methods here return
 * {@link RoleResponse} directly rather than the {@link Role} entity: nothing
 * outside this service (no Kafka event, no other orchestrating service) ever
 * needs the raw entity after a mutation, so there is no reason to make every
 * caller repeat a {@code toResponse(...)} call. {@link #findById} is the
 * exception, kept entity-returning since {@code addPermission}/
 * {@code removePermission} need the managed entity internally to mutate its
 * permission set before saving.
 * <p>
 * Created by Edgar Muhamyangabo on 8/15/26
 * Author : Edgar Muhamyangabo
 * Date : 8/15/26
 * Project : spring-microservices-tutorial
 */
public interface RoleService {

    /** Creates a new role. Throws {@code BusinessRuleException} if the name is already in use. */
    RoleResponse create(RoleRequest request);

    /** Returns every role. */
    List<RoleResponse> findAll();

    /** Looks up a role by id. Throws {@code ResourceNotFoundException} if none matches. */
    Role findById(Long roleId);

    /** Grants the given permission to the given role, returning the updated role. */
    RoleResponse addPermission(Long roleId, Long permissionId);

    /** Revokes the given permission from the given role, returning the updated role. */
    RoleResponse removePermission(Long roleId, Long permissionId);

    /** Deletes a role. Throws {@code ResourceNotFoundException} if none matches. */
    void delete(Long roleId);
}
